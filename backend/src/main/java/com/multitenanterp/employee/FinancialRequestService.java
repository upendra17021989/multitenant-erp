package com.multitenanterp.employee;

import com.multitenanterp.platform.tenant.TenantContext;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.*;
import java.time.*;
import java.util.*;

@Service
public class FinancialRequestService {
    private final JdbcClient db;
    public FinancialRequestService(JdbcClient db){this.db=db;}
    public record Submit(@NotNull UUID employmentId,@NotBlank @Pattern(regexp="EXPENSE|LOAN|ADVANCE") String requestType,
                         @NotNull @DecimalMin("0.01") @Digits(integer=12,fraction=2) BigDecimal amount,
                         @Min(1) @Max(60) int installments,LocalDate firstRecoveryMonth,
                         @NotBlank @Size(max=1000) String purpose,UUID receiptId) {}
    public record Decision(@NotBlank @Pattern(regexp="APPROVED|REJECTED") String decision,@NotBlank @Size(max=1000) String comment) {}
    public record Payment(@NotBlank @Size(max=200) String reference) {}
    public record Request(UUID id,UUID employmentId,String requestType,BigDecimal amount,int installments,LocalDate firstRecoveryMonth,
                          String purpose,UUID receiptId,String status,String requestedBy,String decidedBy,String decisionComment,String paymentReference) {}
    public record Recovery(int year,int month,BigDecimal amount,boolean includedInFinalPayroll) {}
    public List<Request> list(UUID employee) {
        var query=db.sql("SELECT * FROM employee_financial_request WHERE tenant_id=:tenant"+(employee==null?"":" AND employment_id=:employee")+" ORDER BY requested_at DESC").param("tenant",tenant());
        if(employee!=null)query.param("employee",employee);
        return query.query((r,n)->new Request(r.getObject("id",UUID.class),r.getObject("employment_id",UUID.class),r.getString("request_type"),r.getBigDecimal("amount"),r.getInt("installments"),r.getDate("first_recovery_month")==null?null:r.getDate("first_recovery_month").toLocalDate(),r.getString("purpose"),r.getObject("receipt_id",UUID.class),r.getString("status"),r.getString("requested_by"),r.getString("decided_by"),r.getString("decision_comment"),r.getString("payment_reference"))).list();
    }
    @Transactional public Request submit(Submit request,String actor) {
        lock();
        if(db.sql("SELECT COUNT(*) FROM employment WHERE id=:employee AND tenant_id=:tenant AND employment_status IN ('ACTIVE','PROBATION','NOTICE')").param("employee",request.employmentId()).param("tenant",tenant()).query(Integer.class).single()==0)throw bad("Active employee not found in this company");
        boolean expense=request.requestType().equals("EXPENSE");
        if(expense&&request.receiptId()==null)throw bad("Expense claims require a receipt");
        if(request.receiptId()!=null&&db.sql("SELECT COUNT(*) FROM employee_document WHERE id=:id AND tenant_id=:tenant AND employment_id=:employee").param("id",request.receiptId()).param("tenant",tenant()).param("employee",request.employmentId()).query(Integer.class).single()==0)throw bad("Receipt must belong to this employee and company");
        if(!expense&&(request.firstRecoveryMonth()==null||request.firstRecoveryMonth().getDayOfMonth()!=1))throw bad("Select the first day of the first recovery month");
        if(!expense&&request.amount().compareTo(BigDecimal.valueOf(request.installments(),2))<0)throw bad("Each installment must be at least 0.01");
        UUID id=UUID.randomUUID();
        db.sql("INSERT INTO employee_financial_request(id,tenant_id,employment_id,request_type,amount,installments,first_recovery_month,purpose,receipt_id,requested_by) VALUES(:id,:tenant,:employee,:type,:amount,:installments,:first,:purpose,:receipt,:actor)")
                .param("id",id).param("tenant",tenant()).param("employee",request.employmentId()).param("type",request.requestType()).param("amount",request.amount()).param("installments",expense?1:request.installments()).param("first",expense?null:request.firstRecoveryMonth()).param("purpose",request.purpose().trim()).param("receipt",request.receiptId()).param("actor",actor).update();
        return find(id,null);
    }
    @Transactional public Request decide(UUID id,Decision decision,String actor) {
        lock();Request request=find(id,null);
        if(!request.status().equals("PENDING"))throw conflict("Only pending requests can be decided");
        if(request.requestedBy().equals(actor))throw conflict("A different user must approve or reject this request");
        db.sql("UPDATE employee_financial_request SET status=:status,decided_by=:actor,decided_at=CURRENT_TIMESTAMP,decision_comment=:comment WHERE id=:id AND tenant_id=:tenant")
                .param("status",decision.decision()).param("actor",actor).param("comment",decision.comment().trim()).param("id",id).param("tenant",tenant()).update();return find(id,null);
    }
    @Transactional public Request recordPayment(UUID id,String reference,String actor) {
        lock();Request request=find(id,null);
        if(!request.status().equals("APPROVED"))throw conflict("Only approved requests can be recorded as paid");
        if(request.requestedBy().equals(actor))throw conflict("The requester cannot record their own payment");
        boolean expense=request.requestType().equals("EXPENSE");
        if(!expense) {
            List<BigDecimal> amounts=installments(request.amount(),request.installments());
            for(int i=0;i<amounts.size();i++) {
                YearMonth period=YearMonth.from(request.firstRecoveryMonth()).plusMonths(i);
                if(period.getYear()<2000||period.getYear()>2200)throw bad("Recovery period is outside supported payroll years");
                if(db.sql("SELECT COUNT(*) FROM payroll_run WHERE tenant_id=:tenant AND payroll_year=:year AND payroll_month=:month AND status NOT IN ('DRAFT','CALCULATED')").param("tenant",tenant()).param("year",period.getYear()).param("month",period.getMonthValue()).query(Integer.class).single()>0)throw conflict("Recovery month is already under review or finalized");
                db.sql("UPDATE payroll_run SET status='DRAFT' WHERE tenant_id=:tenant AND payroll_year=:year AND payroll_month=:month AND status='CALCULATED'").param("tenant",tenant()).param("year",period.getYear()).param("month",period.getMonthValue()).update();
                db.sql("INSERT INTO payroll_variable_input(id,tenant_id,employment_id,payroll_year,payroll_month,code,name,input_type,amount,notes,created_by,financial_request_id) VALUES(:id,:tenant,:employee,:year,:month,:code,:name,'DEDUCTION',:amount,:notes,:actor,:request)")
                        .param("id",UUID.randomUUID()).param("tenant",tenant()).param("employee",request.employmentId()).param("year",period.getYear()).param("month",period.getMonthValue()).param("code","FR"+id.toString().replace("-","")).param("name",request.requestType()+" recovery "+(i+1)+"/"+amounts.size()).param("amount",amounts.get(i)).param("notes","Payment reference: "+reference).param("actor",actor).param("request",id).update();
            }
        }
        db.sql("UPDATE employee_financial_request SET status=:status,payment_reference=:reference,paid_by=:actor,paid_at=CURRENT_TIMESTAMP WHERE id=:id AND tenant_id=:tenant")
                .param("status",expense?"PAID":"DISBURSED").param("reference",reference.trim()).param("actor",actor).param("id",id).param("tenant",tenant()).update();return find(id,null);
    }
    public List<Recovery> recoveries(UUID id,UUID employee) {
        find(id,employee);
        return db.sql("SELECT v.payroll_year,v.payroll_month,v.amount,EXISTS(SELECT 1 FROM payroll_adjustment_result a JOIN payroll_employee_result r ON r.id=a.payroll_employee_result_id AND r.tenant_id=a.tenant_id JOIN payroll_run p ON p.id=r.payroll_run_id AND p.tenant_id=r.tenant_id WHERE a.tenant_id=v.tenant_id AND a.source_type='VARIABLE' AND a.source_id=v.id AND p.status IN ('LOCKED','PAID')) finalized FROM payroll_variable_input v WHERE v.tenant_id=:tenant AND v.financial_request_id=:id ORDER BY v.payroll_year,v.payroll_month")
                .param("tenant",tenant()).param("id",id).query((r,n)->new Recovery(r.getInt(1),r.getInt(2),r.getBigDecimal(3),r.getBoolean(4))).list();
    }
    static List<BigDecimal> installments(BigDecimal total,int count) {
        BigDecimal regular=total.divide(BigDecimal.valueOf(count),2,RoundingMode.DOWN);List<BigDecimal> amounts=new ArrayList<>();
        for(int i=0;i<count;i++)amounts.add(i==count-1?total.subtract(regular.multiply(BigDecimal.valueOf(count-1))):regular);return amounts;
    }
    private Request find(UUID id,UUID employee){return list(employee).stream().filter(r->r.id().equals(id)).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Financial request not found"));}
    private void lock(){db.sql("SELECT id FROM tenant WHERE id=:tenant FOR UPDATE").param("tenant",tenant()).query(UUID.class).single();}
    private static UUID tenant(){return TenantContext.requireTenantId();}
    private static ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
    private static ResponseStatusException conflict(String message){return new ResponseStatusException(HttpStatus.CONFLICT,message);}
}
