package com.multitenanterp.employee;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.multitenanterp.payroll.*;
import com.multitenanterp.platform.tenant.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
public class HrWorkflowService {
    private static final Set<String> FINANCIAL=Set.of("SALARY_REVISION","EXIT");
    private static final Set<String> ADMIN=Set.of("SYSTEM_ADMIN","GROUP_ADMIN","COMPANY_ADMIN");
    private final JdbcClient db;private final ObjectMapper json;private final SalaryStructureService salaries;
    public HrWorkflowService(JdbcClient db,ObjectMapper json,SalaryStructureService salaries){this.db=db;this.json=json;this.salaries=salaries;}
    public record Configuration(@Min(1) @Max(5) Integer rating,UUID designationId,UUID salaryStructureId,
                                @DecimalMin("0") @Digits(integer=12,fraction=2) BigDecimal annualCtc,
                                @DecimalMin("0") @Digits(integer=12,fraction=2) BigDecimal settlementEarnings,
                                @DecimalMin("0") @Digits(integer=12,fraction=2) BigDecimal settlementDeductions) {}
    public record Create(@NotBlank @Pattern(regexp="RECRUITMENT|ONBOARDING|TRAINING|APPRAISAL|PROMOTION|SALARY_REVISION|EXIT") String workflowType,
                         UUID employmentId,@NotBlank @Size(max=200) String title,@NotBlank @Size(max=4000) String description,
                         @NotNull LocalDate effectiveDate,@NotNull @Valid Configuration configuration) {}
    public record Task(String title,boolean completed,String comment,String completedBy,Instant completedAt) {}
    public record TaskUpdate(boolean completed,@NotBlank @Size(max=1000) String comment) {}
    public record Transition(@NotBlank @Pattern(regexp="SUBMIT|APPROVE|REJECT|COMPLETE") String action,@NotBlank @Size(max=1000) String reason) {}
    public record Workflow(UUID id,UUID employmentId,String workflowType,String title,String description,LocalDate effectiveDate,Configuration configuration,List<Task> tasks,String status,String createdBy) {}
    public record Event(String action,String actor,String reason,String beforeValue,String afterValue,Instant actedAt) {}
    public List<Workflow> list(){return db.sql("SELECT * FROM hr_workflow WHERE tenant_id=:tenant ORDER BY created_at DESC").param("tenant",tenant()).query((r,n)->new Workflow(r.getObject("id",UUID.class),r.getObject("employment_id",UUID.class),r.getString("workflow_type"),r.getString("title"),r.getString("description"),r.getDate("effective_date").toLocalDate(),decode(r.getString("configuration"),new TypeReference<Configuration>(){}),decode(r.getString("tasks"),new TypeReference<List<Task>>(){}),r.getString("status"),r.getString("created_by"))).list().stream().filter(w->permitted(w.workflowType())).toList();}
    @Transactional public Workflow create(Create request,String actor){
        lock();requirePermission(request.workflowType());
        if(!request.workflowType().equals("RECRUITMENT") && request.employmentId()==null)throw bad("This workflow requires an employee");
        if(request.employmentId()!=null && db.sql("SELECT COUNT(*) FROM employment WHERE id=:id AND tenant_id=:tenant").param("id",request.employmentId()).param("tenant",tenant()).query(Integer.class).single()==0)throw bad("Employee not found in this company");
        Configuration config=request.configuration();
        switch(request.workflowType()){
            case "APPRAISAL" -> {if(config.rating()==null)throw bad("Appraisal requires an overall rating from 1 to 5");}
            case "PROMOTION" -> {if(config.designationId()==null || db.sql("SELECT COUNT(*) FROM designation WHERE id=:id AND tenant_id=:tenant").param("id",config.designationId()).param("tenant",tenant()).query(Integer.class).single()==0)throw bad("Select a designation in this company");}
            case "SALARY_REVISION" -> {if(config.salaryStructureId()==null||config.annualCtc()==null||db.sql("SELECT COUNT(*) FROM salary_structure WHERE id=:id AND tenant_id=:tenant AND status='ACTIVE'").param("id",config.salaryStructureId()).param("tenant",tenant()).query(Integer.class).single()==0)throw bad("Select an active salary structure and annual CTC");}
            case "EXIT" -> {if(config.settlementEarnings()==null||config.settlementDeductions()==null)throw bad("Exit requires reviewed settlement earnings and deductions");}
        }
        // Store only fields used by this workflow, so salary data cannot be embedded in a non-payroll case.
        config=switch(request.workflowType()){
            case "APPRAISAL" -> new Configuration(config.rating(),null,null,null,null,null);
            case "PROMOTION" -> new Configuration(null,config.designationId(),null,null,null,null);
            case "SALARY_REVISION" -> new Configuration(null,null,config.salaryStructureId(),config.annualCtc(),null,null);
            case "EXIT" -> new Configuration(null,null,null,null,config.settlementEarnings(),config.settlementDeductions());
            default -> new Configuration(null,null,null,null,null,null);
        };
        UUID id=UUID.randomUUID();List<Task> tasks=defaults(request.workflowType()).stream().map(title->new Task(title,false,"",null,null)).toList();
        db.sql("INSERT INTO hr_workflow(id,tenant_id,employment_id,workflow_type,title,description,effective_date,configuration,tasks,created_by) VALUES(:id,:tenant,:employee,:type,:title,:description,:date,:config,:tasks,:actor)")
                .param("id",id).param("tenant",tenant()).param("employee",request.employmentId()).param("type",request.workflowType()).param("title",request.title().trim()).param("description",request.description().trim()).param("date",request.effectiveDate()).param("config",encode(config)).param("tasks",encode(tasks)).param("actor",actor).update();
        Workflow saved=find(id);audit(id,"CREATE",actor,"Workflow created",null,encode(saved));return saved;
    }
    @Transactional public Workflow task(UUID id,int index,TaskUpdate update,String actor){
        lock();Workflow before=find(id);if(!before.status().equals("IN_PROGRESS"))throw conflict("Tasks can only change while work is in progress");
        if(index<0||index>=before.tasks().size())throw bad("Unknown task");List<Task> tasks=new ArrayList<>(before.tasks());Task old=tasks.get(index);tasks.set(index,new Task(old.title(),update.completed(),update.comment().trim(),actor,Instant.now()));
        db.sql("UPDATE hr_workflow SET tasks=:tasks,updated_at=CURRENT_TIMESTAMP WHERE id=:id AND tenant_id=:tenant").param("tasks",encode(tasks)).param("id",id).param("tenant",tenant()).update();
        Workflow after=find(id);audit(id,"TASK",actor,update.comment(),encode(before),encode(after));return after;
    }
    @Transactional public Workflow transition(UUID id,Transition transition,String actor){
        lock();Workflow before=find(id);String to;
        switch(transition.action()){
            case "SUBMIT" -> {requireState(before,"IN_PROGRESS");if(before.tasks().stream().anyMatch(t->!t.completed()))throw conflict("Complete every checklist task before review");to="UNDER_REVIEW";}
            case "APPROVE","REJECT" -> {requireState(before,"UNDER_REVIEW");if(before.createdBy().equals(actor)||before.tasks().stream().anyMatch(t->actor.equals(t.completedBy())))throw conflict("An independent reviewer must approve or reject this workflow");to=transition.action().equals("APPROVE")?"APPROVED":"REJECTED";}
            case "COMPLETE" -> {requireState(before,"APPROVED");apply(before,actor,transition.reason());to="COMPLETED";}
            default -> throw bad("Unknown workflow action");
        }
        db.sql("UPDATE hr_workflow SET status=:status,updated_at=CURRENT_TIMESTAMP WHERE id=:id AND tenant_id=:tenant").param("status",to).param("id",id).param("tenant",tenant()).update();
        Workflow after=find(id);audit(id,transition.action(),actor,transition.reason(),encode(before),encode(after));return after;
    }
    public List<Event> history(UUID id){find(id);return db.sql("SELECT * FROM hr_workflow_event WHERE tenant_id=:tenant AND workflow_id=:id ORDER BY acted_at,id").param("tenant",tenant()).param("id",id).query((r,n)->new Event(r.getString("action"),r.getString("actor"),r.getString("reason"),r.getString("before_value"),r.getString("after_value"),r.getTimestamp("acted_at").toInstant())).list();}
    private void apply(Workflow workflow,String actor,String reason){
        String type=workflow.workflowType();
        if(Set.of("PROMOTION","EXIT").contains(type)&&workflow.effectiveDate().isAfter(LocalDate.now()))throw conflict("Complete this workflow on or after its effective date");
        if(Set.of("PROMOTION","SALARY_REVISION","EXIT").contains(type)){
            String before=db.sql("SELECT CONCAT(employment_status,'|',COALESCE(CAST(designation_id AS VARCHAR),''),'|',COALESCE(CAST(exit_date AS VARCHAR),'')) FROM employment WHERE id=:employee AND tenant_id=:tenant").param("employee",workflow.employmentId()).param("tenant",tenant()).query(String.class).single();
            if(type.equals("SALARY_REVISION")) salaries.assign(new SaveEmployeeSalaryAssignmentRequest(workflow.employmentId(),workflow.configuration().salaryStructureId(),workflow.effectiveDate(),null,workflow.configuration().annualCtc(),"Approved workflow "+workflow.id()),actor);
            if(type.equals("PROMOTION")) db.sql("UPDATE employment SET designation_id=:designation,updated_at=CURRENT_TIMESTAMP WHERE id=:employee AND tenant_id=:tenant").param("designation",workflow.configuration().designationId()).param("employee",workflow.employmentId()).param("tenant",tenant()).update();
            if(type.equals("EXIT")){
                LocalDate joining=db.sql("SELECT joining_date FROM employment WHERE id=:employee AND tenant_id=:tenant").param("employee",workflow.employmentId()).param("tenant",tenant()).query(LocalDate.class).single();
                if(workflow.effectiveDate().isBefore(joining))throw bad("Exit date cannot precede joining date");
                db.sql("UPDATE employment SET employment_status='EXITED',exit_date=:date,exit_reason=:reason,updated_at=CURRENT_TIMESTAMP WHERE id=:employee AND tenant_id=:tenant").param("date",workflow.effectiveDate()).param("reason",workflow.title()).param("employee",workflow.employmentId()).param("tenant",tenant()).update();
            }
            audit(workflow.id(),"EMPLOYMENT_CHANGE",actor,reason,before,encode(workflow));
        }
    }
    private static List<String> defaults(String type){return switch(type){
        case "RECRUITMENT" -> List.of("Requisition and budget approved","Candidate screened and consent recorded","Interviews and reference checks completed","Offer decision and candidate response recorded");
        case "ONBOARDING" -> List.of("Signed employment contract verified","Identity and bank documents verified","Employee and payroll details configured","Access and induction completed");
        case "TRAINING" -> List.of("Learning objectives and trainer assigned","Attendance recorded","Assessment and completion evidence recorded");
        case "APPRAISAL" -> List.of("Goals and review period recorded","Employee self-review recorded","Reviewer assessment and rating justified","Feedback discussion recorded");
        case "PROMOTION" -> List.of("Role and designation change justified","Budget and reporting-line impact reviewed","Employee communication prepared");
        case "SALARY_REVISION" -> List.of("Salary proposal and effective date reviewed","Budget approval recorded","Salary structure and CTC verified");
        case "EXIT" -> List.of("Resignation and last working day confirmed","Assets and access clearance completed","Outstanding loans and advances reconciled","Leave and statutory settlement calculations verified","Final settlement payment or recovery reference recorded");
        default -> throw bad("Unknown workflow type");
    };}
    private Workflow find(UUID id){return list().stream().filter(w->w.id().equals(id)).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Workflow not found"));}
    private static boolean permitted(String type){return TenantContext.roles().stream().anyMatch(ADMIN::contains)||TenantContext.roles().contains(FINANCIAL.contains(type)?"PAYROLL_MANAGER":"HR_MANAGER");}
    private static void requirePermission(String type){if(!permitted(type))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Workflow role not permitted");}
    private static void requireState(Workflow workflow,String expected){if(!workflow.status().equals(expected))throw conflict("Workflow must be "+expected);}
    private void audit(UUID id,String action,String actor,String reason,String before,String after){db.sql("INSERT INTO hr_workflow_event(id,tenant_id,workflow_id,action,actor,reason,before_value,after_value) VALUES(:id,:tenant,:workflow,:action,:actor,:reason,:before,:after)").param("id",UUID.randomUUID()).param("tenant",tenant()).param("workflow",id).param("action",action).param("actor",actor).param("reason",reason).param("before",before).param("after",after).update();}
    private String encode(Object value){try{return json.writeValueAsString(value);}catch(JsonProcessingException e){throw new IllegalStateException(e);}}
    private <T>T decode(String value,TypeReference<T> type){try{return json.readValue(value,type);}catch(JsonProcessingException e){throw new IllegalStateException(e);}}
    private void lock(){db.sql("SELECT id FROM tenant WHERE id=:tenant FOR UPDATE").param("tenant",tenant()).query(UUID.class).single();}
    private static UUID tenant(){return TenantContext.requireTenantId();}
    private static ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
    private static ResponseStatusException conflict(String message){return new ResponseStatusException(HttpStatus.CONFLICT,message);}
}
