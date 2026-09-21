package com.multitenanterp.leave;

import com.multitenanterp.platform.tenant.TenantContext;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.springframework.core.io.*;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.core.simple.JdbcClient;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class LeaveAccrualIntegrationTest {
    private JdbcClient db;private LeaveService leave;private LeaveAccrualService accrual;private UUID tenant,employee,type;
    @BeforeEach void setup() throws Exception {
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");db=JdbcClient.create(ds);
        db.sql("CREATE TABLE tenant(id UUID PRIMARY KEY,time_zone VARCHAR DEFAULT 'Asia/Kolkata')").update();
        db.sql("CREATE TABLE employment(id UUID PRIMARY KEY,tenant_id UUID,joining_date DATE,exit_date DATE,UNIQUE(id,tenant_id))").update();
        for(String script:new String[]{"V7__create_leave_management.sql","V19__leave_processing.sql"}) {
            String sql=new ClassPathResource("db/migration/"+script).getContentAsString(StandardCharsets.UTF_8).replace("TIMESTAMPTZ","TIMESTAMP WITH TIME ZONE");
            new ResourceDatabasePopulator(new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8))).execute(ds);
        }
        tenant=UUID.randomUUID();employee=UUID.randomUUID();db.sql("INSERT INTO tenant(id,leave_accrual_start) VALUES(?,DATE '2024-01-01')").param(tenant).update();
        db.sql("INSERT INTO employment VALUES(?,?,DATE '2024-01-01',NULL)").params(employee,tenant).update();TenantContext.set(tenant);
        leave=new LeaveService(db);accrual=new LeaveAccrualService(db,leave);
        type=leave.saveType(null,new SaveLeaveTypeRequest("CL","Casual leave",true,new BigDecimal("12"),"MONTHLY",new BigDecimal("0.5"),null,true,new BigDecimal("3"),false,false,"ACTIVE")).id();
    }
    @AfterEach void clear(){TenantContext.clear();}
    @Test void catchUpIsIdempotentAndCarriesOnlyPolicyLimit() {
        assertThat(accrual.process(LocalDate.of(2025,1,1))).isEqualTo(13);
        assertThat(leave.balances(employee,2024).getFirst().accrued()).isEqualByComparingTo("12");
        assertThat(leave.balances(employee,2025).getFirst().openingBalance()).isEqualByComparingTo("3");
        assertThat(accrual.process(LocalDate.of(2025,1,1))).isZero();
        assertThat(accrual.events()).hasSize(13);
        UUID other=UUID.randomUUID();db.sql("INSERT INTO tenant(id) VALUES(?)").param(other).update();TenantContext.set(other);
        assertThat(accrual.events()).isEmpty();assertThat(accrual.process(LocalDate.of(2025,1,1))).isZero();
    }
    @Test void joiningAndExitProrateAccrualAndPreventCarryAfterExit() {
        db.sql("UPDATE employment SET joining_date=DATE '2024-07-01',exit_date=DATE '2024-12-31' WHERE id=?").param(employee).update();
        accrual.process(LocalDate.of(2025,1,1));
        assertThat(leave.balances(employee,2024).getFirst().accrued()).isEqualByComparingTo("6.03");
        assertThat(leave.balances(employee,2025)).isEmpty();
    }
}
