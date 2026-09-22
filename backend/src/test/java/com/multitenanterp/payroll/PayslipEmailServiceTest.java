package com.multitenanterp.payroll;

import com.multitenanterp.platform.tenant.TenantContext;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.web.server.ResponseStatusException;
import java.security.MessageDigest;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PayslipEmailServiceTest {
    private JdbcClient db;
    private PayslipEmailService service;
    private PayslipService slips;
    private PayslipEmailSender sender;
    private UUID tenant, slip, employee, run;
    private Payslip metadata;
    private final byte[] content = "%PDF test attachment".getBytes(java.nio.charset.StandardCharsets.UTF_8);

    @BeforeEach void setup() throws Exception {
        tenant=UUID.randomUUID(); slip=UUID.randomUUID(); employee=UUID.randomUUID(); run=UUID.randomUUID();
        JdbcDataSource ds=new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        db=JdbcClient.create(ds);
        db.sql("CREATE TABLE tenant(id UUID PRIMARY KEY,legal_name VARCHAR)").update();
        db.sql("CREATE TABLE payroll_run(id UUID PRIMARY KEY,tenant_id UUID,status VARCHAR)").update();
        db.sql("CREATE TABLE employment(id UUID PRIMARY KEY,tenant_id UUID,work_email VARCHAR)").update();
        db.sql("CREATE TABLE payslip(id UUID PRIMARY KEY,tenant_id UUID,status VARCHAR,UNIQUE(id,tenant_id))").update();
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V24__payslip_email_delivery.sql")).execute(ds);
        db.sql("INSERT INTO tenant VALUES(?,'Acme')").param(tenant).update();
        db.sql("INSERT INTO payroll_run VALUES(?,?,'LOCKED')").params(run,tenant).update();
        db.sql("INSERT INTO employment VALUES(?,?,'employee@example.com')").params(employee,tenant).update();
        db.sql("INSERT INTO payslip VALUES(?,?,'RELEASED')").params(slip,tenant).update();
        metadata=new Payslip(slip,run,UUID.randomUUID(),employee,"E1","Employee",2026,9,"payslip.pdf",content.length,
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content)),"RELEASED","manager",null,"manager",null,null,null);
        slips=mock(PayslipService.class); sender=mock(PayslipEmailSender.class);
        when(slips.list(2026,9)).thenAnswer(i -> TenantContext.requireTenantId().equals(tenant)?List.of(metadata):List.of());
        when(slips.downloadReleased(slip,employee)).thenReturn(new PayslipDownload(metadata,new ByteArrayResource(content)));
        when(sender.enabled()).thenReturn(true);
        service=new PayslipEmailService(db,slips,sender,new DataSourceTransactionManager(ds));
        TenantContext.set(tenant);
    }
    @AfterEach void clear() { TenantContext.clear(); }

    @Test void acceptedEmailUsesStoredPdfAndAuditsActorWithIdempotentRetry() throws Exception {
        UUID request=UUID.randomUUID();
        var first=service.send(2026,9,slip,request,"manager");
        assertThat(first.status()).isEqualTo("ACCEPTED");
        assertThat(first.requestedBy()).isEqualTo("manager");
        assertThat(first.recipient()).isEqualTo("employee@example.com");
        assertThat(first.completedAt()).isNotNull();
        assertThat(service.send(2026,9,slip,request,"other-manager")).isEqualTo(first);
        verify(sender,times(1)).send("employee@example.com","Acme",2026,9,"payslip.pdf",content);
        assertThat(service.history(2026,9,slip)).containsExactly(first);
    }
    @Test void failedSendPersistsAuditAndNewRequestCanRetry() throws Exception {
        doThrow(new RuntimeException("secret provider details")).doNothing().when(sender).send(anyString(),anyString(),anyInt(),anyInt(),anyString(),any());
        UUID request=UUID.randomUUID();
        var failure=service.send(2026,9,slip,request,"manager");
        assertThat(failure.status()).isEqualTo("FAILED");
        assertThat(failure.failureCode()).isEqualTo("EMAIL_SEND_FAILED");
        assertThat(service.send(2026,9,slip,request,"manager")).isEqualTo(failure);
        assertThat(service.send(2026,9,slip,UUID.randomUUID(),"manager").status()).isEqualTo("ACCEPTED");
        assertThat(service.history(2026,9,slip)).hasSize(2);
        verify(sender,times(2)).send(anyString(),anyString(),anyInt(),anyInt(),anyString(),any());
    }
    @Test void invalidOrMissingWorkEmailIsAuditedWithoutSending() throws Exception {
        for(String value:List.of("", "a@example.com,b@example.com", "a@example.com\r\nBcc: other@example.com")) {
            db.sql("UPDATE employment SET work_email=?").param(value).update();
            assertThat(service.send(2026,9,slip,UUID.randomUUID(),"manager").failureCode()).isEqualTo("INVALID_WORK_EMAIL");
        }
        verify(sender,never()).send(anyString(),anyString(),anyInt(),anyInt(),anyString(),any());
    }
    @Test void disabledUnreleasedAndReversedPayrollCannotSend() throws Exception {
        when(sender.enabled()).thenReturn(false);
        assertThatThrownBy(()->service.send(2026,9,slip,UUID.randomUUID(),"manager")).isInstanceOf(ResponseStatusException.class);
        when(sender.enabled()).thenReturn(true);
        db.sql("UPDATE payslip SET status='GENERATED'").update();
        assertThatThrownBy(()->service.send(2026,9,slip,UUID.randomUUID(),"manager")).isInstanceOf(ResponseStatusException.class);
        db.sql("UPDATE payslip SET status='RELEASED'").update();
        db.sql("UPDATE payroll_run SET status='REVERSED'").update();
        assertThatThrownBy(()->service.send(2026,9,slip,UUID.randomUUID(),"manager")).isInstanceOf(ResponseStatusException.class);
        assertThat(service.history(2026,9,slip)).isEmpty();
        verify(sender,never()).send(anyString(),anyString(),anyInt(),anyInt(),anyString(),any());
    }
    @Test void tenantAndPeriodBoundariesProtectSendingAndAuditHistory() {
        service.send(2026,9,slip,UUID.randomUUID(),"manager");
        assertThatThrownBy(()->service.history(2026,10,slip)).isInstanceOf(ResponseStatusException.class);
        TenantContext.set(UUID.randomUUID());
        assertThatThrownBy(()->service.history(2026,9,slip)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->service.send(2026,9,slip,UUID.randomUUID(),"manager")).isInstanceOf(ResponseStatusException.class);
    }
    @Test void damagedOrMissingContentIsAuditedWithoutSending() throws Exception {
        when(slips.downloadReleased(slip,employee)).thenReturn(new PayslipDownload(metadata,new ByteArrayResource(new byte[]{1})));
        assertThat(service.send(2026,9,slip,UUID.randomUUID(),"manager").failureCode()).isEqualTo("CONTENT_INTEGRITY_FAILED");
        when(slips.downloadReleased(slip,employee)).thenThrow(new RuntimeException("missing"));
        assertThat(service.send(2026,9,slip,UUID.randomUUID(),"manager").failureCode()).isEqualTo("CONTENT_UNAVAILABLE");
        verify(sender,never()).send(anyString(),anyString(),anyInt(),anyInt(),anyString(),any());
    }
    @Test void concurrentRequestsCannotSendTheSamePayslipTwice() throws Exception {
        var entered=new java.util.concurrent.CountDownLatch(1);
        var release=new java.util.concurrent.CountDownLatch(1);
        doAnswer(i->{entered.countDown(); if(!release.await(5,java.util.concurrent.TimeUnit.SECONDS))throw new IllegalStateException("timeout"); return null;})
                .when(sender).send(anyString(),anyString(),anyInt(),anyInt(),anyString(),any());
        UUID request=UUID.randomUUID();
        try(var executor=java.util.concurrent.Executors.newSingleThreadExecutor()) {
            var sending=executor.submit(()->{TenantContext.set(tenant);try{return service.send(2026,9,slip,request,"manager");}finally{TenantContext.clear();}});
            try {
                assertThat(entered.await(5,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
                assertThat(service.send(2026,9,slip,request,"manager").status()).isEqualTo("SENDING");
                assertThatThrownBy(()->service.send(2026,9,slip,UUID.randomUUID(),"manager")).isInstanceOf(ResponseStatusException.class);
            } finally { release.countDown(); }
            assertThat(sending.get(5,java.util.concurrent.TimeUnit.SECONDS).status()).isEqualTo("ACCEPTED");
        }
        verify(sender,times(1)).send(anyString(),anyString(),anyInt(),anyInt(),anyString(),any());
    }
}
