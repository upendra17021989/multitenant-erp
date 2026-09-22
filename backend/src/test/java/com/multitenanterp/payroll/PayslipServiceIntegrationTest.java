package com.multitenanterp.payroll;
import com.multitenanterp.employee.DocumentStorage;import com.multitenanterp.platform.tenant.TenantContext;import org.h2.jdbcx.JdbcDataSource;import org.junit.jupiter.api.*;import org.springframework.core.io.*;import org.springframework.jdbc.core.simple.JdbcClient;import org.springframework.web.server.ResponseStatusException;import java.io.*;import java.util.*;import static org.assertj.core.api.Assertions.*;
class PayslipServiceIntegrationTest{
 private JdbcClient db;private PayslipService service;private UUID tenant,other,payslip;
 @BeforeEach void setup(){tenant=UUID.randomUUID();other=UUID.randomUUID();JdbcDataSource ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");db=JdbcClient.create(ds);schema();new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(new ClassPathResource("db/migration/V23__payslip_acknowledgement.sql")).execute(ds);seed(tenant,"LOCKED","EMP-1");seed(other,"LOCKED","EMP-2");service=new PayslipService(db,new MemoryStorage(),new PayslipPdfGenerator());TenantContext.set(tenant);}
 @AfterEach void clear(){TenantContext.clear();}
 @Test void generatesReleasesDownloadsAndIsolatesTenantPayslips(){List<Payslip> generated=service.generate(2026,9,"payroll@example.com");assertThat(generated).singleElement().satisfies(p->{assertThat(p.status()).isEqualTo("GENERATED");assertThat(p.sha256()).hasSize(64);assertThat(p.sizeBytes()).isPositive();payslip=p.id();});assertThat(service.generate(2026,9,"payroll@example.com")).hasSize(1);assertThat(service.release(2026,9,"manager@example.com").getFirst().status()).isEqualTo("RELEASED");assertThat(service.download(2026,9,payslip).resource().exists()).isTrue();TenantContext.set(other);assertThat(service.list(2026,9)).isEmpty();assertThatThrownBy(()->service.download(2026,9,payslip)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("Payslip not found");}
 @Test void rejectsGenerationBeforePayrollIsLocked(){db.sql("UPDATE payroll_run SET status='APPROVED' WHERE tenant_id=?").param(tenant).update();assertThatThrownBy(()->service.generate(2026,9,"payroll@example.com")).isInstanceOf(ResponseStatusException.class).hasMessageContaining("locked or paid");}
 @Test void selfServiceRequiresOwnerReleaseAndFinalPayroll(){
  Payslip slip=service.generate(2026,9,"payroll").getFirst();UUID employee=slip.employmentId();
  assertThat(service.releasedForEmployee(employee)).isEmpty();
  assertThatThrownBy(()->service.downloadReleased(slip.id(),employee)).isInstanceOf(ResponseStatusException.class);
  service.release(2026,9,"manager");
  assertThat(service.releasedForEmployee(employee)).hasSize(1);
  assertThat(service.downloadReleased(slip.id(),employee).resource().exists()).isTrue();
  assertThat(service.releasedForEmployee(UUID.randomUUID())).isEmpty();
  assertThatThrownBy(()->service.downloadReleased(slip.id(),UUID.randomUUID())).isInstanceOf(ResponseStatusException.class);
  TenantContext.set(other);
  assertThat(service.releasedForEmployee(employee)).isEmpty();
  assertThatThrownBy(()->service.downloadReleased(slip.id(),employee)).isInstanceOf(ResponseStatusException.class);
  TenantContext.set(tenant);db.sql("UPDATE payroll_run SET status='REVERSED' WHERE tenant_id=?").param(tenant).update();
  assertThat(service.releasedForEmployee(employee)).isEmpty();
  assertThatThrownBy(()->service.downloadReleased(slip.id(),employee)).isInstanceOf(ResponseStatusException.class);
 }
 @Test void acknowledgementIsExplicitAndPreservesFirstReceipt() {
  Payslip slip=service.generate(2026,9,"payroll").getFirst();
  service.release(2026,9,"manager");
  assertThat(service.downloadReleased(slip.id(),slip.employmentId()).metadata().acknowledgedAt()).isNull();
  Payslip first=service.acknowledge(slip.id(),slip.employmentId(),"employee-user");
  assertThat(first.acknowledgedAt()).isNotNull();
  assertThat(first.acknowledgedBy()).isEqualTo("employee-user");
  Payslip retry=service.acknowledge(slip.id(),slip.employmentId(),"another-linked-user");
  assertThat(retry.acknowledgedAt()).isEqualTo(first.acknowledgedAt());
  assertThat(retry.acknowledgedBy()).isEqualTo(first.acknowledgedBy());
  assertThat(service.list(2026,9).getFirst().acknowledgedAt()).isEqualTo(first.acknowledgedAt());
  assertThat(service.releasedForEmployee(slip.employmentId()).getFirst().acknowledgedAt()).isEqualTo(first.acknowledgedAt());
 }
 @Test void acknowledgementRejectsUnreleasedOtherEmployeesOtherTenantsAndReversedPayroll() {
  Payslip slip=service.generate(2026,9,"payroll").getFirst();
  assertThatThrownBy(()->service.acknowledge(slip.id(),slip.employmentId(),"user")).isInstanceOf(ResponseStatusException.class);
  service.release(2026,9,"manager");
  assertThatThrownBy(()->service.acknowledge(slip.id(),UUID.randomUUID(),"user")).isInstanceOf(ResponseStatusException.class);
  TenantContext.set(other);
  assertThatThrownBy(()->service.acknowledge(slip.id(),slip.employmentId(),"user")).isInstanceOf(ResponseStatusException.class);
  TenantContext.set(tenant);
  db.sql("UPDATE payroll_run SET status='REVERSED' WHERE tenant_id=?").param(tenant).update();
  assertThatThrownBy(()->service.acknowledge(slip.id(),slip.employmentId(),"user")).isInstanceOf(ResponseStatusException.class);
  assertThat(service.list(2026,9).getFirst().acknowledgedAt()).isNull();
 }
 private void seed(UUID t,String status,String number){UUID person=UUID.randomUUID(),employment=UUID.randomUUID(),run=UUID.randomUUID(),result=UUID.randomUUID();db.sql("INSERT INTO tenant VALUES(?,?)").params(t,"Acme "+number).update();db.sql("INSERT INTO company_profile VALUES(?,?)").params(t,"Bengaluru").update();db.sql("INSERT INTO person(id,tenant_id,first_name,last_name) VALUES(?,?,?,'Kumar')").params(person,t,"Asha").update();db.sql("INSERT INTO employment VALUES(?,?,?,?)").params(employment,t,person,number).update();db.sql("INSERT INTO payroll_run VALUES(?,?,2026,9,?)").params(run,t,status).update();db.sql("INSERT INTO payroll_employee_result VALUES(?,?,?,?,75000,8250,66750,30,29.5)").params(result,t,run,employment).update();db.sql("INSERT INTO payroll_component_result VALUES(?,?,?,?,?,?)").params(UUID.randomUUID(),t,result,"Basic salary","EARNING",75000).update();db.sql("INSERT INTO payroll_adjustment_result VALUES(?,?,?,?,?,?)").params(UUID.randomUUID(),t,result,"Income tax","DEDUCTION",8250).update();}
 private void schema(){db.sql("CREATE TABLE tenant(id UUID PRIMARY KEY,legal_name VARCHAR)").update();db.sql("CREATE TABLE company_profile(tenant_id UUID PRIMARY KEY,registered_address VARCHAR)").update();db.sql("CREATE TABLE person(id UUID PRIMARY KEY,tenant_id UUID,first_name VARCHAR,middle_name VARCHAR,last_name VARCHAR,UNIQUE(id,tenant_id))").update();db.sql("CREATE TABLE employment(id UUID PRIMARY KEY,tenant_id UUID,person_id UUID,employee_number VARCHAR,UNIQUE(id,tenant_id))").update();db.sql("CREATE TABLE payroll_run(id UUID PRIMARY KEY,tenant_id UUID,payroll_year INT,payroll_month INT,status VARCHAR,UNIQUE(id,tenant_id),UNIQUE(tenant_id,payroll_year,payroll_month))").update();db.sql("CREATE TABLE payroll_employee_result(id UUID PRIMARY KEY,tenant_id UUID,payroll_run_id UUID,employment_id UUID,gross_pay NUMERIC,deductions NUMERIC,net_pay NUMERIC,period_days NUMERIC,payable_days NUMERIC,UNIQUE(id,tenant_id))").update();db.sql("CREATE TABLE payroll_component_result(id UUID,tenant_id UUID,payroll_employee_result_id UUID,component_name VARCHAR,component_type VARCHAR,amount NUMERIC)").update();db.sql("CREATE TABLE payroll_adjustment_result(id UUID,tenant_id UUID,payroll_employee_result_id UUID,name VARCHAR,result_type VARCHAR,amount NUMERIC)").update();db.sql("CREATE TABLE payslip(id UUID PRIMARY KEY,tenant_id UUID,payroll_run_id UUID,payroll_employee_result_id UUID,employment_id UUID,file_name VARCHAR,storage_key VARCHAR,size_bytes BIGINT,sha256 VARCHAR,status VARCHAR,generated_by VARCHAR,generated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,released_by VARCHAR,released_at TIMESTAMP WITH TIME ZONE,UNIQUE(tenant_id,payroll_run_id,employment_id))").update();}
 private static class MemoryStorage implements DocumentStorage{private final Map<String,byte[]> values=new HashMap<>();public void store(String key,InputStream content)throws IOException{values.put(key,content.readAllBytes());}public Resource load(String key){byte[] value=values.get(key);return value==null?new Missing():new ByteArrayResource(value);}public void delete(String key){values.remove(key);}private static class Missing extends ByteArrayResource{Missing(){super(new byte[0]);}@Override public boolean exists(){return false;}}}
}
