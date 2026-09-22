package com.multitenanterp.leave;
import com.multitenanterp.platform.tenant.TenantContext;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class LeaveServiceIntegrationTest {
 private JdbcClient db;private LeaveService service;private LeaveApprovalService approvals;private UUID hrAuth,ownerAuth,managerAuth,managerEmployment,adminAuth;private UUID tenantA,tenantB,employeeA,typeA;
 @BeforeEach void setup(){tenantA=UUID.randomUUID();tenantB=UUID.randomUUID();employeeA=UUID.randomUUID();JdbcDataSource ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");db=JdbcClient.create(ds);schema();db.sql("INSERT INTO tenant VALUES(?),(?)").params(tenantA,tenantB).update();db.sql("INSERT INTO employment(id,tenant_id) VALUES(?,?),(?,?)").params(employeeA,tenantA,UUID.randomUUID(),tenantB).update();initializeWorkflow(ds);TenantContext.set(hrAuth,tenantA,java.util.Set.of("HR_MANAGER"));typeA=service.saveType(null,type("CL")).id();db.sql("INSERT INTO leave_balance(id,tenant_id,employment_id,leave_type_id,leave_year,opening_balance,accrued,adjusted,used) VALUES(?,?,?,?,?,?,?,?,?)").params(UUID.randomUUID(),tenantA,employeeA,typeA,2026,new BigDecimal("10"),BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO).update();}
 @AfterEach void clear(){TenantContext.clear();}
 @Test void requestApprovalConsumesBalance(){LeaveRequest request=service.request(new CreateLeaveRequest(employeeA,typeA,LocalDate.of(2026,9,15),LocalDate.of(2026,9,16),new BigDecimal("2"),"Family event"),"employee@example.com");assertThat(request.status()).isEqualTo("PENDING");assertThat(service.decide(request.id(),new DecideLeaveRequest("APPROVED","Approved",approvals.history(request.id()).getFirst().id()),"hr@example.com").status()).isEqualTo("APPROVED");assertThat(service.balances(employeeA,2026).getFirst().available()).isEqualByComparingTo("8");}
 @Test void typesAndRequestsAreTenantIsolated(){service.request(new CreateLeaveRequest(employeeA,typeA,LocalDate.of(2026,10,1),LocalDate.of(2026,10,1),BigDecimal.ONE,"Personal"),"user");TenantContext.set(tenantB);assertThat(service.types()).isEmpty();assertThat(service.requests(null,null)).isEmpty();assertThatThrownBy(()->service.request(new CreateLeaveRequest(employeeA,typeA,LocalDate.of(2026,10,2),LocalDate.of(2026,10,2),BigDecimal.ONE,"Invalid"),"user")).isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode()).isIn(HttpStatus.NOT_FOUND,HttpStatus.CONFLICT));}
 @Test void rejectsOverlapAndInsufficientBalance(){service.request(new CreateLeaveRequest(employeeA,typeA,LocalDate.of(2026,11,1),LocalDate.of(2026,11,3),new BigDecimal("3"),"First"),"user");assertThatThrownBy(()->service.request(new CreateLeaveRequest(employeeA,typeA,LocalDate.of(2026,11,3),LocalDate.of(2026,11,4),BigDecimal.ONE,"Overlap"),"user")).isInstanceOf(ResponseStatusException.class);assertThatThrownBy(()->service.request(new CreateLeaveRequest(employeeA,typeA,LocalDate.of(2026,12,1),LocalDate.of(2026,12,9),new BigDecimal("8"),"Too much"),"user")).isInstanceOf(ResponseStatusException.class);}
 private void initializeWorkflow(JdbcDataSource ds){
  db.sql("CREATE TABLE app_user(id UUID PRIMARY KEY,auth_user_id UUID,status VARCHAR)").update();
  db.sql("CREATE TABLE user_tenant_role(user_id UUID,tenant_id UUID,role_code VARCHAR,revoked_at TIMESTAMP)").update();
  db.sql("CREATE TABLE employee_user_link(tenant_id UUID,user_id UUID,employment_id UUID)").update();
  db.sql("CREATE TABLE person(id UUID,tenant_id UUID,first_name VARCHAR,last_name VARCHAR)").update();
  new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(new org.springframework.core.io.ClassPathResource("db/migration/V25__leave_approval_chains.sql")).execute(ds);
  approvals=new LeaveApprovalService(db);
  var proxy=new org.springframework.aop.framework.ProxyFactory(new LeaveService(db,approvals));
  proxy.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(new org.springframework.jdbc.datasource.DataSourceTransactionManager(ds),new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));
  service=(LeaveService)proxy.getProxy();
  hrAuth=addUser("HR_MANAGER",null);ownerAuth=addUser("EMPLOYEE",employeeA);
  managerEmployment=UUID.randomUUID();db.sql("INSERT INTO employment(id,tenant_id) VALUES(?,?)").params(managerEmployment,tenantA).update();
  managerAuth=addUser("EMPLOYEE",managerEmployment);adminAuth=addUser("COMPANY_ADMIN",null);
  UUID person=UUID.randomUUID();db.sql("INSERT INTO person VALUES(?,?,'Jane','Doe')").params(person,tenantA).update();
  db.sql("UPDATE employment SET person_id=?,employee_number='E1',reporting_manager_employment_id=? WHERE id=?").params(person,managerEmployment,employeeA).update();
 }
 private UUID addUser(String role,UUID employment){
  UUID user=UUID.randomUUID(),auth=UUID.randomUUID();
  db.sql("INSERT INTO app_user VALUES(?,?,'ACTIVE')").params(user,auth).update();
  db.sql("INSERT INTO user_tenant_role(user_id,tenant_id,role_code) VALUES(?,?,?)").params(user,tenantA,role).update();
  if(employment!=null)db.sql("INSERT INTO employee_user_link VALUES(?,?,?)").params(tenantA,user,employment).update();
  return auth;
 }
 private void as(UUID user){TenantContext.set(user,tenantA,java.util.Set.of(user.equals(hrAuth)?"HR_MANAGER":user.equals(adminAuth)?"COMPANY_ADMIN":"EMPLOYEE"));}
 private LeaveRequest submit(){as(ownerAuth);return service.request(new CreateLeaveRequest(employeeA,typeA,LocalDate.of(2026,9,15),LocalDate.of(2026,9,16),new BigDecimal("2"),"Family event"),ownerAuth.toString());}
 private LeaveRequest approve(UUID request,UUID actor,UUID step){as(actor);return service.decide(request,new DecideLeaveRequest("APPROVED","Reviewed",step),actor.toString());}
 private SaveLeaveTypeRequest type(String code){return new SaveLeaveTypeRequest(code,"Casual Leave",true,new BigDecimal("12"),"ANNUAL",new BigDecimal("0.5"),new BigDecimal("5"),true,new BigDecimal("3"),false,false,"ACTIVE");}

 @Test void twoStageChainSnapshotsPolicyAndConsumesBalanceOnlyOnceAtFinalApproval(){
  approvals.configure(java.util.List.of("REPORTING_MANAGER","HR_MANAGER"),hrAuth.toString());
  LeaveRequest request=submit();
  var original=approvals.history(request.id());assertThat(original).hasSize(2);
  assertThat(approvals.inbox()).isEmpty();
  assertThatThrownBy(()->approve(request.id(),ownerAuth,original.getFirst().id())).isInstanceOf(ResponseStatusException.class);
  as(hrAuth);assertThat(approvals.inbox()).isEmpty();
  assertThatThrownBy(()->approve(request.id(),hrAuth,original.getFirst().id())).isInstanceOf(ResponseStatusException.class);
  as(managerAuth);assertThat(approvals.inbox()).singleElement().satisfies(row->assertThat(row.employeeName()).isEqualTo("Jane Doe"));
  assertThat(approvals.notifications()).hasSize(1);
  approvals.configure(java.util.List.of("COMPANY_ADMIN"),hrAuth.toString());
  assertThat(approve(request.id(),managerAuth,original.getFirst().id()).status()).isEqualTo("PENDING");
  assertThat(service.balances(employeeA,2026).getFirst().used()).isEqualByComparingTo("0");
  assertThatThrownBy(()->approve(request.id(),managerAuth,original.getFirst().id())).isInstanceOf(ResponseStatusException.class);
  as(hrAuth);assertThat(approvals.inbox()).hasSize(1);assertThat(approvals.notifications()).hasSize(1);
  assertThat(approve(request.id(),hrAuth,original.get(1).id()).status()).isEqualTo("APPROVED");
  assertThat(service.balances(employeeA,2026).getFirst().used()).isEqualByComparingTo("2");
  assertThatThrownBy(()->approve(request.id(),hrAuth,original.get(1).id())).isInstanceOf(ResponseStatusException.class);
  assertThat(approvals.history(request.id())).allSatisfy(step->{assertThat(step.status()).isEqualTo("APPROVED");assertThat(step.decidedAt()).isNotNull();assertThat(step.comment()).isEqualTo("Reviewed");});
  as(ownerAuth);assertThat(approvals.notifications()).hasSize(3);
 }
 @Test void rejectionAndCancellationCloseRemainingStagesWithoutConsumingBalance(){
  approvals.configure(java.util.List.of("REPORTING_MANAGER","HR_MANAGER"),hrAuth.toString());
  var request=submit();var steps=approvals.history(request.id());as(managerAuth);
  assertThatThrownBy(()->service.decide(request.id(),new DecideLeaveRequest("REJECTED","",steps.getFirst().id()),managerAuth.toString())).isInstanceOf(ResponseStatusException.class);
  assertThat(service.decide(request.id(),new DecideLeaveRequest("REJECTED","Team coverage",steps.getFirst().id()),managerAuth.toString()).status()).isEqualTo("REJECTED");
  assertThat(approvals.history(request.id())).extracting(LeaveApprovalService.Step::status).containsExactly("REJECTED","CANCELLED");
  var second=submit();var first=approvals.history(second.id()).getFirst();approve(second.id(),managerAuth,first.id());as(ownerAuth);
  service.cancelOwned(second.id(),ownerAuth.toString(),employeeA);
  assertThat(approvals.history(second.id())).extracting(LeaveApprovalService.Step::status).containsExactly("APPROVED","CANCELLED");
  assertThat(service.balances(employeeA,2026).getFirst().used()).isEqualByComparingTo("0");
  as(hrAuth);assertThat(approvals.inbox()).isEmpty();
 }
 @Test void failedFinalApprovalRollsBackStageHistoryAndNotifications(){
  var request=submit();as(hrAuth);UUID step=approvals.history(request.id()).getFirst().id();
  int notifications=approvals.notifications().size();
  db.sql("UPDATE leave_balance SET opening_balance=0 WHERE tenant_id=?").param(tenantA).update();
  assertThatThrownBy(()->approve(request.id(),hrAuth,step)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("Insufficient leave balance");
  assertThat(approvals.history(request.id()).getFirst().status()).isEqualTo("PENDING");
  assertThat(approvals.history(request.id()).getFirst().decidedAt()).isNull();
  assertThat(approvals.notifications()).hasSize(notifications);
  assertThat(service.requests(employeeA,null).getFirst().status()).isEqualTo("PENDING");
 }
 @Test void noEligibleManagerRollsBackSubmissionAndRevokedApproversLoseAccess(){
  approvals.configure(java.util.List.of("REPORTING_MANAGER","HR_MANAGER"),hrAuth.toString());
  db.sql("DELETE FROM employee_user_link WHERE employment_id=?").param(managerEmployment).update();
  assertThatThrownBy(this::submit).isInstanceOf(ResponseStatusException.class).hasMessageContaining("No eligible approver");
  assertThat(service.requests(employeeA,null)).isEmpty();
  assertThat(db.sql("SELECT COUNT(*) FROM leave_approval_step").query(Integer.class).single()).isZero();
  approvals.configure(java.util.List.of("HR_MANAGER"),hrAuth.toString());var request=submit();
  as(hrAuth);UUID step=approvals.history(request.id()).getFirst().id();
  db.sql("UPDATE user_tenant_role SET revoked_at=CURRENT_TIMESTAMP WHERE user_id IN (SELECT id FROM app_user WHERE auth_user_id=?)").param(hrAuth).update();
  assertThatThrownBy(()->approve(request.id(),hrAuth,step)).isInstanceOf(ResponseStatusException.class);
 }
 @Test void concurrentFinalDecisionsDeductBalanceOnlyOnce()throws Exception{
  var request=submit();as(hrAuth);UUID step=approvals.history(request.id()).getFirst().id();
  var gate=new java.util.concurrent.CountDownLatch(1);
  try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)){
   var tasks=java.util.List.of(hrAuth,adminAuth).stream().map(user->pool.submit(()->{
    try{gate.await();approve(request.id(),user,step);return true;}
    catch(ResponseStatusException e){assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);return false;}
    finally{TenantContext.clear();}
   })).toList();
   gate.countDown();int successes=0;
   for(var task:tasks)if(task.get(10,java.util.concurrent.TimeUnit.SECONDS))successes++;
   assertThat(successes).isEqualTo(1);
  }
  assertThat(service.balances(employeeA,2026).getFirst().used()).isEqualByComparingTo("2");
  assertThat(approvals.history(request.id())).singleElement().satisfies(s->assertThat(s.status()).isEqualTo("APPROVED"));
 }
 @Test void hrRoleDoesNotAllowOwnerOrRequesterToApproveOwnSubmission(){
  db.sql("INSERT INTO user_tenant_role(user_id,tenant_id,role_code) SELECT id,?,'HR_MANAGER' FROM app_user WHERE auth_user_id=?")
          .params(tenantA,ownerAuth).update();
  var request=submit();UUID step=approvals.history(request.id()).getFirst().id();
  assertThatThrownBy(()->approve(request.id(),ownerAuth,step)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("self-approval");
  as(ownerAuth);service.cancelOwned(request.id(),ownerAuth.toString(),employeeA);
  as(hrAuth);var submitted=service.request(new CreateLeaveRequest(employeeA,typeA,LocalDate.of(2026,9,15),LocalDate.of(2026,9,16),new BigDecimal("2"),"Submitted by HR"),hrAuth.toString());
  UUID next=approvals.history(submitted.id()).getFirst().id();
  assertThatThrownBy(()->approve(submitted.id(),hrAuth,next)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("self-approval");
 }
 @Test void invalidPolicyCannotReplaceExistingChain(){
  approvals.configure(java.util.List.of("REPORTING_MANAGER","HR_MANAGER"),hrAuth.toString());
  for(var chain:java.util.List.of(java.util.List.<String>of(),java.util.List.of("HR_MANAGER","HR_MANAGER"),java.util.List.of("UNKNOWN"))){
   assertThatThrownBy(()->approvals.configure(chain,hrAuth.toString())).isInstanceOf(ResponseStatusException.class);
   assertThat(approvals.policy()).containsExactly("REPORTING_MANAGER","HR_MANAGER");
  }
 }
 @Test void historiesAndNotificationsArePrivateAndTenantScoped(){
  var request=submit();UUID notification=approvals.notifications().getFirst().id();
  approvals.markRead(notification);var read=approvals.notifications().getFirst().readAt();approvals.markRead(notification);
  assertThat(approvals.notifications().getFirst().readAt()).isEqualTo(read);
  as(managerAuth);assertThat(approvals.notifications()).isEmpty();
  assertThatThrownBy(()->approvals.history(request.id())).isInstanceOf(ResponseStatusException.class);
  assertThatThrownBy(()->approvals.markRead(notification)).isInstanceOf(ResponseStatusException.class);
  TenantContext.set(hrAuth,tenantB,java.util.Set.of("HR_MANAGER"));
  assertThatThrownBy(()->approvals.history(request.id())).isInstanceOf(ResponseStatusException.class);
  assertThatThrownBy(()->approvals.markRead(notification)).isInstanceOf(ResponseStatusException.class);
  assertThatThrownBy(()->service.decide(request.id(),new DecideLeaveRequest("APPROVED","",UUID.randomUUID()),hrAuth.toString())).isInstanceOf(ResponseStatusException.class);
 }
 @Test void cannotCancelAnotherEmployeesRequest(){
  LeaveRequest request=service.request(new CreateLeaveRequest(employeeA,typeA,LocalDate.of(2026,9,15),LocalDate.of(2026,9,15),BigDecimal.ONE,"Personal"),"owner");
  assertThatThrownBy(()->service.cancelOwned(request.id(),"other",UUID.randomUUID())).isInstanceOf(ResponseStatusException.class);
  assertThat(service.requests(employeeA,null).getFirst().status()).isEqualTo("PENDING");
  assertThat(service.cancelOwned(request.id(),"owner",employeeA).status()).isEqualTo("CANCELLED");
 }
 private void schema(){db.sql("CREATE TABLE tenant(id UUID PRIMARY KEY)").update();db.sql("CREATE TABLE employment(id UUID PRIMARY KEY,tenant_id UUID NOT NULL,reporting_manager_employment_id UUID,person_id UUID,employee_number VARCHAR,UNIQUE(id,tenant_id))").update();db.sql("CREATE TABLE leave_type(id UUID PRIMARY KEY,tenant_id UUID NOT NULL,code VARCHAR(40),name VARCHAR(160),paid BOOLEAN,annual_entitlement NUMERIC(7,2),accrual_frequency VARCHAR(20),minimum_days NUMERIC(7,2),maximum_days NUMERIC(7,2),half_day_allowed BOOLEAN,carry_forward_limit NUMERIC(7,2),negative_balance_allowed BOOLEAN,supporting_document_required BOOLEAN,status VARCHAR(20),updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,UNIQUE(tenant_id,code),UNIQUE(id,tenant_id))").update();db.sql("CREATE TABLE leave_balance(id UUID PRIMARY KEY,tenant_id UUID NOT NULL,employment_id UUID,leave_type_id UUID,leave_year INT,opening_balance NUMERIC(7,2),accrued NUMERIC(7,2),adjusted NUMERIC(7,2),used NUMERIC(7,2),updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,UNIQUE(tenant_id,employment_id,leave_type_id,leave_year),FOREIGN KEY(employment_id,tenant_id) REFERENCES employment(id,tenant_id),FOREIGN KEY(leave_type_id,tenant_id) REFERENCES leave_type(id,tenant_id))").update();db.sql("CREATE TABLE leave_request(id UUID PRIMARY KEY,tenant_id UUID NOT NULL,employment_id UUID,leave_type_id UUID,start_date DATE,end_date DATE,requested_days NUMERIC(7,2),reason VARCHAR(1000),supporting_document_id UUID,status VARCHAR(30) DEFAULT 'PENDING',requested_by VARCHAR(255),requested_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,decided_by VARCHAR(255),decided_at TIMESTAMP WITH TIME ZONE,decision_comment VARCHAR(1000),updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,UNIQUE(id,tenant_id),FOREIGN KEY(employment_id,tenant_id) REFERENCES employment(id,tenant_id),FOREIGN KEY(leave_type_id,tenant_id) REFERENCES leave_type(id,tenant_id))").update();}
}
