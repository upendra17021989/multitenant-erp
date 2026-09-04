package com.multitenanterp.employee;

import com.multitenanterp.platform.tenant.TenantContext;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmployeeTenantIsolationIntegrationTest {
    private final UUID tenantA = UUID.randomUUID();
    private final UUID tenantB = UUID.randomUUID();
    private EmployeeService service;
    private JdbcClient db;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        db = JdbcClient.create(dataSource);
        createSchema();
        db.sql("INSERT INTO tenant(id) VALUES(?),(?)").params(tenantA, tenantB).update();
        service = new EmployeeService(dataSource);
    }

    @AfterEach void clearTenant() { TenantContext.clear(); }

    @Test
    void employeesCannotBeReadOrUpdatedThroughAnotherTenant() {
        TenantContext.set(tenantA);
        Employee alpha = service.create(request("EMP001", "Alpha", null));
        TenantContext.set(tenantB);
        Employee beta = service.create(request("EMP001", "Beta", null));

        assertThat(service.employees(null)).extracting(Employee::id).containsExactly(beta.id());
        assertThatThrownBy(() -> service.employee(alpha.id())).isInstanceOfSatisfying(ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> service.update(alpha.id(), request("EMP999", "Changed", null)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        TenantContext.set(tenantA);
        assertThat(service.employee(alpha.id()).firstName()).isEqualTo("Alpha");
        assertThat(service.employees("ACTIVE")).extracting(Employee::id).containsExactly(alpha.id());
    }

    @Test
    void employeeNumberIsTenantLocalAndOrganizationAssignmentsCannotCrossTenants() {
        UUID betaBranch = UUID.randomUUID();
        db.sql("INSERT INTO branch(id,tenant_id) VALUES(?,?)").params(betaBranch, tenantB).update();
        TenantContext.set(tenantA);
        service.create(request("SAME", "First", null));
        assertThatThrownBy(() -> service.create(request("SAME", "Duplicate", null)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThatThrownBy(() -> service.create(request("OTHER", "Cross tenant", betaBranch)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        TenantContext.set(tenantB);
        assertThat(service.create(request("SAME", "Allowed", betaBranch)).employeeNumber()).isEqualTo("SAME");
    }

    @Test
    void rejectsInvalidEmploymentDates() {
        TenantContext.set(tenantA);
        SaveEmployeeRequest invalid = request("EMP002", "Date", null,
                LocalDate.of(2026,1,1), LocalDate.of(2025,12,31));
        assertThatThrownBy(() -> service.create(invalid)).isInstanceOfSatisfying(ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private SaveEmployeeRequest request(String number, String firstName, UUID branchId) {
        return request(number, firstName, branchId, LocalDate.of(2026,1,1), null);
    }

    private SaveEmployeeRequest request(String number, String firstName, UUID branchId,
                                        LocalDate joiningDate, LocalDate confirmationDate) {
        return new SaveEmployeeRequest(number,"ACTIVE","PERMANENT",firstName,null,"Employee",
                LocalDate.of(1990,1,1),null,firstName.toLowerCase()+"@example.com","9999999999",
                null,null,null,null,joiningDate,confirmationDate,null,null,null,
                firstName.toLowerCase()+"@work.example.com",branchId,null,null,null,null,null,
                "BANK_TRANSFER",firstName+" Employee","123456789","Example Bank",null,"ABCD0123456",
                "ABCDE1234F","1234","100000000001",null,null);
    }

    private void createSchema() {
        db.sql("CREATE TABLE tenant(id UUID PRIMARY KEY)").update();
        db.sql("CREATE TABLE branch(id UUID PRIMARY KEY,tenant_id UUID NOT NULL,UNIQUE(id,tenant_id))").update();
        db.sql("CREATE TABLE department(id UUID PRIMARY KEY,tenant_id UUID NOT NULL,UNIQUE(id,tenant_id))").update();
        db.sql("CREATE TABLE designation(id UUID PRIMARY KEY,tenant_id UUID NOT NULL,UNIQUE(id,tenant_id))").update();
        db.sql("CREATE TABLE grade(id UUID PRIMARY KEY,tenant_id UUID NOT NULL,UNIQUE(id,tenant_id))").update();
        db.sql("CREATE TABLE cost_centre(id UUID PRIMARY KEY,tenant_id UUID NOT NULL,UNIQUE(id,tenant_id))").update();
        db.sql("CREATE TABLE person(id UUID PRIMARY KEY,tenant_id UUID NOT NULL,first_name VARCHAR(100),middle_name VARCHAR(100),last_name VARCHAR(100),date_of_birth DATE,gender VARCHAR(30),personal_email VARCHAR(320),mobile_number VARCHAR(30),current_address VARCHAR(4000),permanent_address VARCHAR(4000),emergency_contact_name VARCHAR(160),emergency_contact_phone VARCHAR(30),updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,UNIQUE(id,tenant_id))").update();
        db.sql("""
                CREATE TABLE employment(id UUID PRIMARY KEY,tenant_id UUID NOT NULL,person_id UUID NOT NULL,
                employee_number VARCHAR(40) NOT NULL,employment_status VARCHAR(20),employment_type VARCHAR(30),joining_date DATE,
                confirmation_date DATE,probation_end_date DATE,exit_date DATE,exit_reason VARCHAR(500),work_email VARCHAR(320),
                branch_id UUID,department_id UUID,designation_id UUID,grade_id UUID,cost_centre_id UUID,reporting_manager_employment_id UUID,
                payment_mode VARCHAR(20),bank_account_name VARCHAR(160),bank_account_number VARCHAR(50),bank_name VARCHAR(160),
                bank_branch VARCHAR(160),bank_ifsc VARCHAR(11),pan VARCHAR(10),aadhaar_last_four VARCHAR(4),uan VARCHAR(12),
                pf_number VARCHAR(40),esi_number VARCHAR(30),updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(tenant_id,employee_number),UNIQUE(id,tenant_id),
                FOREIGN KEY(person_id,tenant_id) REFERENCES person(id,tenant_id),
                FOREIGN KEY(branch_id,tenant_id) REFERENCES branch(id,tenant_id))
                """).update();
    }
}
