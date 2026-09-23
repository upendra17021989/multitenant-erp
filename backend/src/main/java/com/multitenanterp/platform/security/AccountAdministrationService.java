package com.multitenanterp.platform.security;

import com.multitenanterp.platform.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class AccountAdministrationService {
    private static final Set<String> ASSIGNABLE_ROLES=Set.of("EMPLOYEE","HR_EXECUTIVE","HR_MANAGER",
            "PAYROLL_EXECUTIVE","PAYROLL_MANAGER","COMPANY_ADMIN");
    private final JdbcClient db;
    private final SupabaseAuthAdminClient auth;

    public AccountAdministrationService(JdbcClient db,SupabaseAuthAdminClient auth){this.db=db;this.auth=auth;}

    public List<AccountSummary> accounts(){
        UUID tenant=tenant();
        Map<UUID,Builder> accounts=new LinkedHashMap<>();
        db.sql("""
                SELECT u.id,u.auth_user_id,u.email,u.display_name,u.status,r.role_code
                FROM app_user u JOIN user_tenant_role r ON r.user_id=u.id
                WHERE r.tenant_id=:tenant AND r.revoked_at IS NULL ORDER BY u.display_name,u.email,r.role_code
                """).param("tenant",tenant).query((rs,n)->new Object[]{rs.getObject("id",UUID.class),rs.getObject("auth_user_id",UUID.class),
                rs.getString("email"),rs.getString("display_name"),rs.getString("status"),rs.getString("role_code")}).list()
                .forEach(row->accounts.computeIfAbsent((UUID)row[0],id->new Builder(id,(UUID)row[1],(String)row[2],(String)row[3],(String)row[4])).roles.add((String)row[5]));
        db.sql("""
                SELECT l.user_id,l.employment_id,e.employee_number,TRIM(CONCAT(p.first_name,' ',p.last_name)) employee_name
                FROM employee_user_link l JOIN employment e ON e.id=l.employment_id AND e.tenant_id=l.tenant_id
                JOIN person p ON p.id=e.person_id AND p.tenant_id=e.tenant_id WHERE l.tenant_id=:tenant
                """).param("tenant",tenant).query((rs,n)->new Object[]{rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getString(3),rs.getString(4)})
                .list().forEach(row->{Builder b=accounts.get(row[0]);if(b!=null){b.employmentId=(UUID)row[1];b.employeeNumber=(String)row[2];b.employeeName=(String)row[3];}});
        return accounts.values().stream().map(Builder::build).toList();
    }

    @Transactional
    public AccountSummary invite(CreateAccountRequest request,String actor){
        UUID tenant=tenant();
        String email=request.email().trim().toLowerCase(Locale.ROOT);
        Set<String> roles=new TreeSet<>();
        request.roles().forEach(role->roles.add(role.trim().toUpperCase(Locale.ROOT)));
        if(!ASSIGNABLE_ROLES.containsAll(roles))throw bad("One or more roles cannot be assigned here");
        if(db.sql("SELECT COUNT(*) FROM app_user WHERE LOWER(email)=:email").param("email",email).query(Integer.class).single()>0)
            throw conflict("An application user already exists for this email");
        if(request.employmentId()!=null&&db.sql("SELECT COUNT(*) FROM employment WHERE id=:id AND tenant_id=:tenant")
                .param("id",request.employmentId()).param("tenant",tenant).query(Integer.class).single()==0)throw missing("Employee");

        UUID authId=auth.invite(email,request.displayName().trim());
        UUID userId=UUID.randomUUID();
        try {
            db.sql("INSERT INTO app_user(id,auth_user_id,email,display_name,status) VALUES(:id,:auth,:email,:name,'ACTIVE')")
                    .param("id",userId).param("auth",authId).param("email",email).param("name",request.displayName().trim()).update();
            for(String role:roles)db.sql("INSERT INTO user_tenant_role(id,tenant_id,user_id,role_code) VALUES(:id,:tenant,:user,:role)")
                    .param("id",UUID.randomUUID()).param("tenant",tenant).param("user",userId).param("role",role).update();
            if(request.employmentId()!=null)db.sql("INSERT INTO employee_user_link(id,tenant_id,user_id,employment_id,linked_by) VALUES(:id,:tenant,:user,:employee,:actor)")
                    .param("id",UUID.randomUUID()).param("tenant",tenant).param("user",userId).param("employee",request.employmentId()).param("actor",actor).update();
        } catch(RuntimeException exception){auth.delete(authId);throw exception;}
        return accounts().stream().filter(account->account.id().equals(userId)).findFirst().orElseThrow();
    }

    @Transactional
    public AccountSummary setStatus(UUID userId,String status,UUID actingAuthUser){
        String normalized=status.trim().toUpperCase(Locale.ROOT);
        if(!Set.of("ACTIVE","DISABLED").contains(normalized))throw bad("Status must be ACTIVE or DISABLED");
        UUID targetAuth=db.sql("SELECT u.auth_user_id FROM app_user u JOIN user_tenant_role r ON r.user_id=u.id WHERE u.id=:user AND r.tenant_id=:tenant AND r.revoked_at IS NULL")
                .param("user",userId).param("tenant",tenant()).query(UUID.class).optional().orElseThrow(()->missing("Account"));
        if(targetAuth.equals(actingAuthUser)&&normalized.equals("DISABLED"))throw conflict("You cannot disable your own account");
        if(normalized.equals("DISABLED")&&db.sql("SELECT COUNT(DISTINCT tenant_id) FROM user_tenant_role WHERE user_id=:user AND revoked_at IS NULL")
                .param("user",userId).query(Integer.class).single()>1)throw conflict("This user belongs to multiple companies and must be disabled by a group or system administrator");
        db.sql("UPDATE app_user SET status=:status,updated_at=CURRENT_TIMESTAMP WHERE id=:id").param("status",normalized).param("id",userId).update();
        return accounts().stream().filter(account->account.id().equals(userId)).findFirst().orElseThrow();
    }

    private static final class Builder{
        final UUID id,authUserId;final String email,displayName,status;final Set<String> roles=new LinkedHashSet<>();
        UUID employmentId;String employeeNumber,employeeName;
        Builder(UUID id,UUID authUserId,String email,String displayName,String status){this.id=id;this.authUserId=authUserId;this.email=email;this.displayName=displayName;this.status=status;}
        AccountSummary build(){return new AccountSummary(id,authUserId,email,displayName,status,roles,employmentId,employeeNumber,employeeName);}
    }
    private static UUID tenant(){return TenantContext.requireTenantId();}
    private static ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
    private static ResponseStatusException conflict(String message){return new ResponseStatusException(HttpStatus.CONFLICT,message);}
    private static ResponseStatusException missing(String value){return new ResponseStatusException(HttpStatus.NOT_FOUND,value+" not found");}
}
