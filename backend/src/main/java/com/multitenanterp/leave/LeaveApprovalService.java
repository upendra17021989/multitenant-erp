package com.multitenanterp.leave;

import com.multitenanterp.platform.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.sql.*;
import java.time.Instant;
import java.util.*;

@Service
public class LeaveApprovalService {
    public record Step(UUID id,int stepNumber,String approverType,UUID managerEmploymentId,String status,
                       String decidedBy,Instant decidedAt,String comment) {}
    public record Inbox(UUID requestId,UUID stepId,String employeeNumber,String employeeName,String leaveType,
                        java.time.LocalDate startDate,java.time.LocalDate endDate,java.math.BigDecimal days,
                        String reason,int stepNumber,String approverType) {}
    public record Notification(UUID id,UUID requestId,String message,Instant createdAt,Instant readAt) {}
    private record Request(UUID employee,String actor,String status) {}
    private final JdbcClient db;
    public LeaveApprovalService(JdbcClient db){this.db=db;}

    public List<String> policy(){
        var rows=db.sql("SELECT approver_type FROM leave_approval_policy WHERE tenant_id=:tenant ORDER BY step_number")
                .param("tenant",tenant()).query(String.class).list();
        return rows.isEmpty()?List.of("HR_MANAGER"):rows;
    }
    @Transactional public List<String> configure(List<String> chain,String actor){
        if(chain==null||chain.isEmpty()||chain.size()>3||new HashSet<>(chain).size()!=chain.size()
                ||chain.stream().anyMatch(s->s==null||!Set.of("REPORTING_MANAGER","HR_MANAGER","COMPANY_ADMIN").contains(s)))
            throw problem(HttpStatus.BAD_REQUEST,"Choose one to three distinct approval stages");
        lockTenant();
        db.sql("DELETE FROM leave_approval_policy WHERE tenant_id=:tenant").param("tenant",tenant()).update();
        for(int i=0;i<chain.size();i++)db.sql("INSERT INTO leave_approval_policy(tenant_id,step_number,approver_type,updated_by) VALUES(:tenant,:step,:type,:actor)")
                .param("tenant",tenant()).param("step",i+1).param("type",chain.get(i)).param("actor",actor).update();
        return policy();
    }
    // Called inside LeaveService's tenant-locked transaction, so request, chain and notifications commit together.
    public void start(UUID id){
        Request request=request(id);
        List<String> chain=policy();
        for(int i=0;i<chain.size();i++){
            String type=chain.get(i);UUID manager=null;
            if(type.equals("REPORTING_MANAGER")){
                manager=db.sql("SELECT reporting_manager_employment_id FROM employment WHERE id=:employee AND tenant_id=:tenant")
                        .param("employee",request.employee()).param("tenant",tenant()).query((r,n)->r.getObject(1,UUID.class)).single();
                if(manager==null||manager.equals(request.employee()))throw problem(HttpStatus.CONFLICT,"Configure a reporting manager for this employee before requesting leave");
            }
            Step step=new Step(UUID.randomUUID(),i+1,type,manager,i==0?"PENDING":"WAITING",null,null,null);
            if(eligible(step,request).isEmpty())throw problem(HttpStatus.CONFLICT,"No eligible approver for stage "+(i+1)+" ("+type+"). Check company roles and employee user links");
            db.sql("INSERT INTO leave_approval_step(id,tenant_id,request_id,step_number,approver_type,manager_employment_id,status) VALUES(:id,:tenant,:request,:step,:type,:manager,:status)")
                    .param("id",step.id()).param("tenant",tenant()).param("request",id).param("step",i+1)
                    .param("type",type).param("manager",manager).param("status",step.status()).update();
        }
        notifyApprovers(id);notifyEmployee(id,"Leave request submitted for approval.");
    }
    public boolean decide(UUID id,DecideLeaveRequest decision,String actor){
        Request request=request(id);
        Step step=steps(id).stream().filter(s->s.status().equals("PENDING")).findFirst()
                .orElseThrow(()->problem(HttpStatus.CONFLICT,"No pending approval stage"));
        if(decision.stepId()==null||!decision.stepId().equals(step.id()))throw problem(HttpStatus.CONFLICT,"Approval stage changed. Refresh the inbox and try again");
        if(!eligible(step,request).contains(currentUser()))throw problem(HttpStatus.FORBIDDEN,"This approval stage is not assigned to you; self-approval is not allowed");
        if(!Set.of("APPROVED","REJECTED").contains(decision.decision()))throw problem(HttpStatus.BAD_REQUEST,"Invalid decision");
        if(decision.comment()!=null&&decision.comment().length()>1000)throw problem(HttpStatus.BAD_REQUEST,"Comment is too long");
        if(decision.decision().equals("REJECTED")&&(decision.comment()==null||decision.comment().isBlank()))throw problem(HttpStatus.BAD_REQUEST,"Enter a reason for rejection");
        db.sql("UPDATE leave_approval_step SET status=:status,decided_by=:actor,decided_at=CURRENT_TIMESTAMP,comment=:comment WHERE id=:id AND tenant_id=:tenant")
                .param("status",decision.decision()).param("actor",actor).param("comment",decision.comment())
                .param("id",step.id()).param("tenant",tenant()).update();
        if(decision.decision().equals("REJECTED")){closeRemaining(id,actor,"Not required after rejection");return true;}
        Optional<Step> next=steps(id).stream().filter(s->s.status().equals("WAITING")).findFirst();
        if(next.isEmpty())return true;
        if(eligible(next.get(),request).isEmpty())throw problem(HttpStatus.CONFLICT,"The next stage has no eligible approver. Restore the required company access before approving");
        db.sql("UPDATE leave_approval_step SET status='PENDING' WHERE id=:id AND tenant_id=:tenant")
                .param("id",next.get().id()).param("tenant",tenant()).update();
        notifyApprovers(id);notifyEmployee(id,"Leave approval stage "+step.stepNumber()+" approved; awaiting stage "+next.get().stepNumber()+".");
        return false;
    }
    public void finished(UUID id,String status){notifyEmployee(id,"Leave request "+status.toLowerCase(Locale.ROOT)+".");}
    public void cancelled(UUID id,String actor){
        notifyApprovers(id,"Leave request was cancelled.");
        closeRemaining(id,actor,"Request cancelled");finished(id,"CANCELLED");
    }
    private void closeRemaining(UUID id,String actor,String comment){
        db.sql("UPDATE leave_approval_step SET status='CANCELLED',decided_by=:actor,decided_at=CURRENT_TIMESTAMP,comment=:comment WHERE tenant_id=:tenant AND request_id=:id AND status IN ('PENDING','WAITING')")
                .param("tenant",tenant()).param("id",id).param("actor",actor).param("comment",comment).update();
    }
    public List<Step> history(UUID id){
        Request r=request(id);UUID user=currentUser();
        boolean admin=TenantContext.roles().stream().anyMatch(Set.of("SYSTEM_ADMIN","GROUP_ADMIN","COMPANY_ADMIN","HR_MANAGER","HR_EXECUTIVE")::contains);
        var rows=steps(id);
        boolean owner=employeeUsers(r.employee()).contains(user)||r.actor().equals(TenantContext.requireAuthUserId().toString());
        boolean participant=rows.stream().anyMatch(s->eligible(s,r).contains(user)||TenantContext.requireAuthUserId().toString().equals(s.decidedBy()));
        if(!admin&&!owner&&!participant)throw problem(HttpStatus.NOT_FOUND,"Leave request not found");
        return rows;
    }
    public List<Inbox> inbox(){
        UUID user=currentUser();
        var candidates=db.sql("""
            SELECT r.id,s.id step_id,e.employee_number,CONCAT(p.first_name,' ',p.last_name) employee_name,
              t.name leave_type,r.start_date,r.end_date,r.requested_days,r.reason,s.step_number,s.approver_type
            FROM leave_request r JOIN leave_approval_step s ON s.request_id=r.id AND s.tenant_id=r.tenant_id
            JOIN employment e ON e.id=r.employment_id AND e.tenant_id=r.tenant_id
            JOIN person p ON p.id=e.person_id AND p.tenant_id=e.tenant_id
            JOIN leave_type t ON t.id=r.leave_type_id AND t.tenant_id=r.tenant_id
            WHERE r.tenant_id=:tenant AND r.status='PENDING' AND s.status='PENDING' ORDER BY r.requested_at
            """).param("tenant",tenant()).query((r,n)->new Inbox(r.getObject("id",UUID.class),r.getObject("step_id",UUID.class),r.getString("employee_number"),
                    r.getString("employee_name"),r.getString("leave_type"),r.getDate("start_date").toLocalDate(),r.getDate("end_date").toLocalDate(),
                    r.getBigDecimal("requested_days"),r.getString("reason"),r.getInt("step_number"),r.getString("approver_type"))).list();
        return candidates.stream().filter(row->steps(row.requestId()).stream().filter(s->s.id().equals(row.stepId()))
                .anyMatch(s->eligible(s,request(row.requestId())).contains(user))).toList();
    }
    public List<Notification> notifications(){
        return db.sql("SELECT * FROM leave_notification WHERE tenant_id=:tenant AND user_id=:user ORDER BY created_at DESC LIMIT 100")
                .param("tenant",tenant()).param("user",currentUser()).query((r,n)->new Notification(r.getObject("id",UUID.class),r.getObject("request_id",UUID.class),
                        r.getString("message"),r.getTimestamp("created_at").toInstant(),instant(r,"read_at"))).list();
    }
    public void markRead(UUID id){
        if(db.sql("UPDATE leave_notification SET read_at=COALESCE(read_at,CURRENT_TIMESTAMP) WHERE id=:id AND tenant_id=:tenant AND user_id=:user")
                .param("id",id).param("tenant",tenant()).param("user",currentUser()).update()==0)throw problem(HttpStatus.NOT_FOUND,"Notification not found");
    }
    private List<Step> steps(UUID id){return db.sql("SELECT * FROM leave_approval_step WHERE tenant_id=:tenant AND request_id=:id ORDER BY step_number")
            .param("tenant",tenant()).param("id",id).query((r,n)->new Step(r.getObject("id",UUID.class),r.getInt("step_number"),r.getString("approver_type"),
                    r.getObject("manager_employment_id",UUID.class),r.getString("status"),r.getString("decided_by"),instant(r,"decided_at"),r.getString("comment"))).list();}
    private Request request(UUID id){return db.sql("SELECT employment_id,requested_by,status FROM leave_request WHERE id=:id AND tenant_id=:tenant")
            .param("id",id).param("tenant",tenant()).query((r,n)->new Request(r.getObject(1,UUID.class),r.getString(2),r.getString(3))).optional()
            .orElseThrow(()->problem(HttpStatus.NOT_FOUND,"Leave request not found"));}
    private List<UUID> eligible(Step step,Request request){
        String filter=switch(step.approverType()){
            case "REPORTING_MANAGER" -> " AND EXISTS(SELECT 1 FROM employee_user_link l WHERE l.tenant_id=r.tenant_id AND l.user_id=u.id AND l.employment_id=:manager)";
            case "HR_MANAGER" -> " AND r.role_code IN ('HR_MANAGER','HR_EXECUTIVE','COMPANY_ADMIN','GROUP_ADMIN','SYSTEM_ADMIN')";
            case "COMPANY_ADMIN" -> " AND r.role_code IN ('COMPANY_ADMIN','GROUP_ADMIN','SYSTEM_ADMIN')";
            default -> throw new IllegalStateException("Unknown approval stage");
        };
        var query=db.sql("SELECT DISTINCT u.id FROM app_user u JOIN user_tenant_role r ON r.user_id=u.id WHERE r.tenant_id=:tenant AND r.revoked_at IS NULL AND u.status='ACTIVE'"+filter+
                " AND CAST(u.auth_user_id AS VARCHAR)<>:actor AND NOT EXISTS(SELECT 1 FROM employee_user_link own WHERE own.tenant_id=r.tenant_id AND own.user_id=u.id AND own.employment_id=:employee)")
                .param("tenant",tenant()).param("actor",request.actor()).param("employee",request.employee());
        if(step.approverType().equals("REPORTING_MANAGER"))query.param("manager",step.managerEmploymentId());
        return query.query(UUID.class).list();
    }
    private List<UUID> employeeUsers(UUID employee){return db.sql("SELECT user_id FROM employee_user_link WHERE tenant_id=:tenant AND employment_id=:employee")
            .param("tenant",tenant()).param("employee",employee).query(UUID.class).list();}
    private UUID currentUser(){return db.sql("SELECT u.id FROM app_user u WHERE u.auth_user_id=:auth AND u.status='ACTIVE' AND EXISTS(SELECT 1 FROM user_tenant_role r WHERE r.user_id=u.id AND r.tenant_id=:tenant AND r.revoked_at IS NULL)")
            .param("auth",TenantContext.requireAuthUserId()).param("tenant",tenant()).query(UUID.class).optional().orElseThrow(()->problem(HttpStatus.FORBIDDEN,"Active company membership required"));}
    private void notifyApprovers(UUID id){notifyApprovers(id,"Leave request needs your approval.");}
    private void notifyApprovers(UUID id,String message){Request r=request(id);steps(id).stream().filter(s->s.status().equals("PENDING")).forEach(s->eligible(s,r).forEach(u->notify(id,u,message)));}
    private void notifyEmployee(UUID id,String message){Request r=request(id);Set<UUID> users=new HashSet<>(employeeUsers(r.employee()));
        db.sql("SELECT id FROM app_user WHERE CAST(auth_user_id AS VARCHAR)=:actor").param("actor",r.actor()).query(UUID.class).optional().ifPresent(users::add);
        users.forEach(u->notify(id,u,message));}
    private void notify(UUID id,UUID user,String message){
        String context=db.sql("SELECT e.employee_number,r.start_date,r.end_date FROM leave_request r JOIN employment e ON e.id=r.employment_id AND e.tenant_id=r.tenant_id WHERE r.id=:id AND r.tenant_id=:tenant")
                .param("id",id).param("tenant",tenant()).query((r,n)->r.getString(1)+": "+r.getDate(2)+" to "+r.getDate(3)).single();
        db.sql("INSERT INTO leave_notification(id,tenant_id,request_id,user_id,message) VALUES(:id,:tenant,:request,:user,:message)")
                .param("id",UUID.randomUUID()).param("tenant",tenant()).param("request",id).param("user",user).param("message",context+" — "+message).update();
    }
    private void lockTenant(){db.sql("SELECT id FROM tenant WHERE id=:tenant FOR UPDATE").param("tenant",tenant()).query(UUID.class).single();}
    private static UUID tenant(){return TenantContext.requireTenantId();}
    private static Instant instant(ResultSet r,String column)throws SQLException{return r.getTimestamp(column)==null?null:r.getTimestamp(column).toInstant();}
    private static ResponseStatusException problem(HttpStatus status,String message){return new ResponseStatusException(status,message);}
}
