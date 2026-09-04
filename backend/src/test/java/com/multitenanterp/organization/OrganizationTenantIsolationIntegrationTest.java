package com.multitenanterp.organization;

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

class OrganizationTenantIsolationIntegrationTest {
    private final UUID tenantA = UUID.randomUUID();
    private final UUID tenantB = UUID.randomUUID();
    private JdbcClient db;
    private OrganizationService organizations;
    private OrganizationMasterService masters;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        db = JdbcClient.create(dataSource);
        createSchema();
        db.sql("INSERT INTO tenant(id,code,legal_name,status) VALUES(?,?,?,?)")
                .params(tenantA, "ALPHA", "Alpha Ltd", "ACTIVE").update();
        db.sql("INSERT INTO tenant(id,code,legal_name,status) VALUES(?,?,?,?)")
                .params(tenantB, "BETA", "Beta Ltd", "ACTIVE").update();
        organizations = new OrganizationService(db);
        masters = new OrganizationMasterService(db);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void listOperationsReturnOnlyTheActiveTenantsRecords() {
        Seed alpha = seedTenant(tenantA, "A", LocalDate.of(2026, 1, 26));
        Seed beta = seedTenant(tenantB, "B", LocalDate.of(2026, 8, 15));

        TenantContext.set(tenantA);
        assertThat(organizations.companyProfile().registeredAddress()).isEqualTo("A address");
        assertThat(organizations.branches()).extracting(Branch::id).containsExactly(alpha.branch());
        assertThat(masters.departments()).extracting(Department::id).containsExactly(alpha.department());
        assertThat(masters.grades()).extracting(Grade::id).containsExactly(alpha.grade());
        assertThat(masters.designations()).extracting(Designation::id).containsExactly(alpha.designation());
        assertThat(masters.costCentres()).extracting(CostCentre::id).containsExactly(alpha.costCentre());
        assertThat(masters.holidays(2026)).extracting(Holiday::id).containsExactly(alpha.holiday());

        TenantContext.set(tenantB);
        assertThat(organizations.companyProfile().registeredAddress()).isEqualTo("B address");
        assertThat(organizations.branches()).extracting(Branch::id).containsExactly(beta.branch());
        assertThat(masters.departments()).extracting(Department::id).containsExactly(beta.department());
        assertThat(masters.grades()).extracting(Grade::id).containsExactly(beta.grade());
        assertThat(masters.designations()).extracting(Designation::id).containsExactly(beta.designation());
        assertThat(masters.costCentres()).extracting(CostCentre::id).containsExactly(beta.costCentre());
        assertThat(masters.holidays(2026)).extracting(Holiday::id).containsExactly(beta.holiday());
    }

    @Test
    void updatesCannotModifyAnotherTenantsRecords() {
        Seed alpha = seedTenant(tenantA, "A", LocalDate.of(2026, 1, 26));
        seedTenant(tenantB, "B", LocalDate.of(2026, 8, 15));
        TenantContext.set(tenantB);

        assertNotFound(() -> organizations.updateBranch(alpha.branch(),
                new SaveBranchRequest("X", "Changed", null, null, false, false, "ACTIVE")));
        assertNotFound(() -> masters.saveDepartment(alpha.department(),
                new SaveDepartmentRequest("X", "Changed", null, "ACTIVE")));
        assertNotFound(() -> masters.saveGrade(alpha.grade(), new SaveGradeRequest("X", "Changed", "ACTIVE")));
        assertNotFound(() -> masters.saveDesignation(alpha.designation(),
                new SaveDesignationRequest("X", "Changed", null, "ACTIVE")));
        assertNotFound(() -> masters.saveCostCentre(alpha.costCentre(),
                new SaveCostCentreRequest("X", "Changed", null, "ACTIVE")));
        assertNotFound(() -> masters.saveHoliday(alpha.holiday(),
                new SaveHolidayRequest(null, LocalDate.of(2026, 2, 1), "Changed", false)));

        assertThat(db.sql("SELECT COUNT(*) FROM branch WHERE id=? AND name='Changed'").param(alpha.branch()).query(Integer.class).single()).isZero();
        assertThat(db.sql("SELECT COUNT(*) FROM department WHERE id=? AND name='Changed'").param(alpha.department()).query(Integer.class).single()).isZero();
        assertThat(db.sql("SELECT COUNT(*) FROM grade WHERE id=? AND name='Changed'").param(alpha.grade()).query(Integer.class).single()).isZero();
        assertThat(db.sql("SELECT COUNT(*) FROM designation WHERE id=? AND title='Changed'").param(alpha.designation()).query(Integer.class).single()).isZero();
        assertThat(db.sql("SELECT COUNT(*) FROM cost_centre WHERE id=? AND name='Changed'").param(alpha.costCentre()).query(Integer.class).single()).isZero();
        assertThat(db.sql("SELECT COUNT(*) FROM holiday WHERE id=? AND name='Changed'").param(alpha.holiday()).query(Integer.class).single()).isZero();
    }

    @Test
    void masterCodesAreUniqueWithinTenantButReusableByAnotherTenant() {
        TenantContext.set(tenantA);
        organizations.createBranch(new SaveBranchRequest("HQ", "Alpha HQ", null, null, false, false, "ACTIVE"));
        masters.saveDepartment(null, new SaveDepartmentRequest("HR", "Alpha HR", null, "ACTIVE"));

        TenantContext.set(tenantB);
        assertThat(organizations.createBranch(new SaveBranchRequest("HQ", "Beta HQ", null, null, false, false, "ACTIVE")).code()).isEqualTo("HQ");
        assertThat(masters.saveDepartment(null, new SaveDepartmentRequest("HR", "Beta HR", null, "ACTIVE")).code()).isEqualTo("HR");
    }

    private Seed seedTenant(UUID tenant, String prefix, LocalDate holidayDate) {
        UUID branch = UUID.randomUUID(), department = UUID.randomUUID(), grade = UUID.randomUUID();
        UUID designation = UUID.randomUUID(), costCentre = UUID.randomUUID(), holiday = UUID.randomUUID();
        db.sql("INSERT INTO company_profile(tenant_id,registered_address) VALUES(?,?)").params(tenant, prefix + " address").update();
        db.sql("INSERT INTO branch(id,tenant_id,code,name,status) VALUES(?,?,?,?, 'ACTIVE')").params(branch, tenant, prefix + "HQ", prefix + " branch").update();
        db.sql("INSERT INTO department(id,tenant_id,code,name,status) VALUES(?,?,?,?, 'ACTIVE')").params(department, tenant, prefix + "HR", prefix + " department").update();
        db.sql("INSERT INTO grade(id,tenant_id,code,name,status) VALUES(?,?,?,?, 'ACTIVE')").params(grade, tenant, prefix + "G1", prefix + " grade").update();
        db.sql("INSERT INTO designation(id,tenant_id,code,title,grade_id,status) VALUES(?,?,?,?,?, 'ACTIVE')").params(designation, tenant, prefix + "DEV", prefix + " designation", grade).update();
        db.sql("INSERT INTO cost_centre(id,tenant_id,code,name,status) VALUES(?,?,?,?, 'ACTIVE')").params(costCentre, tenant, prefix + "CC", prefix + " cost centre").update();
        db.sql("INSERT INTO holiday(id,tenant_id,branch_id,holiday_date,name,optional) VALUES(?,?,?,?,?,FALSE)").params(holiday, tenant, branch, holidayDate, prefix + " holiday").update();
        return new Seed(branch, department, grade, designation, costCentre, holiday);
    }

    private void assertNotFound(Runnable operation) {
        assertThatThrownBy(operation::run).isInstanceOfSatisfying(ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private void createSchema() {
        db.sql("CREATE TABLE tenant(id UUID PRIMARY KEY, code VARCHAR(40) UNIQUE NOT NULL, legal_name VARCHAR(200) NOT NULL, status VARCHAR(20) NOT NULL)").update();
        db.sql("CREATE TABLE company_profile(tenant_id UUID PRIMARY KEY REFERENCES tenant(id), registered_address VARCHAR(4000), pan VARCHAR(10), tan VARCHAR(10), gstin VARCHAR(15), pf_registration VARCHAR(80), esi_registration VARCHAR(80), logo_path VARCHAR(500), updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)").update();
        db.sql("CREATE TABLE branch(id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenant(id), code VARCHAR(40) NOT NULL, name VARCHAR(160) NOT NULL, address VARCHAR(4000), state_code VARCHAR(2), professional_tax_applicable BOOLEAN DEFAULT FALSE NOT NULL, labour_welfare_fund_applicable BOOLEAN DEFAULT FALSE NOT NULL, status VARCHAR(20) NOT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, UNIQUE(tenant_id,code), UNIQUE(id,tenant_id))").update();
        db.sql("CREATE TABLE department(id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenant(id), code VARCHAR(40) NOT NULL, name VARCHAR(160) NOT NULL, parent_department_id UUID, status VARCHAR(20) NOT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, UNIQUE(tenant_id,code))").update();
        db.sql("CREATE TABLE grade(id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenant(id), code VARCHAR(40) NOT NULL, name VARCHAR(120) NOT NULL, status VARCHAR(20) NOT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, UNIQUE(tenant_id,code))").update();
        db.sql("CREATE TABLE designation(id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenant(id), code VARCHAR(40) NOT NULL, title VARCHAR(160) NOT NULL, grade_id UUID, status VARCHAR(20) NOT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, UNIQUE(tenant_id,code))").update();
        db.sql("CREATE TABLE cost_centre(id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenant(id), code VARCHAR(40) NOT NULL, name VARCHAR(160) NOT NULL, accounting_reference VARCHAR(100), status VARCHAR(20) NOT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, UNIQUE(tenant_id,code))").update();
        db.sql("CREATE TABLE holiday(id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenant(id), branch_id UUID, holiday_date DATE NOT NULL, name VARCHAR(160) NOT NULL, optional BOOLEAN NOT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, UNIQUE(tenant_id,branch_id,holiday_date))").update();
    }

    private record Seed(UUID branch, UUID department, UUID grade, UUID designation, UUID costCentre, UUID holiday) {}
}
