package com.multitenanterp.employee;

import com.multitenanterp.platform.tenant.TenantContext;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.springframework.core.io.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class FinancialRequestServiceTest {
    private JdbcClient db;private FinancialRequestService service;private UUID tenant,other,employee;
    @BeforeEach void setup() throws Exception {
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");db=JdbcClient.create(ds);
        db.sql("CREATE TABLE tenant(id UUID PRIMARY KEY)").update();
        db.sql("CREATE TABLE employment(id UUID PRIMARY KEY,tenant_id UUID,employment_status VARCHAR,UNIQUE(id,tenant_id))").update();
        db.sql("CREATE TABLE employee_document(id UUID PRIMARY KEY,tenant_id UUID,employment_id UUID,UNIQUE(id,tenant_id,employment_id))").update();
        db.sql("CREATE TABLE payroll_run(id UUID PRIMARY KEY,tenant_id UUID,payroll_year INT,payroll_month INT,status VARCHAR)").update();
        db.sql("CREATE TABLE payroll_variable_input(id UUID PRIMARY KEY,tenant_id UUID,employment_id UUID,payroll_year INT,payroll_month INT,code VARCHAR,name VARCHAR,input_type VARCHAR,amount DECIMAL(14,2),notes VARCHAR,created_by VARCHAR,UNIQUE(tenant_id,employment_id,payroll_year,payroll_month,code))").update();
        String sql=new ClassPathResource("db/migration/V21__employee_financial_requests.sql").getContentAsString(StandardCharsets.UTF_8).replace("TIMESTAMPTZ","TIMESTAMP WITH TIME ZONE");
        new ResourceDatabasePopulator(new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8))).execute(ds);
        tenant=UUID.randomUUID();other=UUID.randomUUID();employee=UUID.randomUUID();db.sql("INSERT INTO tenant VALUES(?),(?)").params(tenant,other).update();db.sql("INSERT INTO employment VALUES(?,?,'ACTIVE')").params(employee,tenant).update();TenantContext.set(tenant);service=new FinancialRequestService(db);
    }
    @AfterEach void clear(){TenantContext.clear();}
    private FinancialRequestService.Request loan(){return service.submit(new FinancialRequestService.Submit(employee,"LOAN",new BigDecimal("1000.00"),3,LocalDate.of(2026,10,1),"Personal loan",null),"maker");}
    @Test void installmentsReconcileToPrincipal(){assertThat(FinancialRequestService.installments(new BigDecimal("1000.00"),3)).containsExactly(new BigDecimal("333.33"),new BigDecimal("333.33"),new BigDecimal("333.34"));}
    @Test void approvalAndDisbursementScheduleRecoveryExactlyOnce(){
        var request=loan();
        assertThatThrownBy(()->service.decide(request.id(),new FinancialRequestService.Decision("APPROVED","Accepted"),"maker")).isInstanceOf(ResponseStatusException.class);
        service.decide(request.id(),new FinancialRequestService.Decision("APPROVED","Accepted"),"checker");
        assertThat(service.recordPayment(request.id(),"BANK-REF-1","finance").status()).isEqualTo("DISBURSED");
        assertThat(db.sql("SELECT SUM(amount) FROM payroll_variable_input").query(BigDecimal.class).single()).isEqualByComparingTo("1000");
        assertThat(db.sql("SELECT COUNT(*) FROM payroll_variable_input").query(Integer.class).single()).isEqualTo(3);
        assertThatThrownBy(()->service.recordPayment(request.id(),"BANK-REF-1","finance")).isInstanceOf(ResponseStatusException.class);
        TenantContext.set(other);assertThat(service.list(null)).isEmpty();
        assertThatThrownBy(()->service.recordPayment(request.id(),"BANK-REF-1","finance")).isInstanceOf(ResponseStatusException.class);
    }
    @Test void finalizedPayrollBlocksRecoveryScheduling(){
        var request=loan();service.decide(request.id(),new FinancialRequestService.Decision("APPROVED","Accepted"),"checker");
        db.sql("INSERT INTO payroll_run VALUES(?,?,2026,10,'LOCKED')").params(UUID.randomUUID(),tenant).update();
        assertThatThrownBy(()->service.recordPayment(request.id(),"BANK-REF","finance")).isInstanceOf(ResponseStatusException.class).hasMessageContaining("finalized");
        assertThat(service.list(null).getFirst().status()).isEqualTo("APPROVED");
        assertThat(db.sql("SELECT COUNT(*) FROM payroll_variable_input").query(Integer.class).single()).isZero();
    }
    @Test void expenseReceiptMustBelongToEmployee(){
        assertThatThrownBy(()->service.submit(new FinancialRequestService.Submit(employee,"EXPENSE",BigDecimal.TEN,1,null,"Travel",null),"maker")).isInstanceOf(ResponseStatusException.class).hasMessageContaining("receipt");
        UUID receipt=UUID.randomUUID();db.sql("INSERT INTO employee_document VALUES(?,?,?)").params(receipt,other,UUID.randomUUID()).update();
        assertThatThrownBy(()->service.submit(new FinancialRequestService.Submit(employee,"EXPENSE",BigDecimal.TEN,1,null,"Travel",receipt),"maker")).isInstanceOf(ResponseStatusException.class).hasMessageContaining("company");
    }
}
