package com.multitenanterp.employee;

import com.multitenanterp.platform.tenant.TenantContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class EmployeeUserLinkService {
    private static final String SELECT="""
            SELECT l.id,l.employment_id,e.employee_number,TRIM(CONCAT(p.first_name,' ',COALESCE(p.middle_name||' ',''),p.last_name)) employee_name,
              u.id user_id,u.auth_user_id,u.email user_email,l.linked_by,l.linked_at
            FROM employee_user_link l
            JOIN employment e ON e.id=l.employment_id AND e.tenant_id=l.tenant_id
            JOIN person p ON p.id=e.person_id AND p.tenant_id=e.tenant_id
            JOIN app_user u ON u.id=l.user_id
            """;
    private final JdbcClient db;
    public EmployeeUserLinkService(JdbcClient db){this.db=db;}

    public List<EmployeeUserLink> links(){return db.sql(SELECT+" WHERE l.tenant_id=:tenant ORDER BY e.employee_number")
            .param("tenant",tenant()).query(EmployeeUserLinkService::map).list();}

    @Transactional
    public EmployeeUserLink link(UUID employmentId,String email,String actor){
        UUID tenant=tenant();
        UUID user=db.sql("""
                SELECT u.id FROM app_user u JOIN user_tenant_role r ON r.user_id=u.id AND r.tenant_id=:tenant AND r.revoked_at IS NULL
                WHERE LOWER(u.email)=:email AND u.status='ACTIVE' LIMIT 1
                """).param("tenant",tenant).param("email",email.trim().toLowerCase(Locale.ROOT)).query(UUID.class).optional()
                .orElseThrow(()->missing("Active tenant user"));
        boolean employeeExists=db.sql("SELECT COUNT(*) FROM employment WHERE id=:employment AND tenant_id=:tenant")
                .param("employment",employmentId).param("tenant",tenant).query(Integer.class).single()>0;
        if(!employeeExists)throw missing("Employee");
        UUID id=UUID.randomUUID();
        try{db.sql("INSERT INTO employee_user_link(id,tenant_id,user_id,employment_id,linked_by) VALUES(:id,:tenant,:user,:employment,:actor)")
                .param("id",id).param("tenant",tenant).param("user",user).param("employment",employmentId).param("actor",actor).update();}
        catch(DataIntegrityViolationException e){throw conflict("The user or employee is already linked in this company");}
        return db.sql(SELECT+" WHERE l.id=:id AND l.tenant_id=:tenant").param("id",id).param("tenant",tenant).query(EmployeeUserLinkService::map).single();
    }

    public void unlink(UUID employmentId){int deleted=db.sql("DELETE FROM employee_user_link WHERE tenant_id=:tenant AND employment_id=:employment")
            .param("tenant",tenant()).param("employment",employmentId).update();if(deleted==0)throw missing("Employee user link");}

    public boolean isCurrentEmployee(UUID employmentId){return TenantContext.currentAuthUserId().map(authUserId->db.sql("""
            SELECT COUNT(*) FROM employee_user_link l JOIN app_user u ON u.id=l.user_id
            WHERE l.tenant_id=:tenant AND l.employment_id=:employment AND u.auth_user_id=:authUser
            """).param("tenant",tenant()).param("employment",employmentId).param("authUser",authUserId).query(Integer.class).single()>0).orElse(false);}

    public UUID currentEmploymentId(){UUID authUser=TenantContext.requireAuthUserId();return db.sql("""
            SELECT l.employment_id FROM employee_user_link l JOIN app_user u ON u.id=l.user_id
            WHERE l.tenant_id=:tenant AND u.auth_user_id=:authUser
            """).param("tenant",tenant()).param("authUser",authUser).query(UUID.class).optional().orElseThrow(()->missing("Employee user link"));}

    private static EmployeeUserLink map(ResultSet r,int n)throws SQLException{return new EmployeeUserLink(r.getObject("id",UUID.class),r.getObject("employment_id",UUID.class),r.getString("employee_number"),r.getString("employee_name"),r.getObject("user_id",UUID.class),r.getObject("auth_user_id",UUID.class),r.getString("user_email"),r.getString("linked_by"),r.getTimestamp("linked_at").toInstant());}
    private static UUID tenant(){return TenantContext.requireTenantId();}
    private static ResponseStatusException missing(String value){return new ResponseStatusException(HttpStatus.NOT_FOUND,value+" not found");}
    private static ResponseStatusException conflict(String value){return new ResponseStatusException(HttpStatus.CONFLICT,value);}
}

