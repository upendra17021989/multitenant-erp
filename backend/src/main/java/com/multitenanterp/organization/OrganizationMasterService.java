package com.multitenanterp.organization;

import com.multitenanterp.platform.tenant.TenantContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class OrganizationMasterService {
    private final JdbcClient db;

    public OrganizationMasterService(JdbcClient db) { this.db = db; }

    public List<Department> departments() {
        return db.sql("SELECT id,code,name,parent_department_id,status FROM department WHERE tenant_id=:tenant ORDER BY name")
                .param("tenant", tenant()).query((r,n) -> new Department(r.getObject("id", UUID.class), r.getString("code"), r.getString("name"), r.getObject("parent_department_id", UUID.class), r.getString("status"))).list();
    }
    public Department saveDepartment(UUID id, SaveDepartmentRequest request) {
        boolean create = id == null; id = create ? UUID.randomUUID() : id;
        if (id.equals(request.parentDepartmentId())) throw badRequest("A department cannot be its own parent");
        try {
            int count = create
                    ? db.sql("INSERT INTO department(id,tenant_id,code,name,parent_department_id,status) VALUES(:id,:tenant,:code,:name,:parent,:status)")
                        .param("id",id).param("tenant",tenant()).param("code",code(request.code())).param("name",request.name().trim()).param("parent",request.parentDepartmentId()).param("status",status(request.status())).update()
                    : db.sql("UPDATE department SET code=:code,name=:name,parent_department_id=:parent,status=:status,updated_at=CURRENT_TIMESTAMP WHERE id=:id AND tenant_id=:tenant")
                        .param("id",id).param("tenant",tenant()).param("code",code(request.code())).param("name",request.name().trim()).param("parent",request.parentDepartmentId()).param("status",status(request.status())).update();
            found(count, "Department");
        } catch (DataIntegrityViolationException e) { throw conflict("Department code or parent is invalid for this company"); }
        UUID savedId = id;
        return departments().stream().filter(x -> x.id().equals(savedId)).findFirst().orElseThrow(() -> missing("Department"));
    }

    public List<Grade> grades() {
        return db.sql("SELECT id,code,name,status FROM grade WHERE tenant_id=:tenant ORDER BY name").param("tenant",tenant())
                .query((r,n) -> new Grade(r.getObject("id",UUID.class),r.getString("code"),r.getString("name"),r.getString("status"))).list();
    }
    public Grade saveGrade(UUID id, SaveGradeRequest request) {
        boolean create=id==null; id=create?UUID.randomUUID():id;
        try {
            int count=create
                    ? db.sql("INSERT INTO grade(id,tenant_id,code,name,status) VALUES(:id,:tenant,:code,:name,:status)").param("id",id).param("tenant",tenant()).param("code",code(request.code())).param("name",request.name().trim()).param("status",status(request.status())).update()
                    : db.sql("UPDATE grade SET code=:code,name=:name,status=:status,updated_at=CURRENT_TIMESTAMP WHERE id=:id AND tenant_id=:tenant").param("id",id).param("tenant",tenant()).param("code",code(request.code())).param("name",request.name().trim()).param("status",status(request.status())).update();
            found(count,"Grade");
        } catch(DataIntegrityViolationException e){throw conflict("Grade code already exists in this company");}
        UUID savedId=id; return grades().stream().filter(x->x.id().equals(savedId)).findFirst().orElseThrow(()->missing("Grade"));
    }

    public List<Designation> designations() {
        return db.sql("SELECT id,code,title,grade_id,status FROM designation WHERE tenant_id=:tenant ORDER BY title").param("tenant",tenant())
                .query((r,n)->new Designation(r.getObject("id",UUID.class),r.getString("code"),r.getString("title"),r.getObject("grade_id",UUID.class),r.getString("status"))).list();
    }
    public Designation saveDesignation(UUID id, SaveDesignationRequest request) {
        boolean create=id==null; id=create?UUID.randomUUID():id;
        try {
            int count=create
                    ? db.sql("INSERT INTO designation(id,tenant_id,code,title,grade_id,status) VALUES(:id,:tenant,:code,:title,:grade,:status)").param("id",id).param("tenant",tenant()).param("code",code(request.code())).param("title",request.title().trim()).param("grade",request.gradeId()).param("status",status(request.status())).update()
                    : db.sql("UPDATE designation SET code=:code,title=:title,grade_id=:grade,status=:status,updated_at=CURRENT_TIMESTAMP WHERE id=:id AND tenant_id=:tenant").param("id",id).param("tenant",tenant()).param("code",code(request.code())).param("title",request.title().trim()).param("grade",request.gradeId()).param("status",status(request.status())).update();
            found(count,"Designation");
        } catch(DataIntegrityViolationException e){throw conflict("Designation code or grade is invalid for this company");}
        UUID savedId=id; return designations().stream().filter(x->x.id().equals(savedId)).findFirst().orElseThrow(()->missing("Designation"));
    }

    public List<CostCentre> costCentres() {
        return db.sql("SELECT id,code,name,accounting_reference,status FROM cost_centre WHERE tenant_id=:tenant ORDER BY name").param("tenant",tenant())
                .query((r,n)->new CostCentre(r.getObject("id",UUID.class),r.getString("code"),r.getString("name"),r.getString("accounting_reference"),r.getString("status"))).list();
    }
    public CostCentre saveCostCentre(UUID id, SaveCostCentreRequest request) {
        boolean create=id==null; id=create?UUID.randomUUID():id;
        try {
            int count=create
                    ? db.sql("INSERT INTO cost_centre(id,tenant_id,code,name,accounting_reference,status) VALUES(:id,:tenant,:code,:name,:reference,:status)").param("id",id).param("tenant",tenant()).param("code",code(request.code())).param("name",request.name().trim()).param("reference",blank(request.accountingReference())).param("status",status(request.status())).update()
                    : db.sql("UPDATE cost_centre SET code=:code,name=:name,accounting_reference=:reference,status=:status,updated_at=CURRENT_TIMESTAMP WHERE id=:id AND tenant_id=:tenant").param("id",id).param("tenant",tenant()).param("code",code(request.code())).param("name",request.name().trim()).param("reference",blank(request.accountingReference())).param("status",status(request.status())).update();
            found(count,"Cost centre");
        } catch(DataIntegrityViolationException e){throw conflict("Cost-centre code already exists in this company");}
        UUID savedId=id; return costCentres().stream().filter(x->x.id().equals(savedId)).findFirst().orElseThrow(()->missing("Cost centre"));
    }

    public List<Holiday> holidays(Integer year) {
        String sql="SELECT id,branch_id,holiday_date,name,optional FROM holiday WHERE tenant_id=:tenant"+(year==null?"":" AND EXTRACT(YEAR FROM holiday_date)=:year")+" ORDER BY holiday_date,name";
        var query=db.sql(sql).param("tenant",tenant()); if(year!=null) query.param("year",year);
        return query.query((r,n)->new Holiday(r.getObject("id",UUID.class),r.getObject("branch_id",UUID.class),r.getDate("holiday_date").toLocalDate(),r.getString("name"),r.getBoolean("optional"))).list();
    }
    public Holiday saveHoliday(UUID id, SaveHolidayRequest request) {
        boolean create=id==null; id=create?UUID.randomUUID():id;
        try {
            int count=create
                    ? db.sql("INSERT INTO holiday(id,tenant_id,branch_id,holiday_date,name,optional) VALUES(:id,:tenant,:branch,:date,:name,:optional)").param("id",id).param("tenant",tenant()).param("branch",request.branchId()).param("date",request.holidayDate()).param("name",request.name().trim()).param("optional",request.optional()).update()
                    : db.sql("UPDATE holiday SET branch_id=:branch,holiday_date=:date,name=:name,optional=:optional,updated_at=CURRENT_TIMESTAMP WHERE id=:id AND tenant_id=:tenant").param("id",id).param("tenant",tenant()).param("branch",request.branchId()).param("date",request.holidayDate()).param("name",request.name().trim()).param("optional",request.optional()).update();
            found(count,"Holiday");
        } catch(DataIntegrityViolationException e){throw conflict("Holiday date or branch is invalid for this company");}
        UUID savedId=id; return holidays(null).stream().filter(x->x.id().equals(savedId)).findFirst().orElseThrow(()->missing("Holiday"));
    }

    private static UUID tenant(){return TenantContext.requireTenantId();}
    private static String code(String value){return value.trim().toUpperCase();}
    private static String status(String value){return value==null?"ACTIVE":value;}
    private static String blank(String value){return value==null||value.isBlank()?null:value.trim();}
    private static void found(int count,String name){if(count==0)throw missing(name);}
    private static ResponseStatusException missing(String name){return new ResponseStatusException(HttpStatus.NOT_FOUND,name+" not found");}
    private static ResponseStatusException conflict(String message){return new ResponseStatusException(HttpStatus.CONFLICT,message);}
    private static ResponseStatusException badRequest(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}