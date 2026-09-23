package com.multitenanterp.platform.security;

import com.multitenanterp.platform.tenant.TenantContext;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AccountAdministrationServiceTest {
    private final UUID tenantA=UUID.randomUUID(),tenantB=UUID.randomUUID(),employeeA=UUID.randomUUID();
    private JdbcClient db;
    private SupabaseAuthAdminClient auth;
    private AccountAdministrationService service;

    @BeforeEach void setUp(){
        JdbcDataSource ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        db=JdbcClient.create(ds);auth=mock(SupabaseAuthAdminClient.class);service=new AccountAdministrationService(db,auth);
        db.sql("CREATE TABLE tenant(id UUID PRIMARY KEY)").update();
        db.sql("CREATE TABLE person(id UUID PRIMARY KEY,tenant_id UUID NOT NULL,first_name VARCHAR(100),last_name VARCHAR(100),UNIQUE(id,tenant_id))").update();
        db.sql("CREATE TABLE employment(id UUID PRIMARY KEY,tenant_id UUID NOT NULL,person_id UUID NOT NULL,employee_number VARCHAR(40),UNIQUE(id,tenant_id))").update();
        db.sql("CREATE TABLE app_user(id UUID PRIMARY KEY,auth_user_id UUID NOT NULL UNIQUE,email VARCHAR(320) NOT NULL UNIQUE,display_name VARCHAR(160),status VARCHAR(20),updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)").update();
        db.sql("CREATE TABLE user_tenant_role(id UUID PRIMARY KEY,tenant_id UUID NOT NULL,user_id UUID NOT NULL,role_code VARCHAR(80),revoked_at TIMESTAMP,UNIQUE(tenant_id,user_id,role_code))").update();
        db.sql("CREATE TABLE employee_user_link(id UUID PRIMARY KEY,tenant_id UUID NOT NULL,user_id UUID NOT NULL,employment_id UUID NOT NULL,linked_by VARCHAR(320),linked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,UNIQUE(tenant_id,user_id),UNIQUE(tenant_id,employment_id))").update();
        UUID person=UUID.randomUUID();
        db.sql("INSERT INTO tenant(id) VALUES(:a),(:b)").param("a",tenantA).param("b",tenantB).update();
        db.sql("INSERT INTO person(id,tenant_id,first_name,last_name) VALUES(:id,:tenant,'Ada','Employee')").param("id",person).param("tenant",tenantA).update();
        db.sql("INSERT INTO employment(id,tenant_id,person_id,employee_number) VALUES(:id,:tenant,:person,'EMP-1')").param("id",employeeA).param("tenant",tenantA).param("person",person).update();
        TenantContext.set(tenantA);
    }

    @AfterEach void clear(){TenantContext.clear();}

    @Test void inviteCreatesMembershipRolesAndEmployeeLink(){
        UUID authId=UUID.randomUUID();when(auth.invite("ada@example.com","Ada Employee")).thenReturn(authId);
        AccountSummary created=service.invite(new CreateAccountRequest("ADA@example.com"," Ada Employee ",Set.of("employee","hr_executive"),employeeA),"admin@example.com");
        assertThat(created.authUserId()).isEqualTo(authId);
        assertThat(created.roles()).containsExactly("EMPLOYEE","HR_EXECUTIVE");
        assertThat(created.employmentId()).isEqualTo(employeeA);
        assertThat(created.employeeNumber()).isEqualTo("EMP-1");
        verify(auth).invite("ada@example.com","Ada Employee");
    }

    @Test void accountsAreTenantIsolatedAndDuplicateIsRejectedBeforeInvite(){
        when(auth.invite(anyString(),anyString())).thenReturn(UUID.randomUUID());
        service.invite(new CreateAccountRequest("ada@example.com","Ada",Set.of("EMPLOYEE"),null),"admin");
        assertThatThrownBy(()->service.invite(new CreateAccountRequest("ADA@example.com","Other",Set.of("EMPLOYEE"),null),"admin"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("already exists");
        verify(auth,times(1)).invite(anyString(),anyString());
        TenantContext.set(tenantB);
        assertThat(service.accounts()).isEmpty();
    }

    @Test void rejectsElevatedOrUnknownRoles(){
        assertThatThrownBy(()->service.invite(new CreateAccountRequest("root@example.com","Root",Set.of("SYSTEM_ADMIN"),null),"admin"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("cannot be assigned");
        verifyNoInteractions(auth);
    }
}
