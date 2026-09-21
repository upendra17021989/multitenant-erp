package com.multitenanterp.reporting;

import com.multitenanterp.platform.tenant.TenantContext;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class ReportingServiceTest {
    @AfterEach void clear(){TenantContext.clear();}
    @Test void csvEscapesFormulasQuotesAndNewlines() {
        assertThat(ReportingService.csvCell(" =HYPERLINK(\"bad\")")).isEqualTo("\"' =HYPERLINK(\"\"bad\"\")\"");
        assertThat(ReportingService.csvCell("line1\nline2")).isEqualTo("\"line1\nline2\"");
        assertThat(ReportingService.csvCell("\t@SUM(A1)")).startsWith("\"'");
    }
    @Test void tenantReportsReconcileAndSalaryRequiresPayrollRole() {
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");var db=JdbcClient.create(ds);
        db.sql("CREATE TABLE tenant(id UUID,legal_name VARCHAR)").update();
        db.sql("CREATE TABLE employment(id UUID,tenant_id UUID,employee_number VARCHAR)").update();
        db.sql("CREATE TABLE payroll_run(id UUID,tenant_id UUID,payroll_year INT,payroll_month INT,status VARCHAR)").update();
        db.sql("CREATE TABLE payroll_employee_result(id UUID,tenant_id UUID,payroll_run_id UUID,employment_id UUID,gross_pay DECIMAL,deductions DECIMAL,employer_contributions DECIMAL,net_pay DECIMAL,ctc DECIMAL,payable_days DECIMAL)").update();
        UUID a=UUID.randomUUID(),b=UUID.randomUUID();
        for(UUID tenant:List.of(a,b)) {db.sql("INSERT INTO tenant VALUES(?,?)").params(tenant,tenant.equals(a)?"Company A":"Company B").update();db.sql("INSERT INTO employment VALUES(?,?,'EMP-1')").params(tenant,tenant).update();db.sql("INSERT INTO payroll_run VALUES(?,?,2026,9,'LOCKED')").params(tenant,tenant).update();db.sql("INSERT INTO payroll_employee_result VALUES(?,?,?,?,100,10,5,90,105,30)").params(tenant,tenant,tenant,tenant).update();}
        var service=new ReportingService(db);TenantContext.set(UUID.randomUUID(),a,Set.of("PAYROLL_MANAGER"));
        var report=service.report("salary-register",LocalDate.of(2026,9,1),LocalDate.of(2026,9,30));
        assertThat(report.company()).isEqualTo("Company A");assertThat(report.rows()).hasSize(1);
        assertThat(report.rows().getFirst().get(report.columns().indexOf("net_pay"))).isEqualTo("90");
        assertThat(ReportingService.csv(report)).contains("Company A").doesNotContain("Company B");
        db.sql("UPDATE payroll_run SET status='CALCULATED' WHERE tenant_id=?").param(a).update();
        assertThat(service.report("salary-register",report.from(),report.to()).rows()).isEmpty();
        TenantContext.set(UUID.randomUUID(),a,Set.of("HR_MANAGER"));
        assertThatThrownBy(()->service.report("salary-register",report.from(),report.to())).isInstanceOf(ResponseStatusException.class);
        TenantContext.set(UUID.randomUUID(),a,Set.of("EMPLOYEE"));
        assertThatThrownBy(()->service.report("employees",report.from(),report.to())).isInstanceOf(ResponseStatusException.class);
    }
}
