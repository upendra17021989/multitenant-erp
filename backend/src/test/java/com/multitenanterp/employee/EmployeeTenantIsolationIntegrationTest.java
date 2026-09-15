package com.multitenanterp.employee;

import com.multitenanterp.platform.tenant.TenantContext;
import org.h2.jdbcx.JdbcDataSource;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.util.UUID;
import java.util.Set;
import java.nio.file.Path;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmployeeTenantIsolationIntegrationTest {
    private final UUID tenantA = UUID.randomUUID();
    private final UUID tenantB = UUID.randomUUID();
    private EmployeeService service;
    private EmployeeDocumentService documents;
    private EmployeeExcelImportService importer;
    private EmployeeUserLinkService userLinks;
    private JdbcClient db;
    @TempDir Path storageRoot;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        db = JdbcClient.create(dataSource);
        createSchema();
        db.sql("INSERT INTO tenant(id) VALUES(?),(?)").params(tenantA, tenantB).update();
        service = new EmployeeService(dataSource);
        importer = new EmployeeExcelImportService(db, service);
        userLinks = new EmployeeUserLinkService(db);
        documents = new EmployeeDocumentService(db, new FileSystemDocumentStorage(storageRoot.toString()));
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
        assertThat(service.employee(alpha.id()))
                .extracting(Employee::title, Employee::maritalStatus, Employee::fatherGuardianName,
                        Employee::ticketNumber, Employee::retirementDate, Employee::pfJoiningDate,
                        Employee::pran, Employee::groupJoiningDate, Employee::ccEmail,
                        Employee::division, Employee::unit, Employee::category, Employee::project)
                .containsExactly("Mr.", "Married", "Guardian", "T-1", LocalDate.of(2050,1,1),
                        LocalDate.of(2026,1,1), "100000000002", LocalDate.of(2025,1,1),
                        "cc@example.com", "Operations", "Unit 1", "Staff", "Project A");
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

    @Test
    void importsValidExcelRowsAndReportsInvalidRows() throws Exception {
        byte[] content;
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Employees");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("Employee Code");
            header.createCell(1).setCellValue("First Name");
            header.createCell(2).setCellValue("Join Date");
            header.createCell(3).setCellValue("Status");
            header.createCell(4).setCellValue("Title");
            var valid = sheet.createRow(1);
            valid.createCell(0).setCellValue("EMP100"); valid.createCell(1).setCellValue("Jane Doe");
            valid.createCell(2).setCellValue("2026-01-15"); valid.createCell(3).setCellValue("Active"); valid.createCell(4).setCellValue("Ms.");
            var invalid = sheet.createRow(2);
            invalid.createCell(0).setCellValue("EMP101"); invalid.createCell(1).setCellValue("SingleName");
            workbook.write(output); content=output.toByteArray();
        }

        TenantContext.set(tenantA);
        EmployeeImportResult result=importer.importExcel(new MockMultipartFile("file","employees.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",content),"Employees","PERMANENT");

        assertThat(result.acceptedRows()).as(result.rows().toString()).isEqualTo(1);
        assertThat(result.rejectedRows()).isEqualTo(1);
        assertThat(service.employees(null)).singleElement().satisfies(employee -> {
            assertThat(employee.employeeNumber()).isEqualTo("EMP100");
            assertThat(employee.firstName()).isEqualTo("Jane");
            assertThat(employee.lastName()).isEqualTo("Doe");
            assertThat(employee.title()).isEqualTo("Ms.");
        });
    }

    @Test
    void userEmploymentLinksAreTenantScopedAndResolveTheCurrentEmployee() {
        UUID appUser=UUID.randomUUID(),authUser=UUID.randomUUID();
        db.sql("INSERT INTO app_user(id,auth_user_id,email,status) VALUES(?,?,?,?)").params(appUser,authUser,"employee@example.com","ACTIVE").update();
        db.sql("INSERT INTO user_tenant_role(id,tenant_id,user_id,role_code) VALUES(?,?,?,?)").params(UUID.randomUUID(),tenantA,appUser,"EMPLOYEE").update();
        TenantContext.set(tenantA);
        Employee employee=service.create(request("SELF001","Self",null));

        EmployeeUserLink link=userLinks.link(employee.id(),"EMPLOYEE@EXAMPLE.COM","admin@example.com");
        assertThat(link.employmentId()).isEqualTo(employee.id());
        assertThat(link.userEmail()).isEqualTo("employee@example.com");

        TenantContext.set(authUser,tenantA,Set.of("EMPLOYEE"));
        assertThat(userLinks.currentEmploymentId()).isEqualTo(employee.id());
        assertThat(userLinks.isCurrentEmployee(employee.id())).isTrue();

        TenantContext.set(authUser,tenantB,Set.of("EMPLOYEE"));
        assertThat(userLinks.links()).isEmpty();
        assertThat(userLinks.isCurrentEmployee(employee.id())).isFalse();
    }

    @Test
    void employeeDocumentsUseTenantScopedMetadataAndStorageKeys() throws Exception {
        TenantContext.set(tenantA);
        Employee employee = service.create(request("DOC001", "Document", null));
        EmployeeDocument uploaded = documents.upload(employee.id(), "IDENTITY_PROOF",
                new MockMultipartFile("file", "../identity.pdf", "application/pdf", "pdf-content".getBytes()));
        assertThat(uploaded.fileName()).isEqualTo("identity.pdf");
        assertThat(documents.documents(employee.id())).extracting(EmployeeDocument::id).containsExactly(uploaded.id());
        assertThat(documents.download(employee.id(), uploaded.id()).resource().getContentAsByteArray()).isEqualTo("pdf-content".getBytes());
        assertThat(storageRoot.resolve(tenantA.toString()).resolve(employee.id().toString()).resolve(uploaded.id().toString())).exists();

        TenantContext.set(tenantB);
        assertThatThrownBy(() -> documents.documents(employee.id())).isInstanceOfSatisfying(ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> documents.download(employee.id(), uploaded.id())).isInstanceOfSatisfying(ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
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
                "ABCDE1234F","1234","100000000001",null,null,
                "Mr.","Married","Guardian","T-1",LocalDate.of(2050,1,1),LocalDate.of(2026,1,1),
                "100000000002",LocalDate.of(2025,1,1),"CC@EXAMPLE.COM","Operations","Unit 1","Staff","Project A");
    }

    private void createSchema() {
        db.sql("CREATE TABLE tenant(id UUID PRIMARY KEY)").update();
        db.sql("CREATE TABLE app_user(id UUID PRIMARY KEY,auth_user_id UUID UNIQUE,email VARCHAR(320),status VARCHAR(20))").update();
        db.sql("CREATE TABLE user_tenant_role(id UUID PRIMARY KEY,tenant_id UUID NOT NULL,user_id UUID NOT NULL,role_code VARCHAR(80),revoked_at TIMESTAMP)").update();
        db.sql("CREATE TABLE branch(id UUID PRIMARY KEY,tenant_id UUID NOT NULL,UNIQUE(id,tenant_id))").update();
        db.sql("CREATE TABLE department(id UUID PRIMARY KEY,tenant_id UUID NOT NULL,UNIQUE(id,tenant_id))").update();
        db.sql("CREATE TABLE designation(id UUID PRIMARY KEY,tenant_id UUID NOT NULL,UNIQUE(id,tenant_id))").update();
        db.sql("CREATE TABLE grade(id UUID PRIMARY KEY,tenant_id UUID NOT NULL,UNIQUE(id,tenant_id))").update();
        db.sql("CREATE TABLE cost_centre(id UUID PRIMARY KEY,tenant_id UUID NOT NULL,UNIQUE(id,tenant_id))").update();
        db.sql("CREATE TABLE person(id UUID PRIMARY KEY,tenant_id UUID NOT NULL,first_name VARCHAR(100),middle_name VARCHAR(100),last_name VARCHAR(100),date_of_birth DATE,gender VARCHAR(30),personal_email VARCHAR(320),mobile_number VARCHAR(30),current_address VARCHAR(4000),permanent_address VARCHAR(4000),emergency_contact_name VARCHAR(160),emergency_contact_phone VARCHAR(30),title VARCHAR(30),marital_status VARCHAR(30),father_guardian_name VARCHAR(160),updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,UNIQUE(id,tenant_id))").update();
        db.sql("""
                CREATE TABLE employment(id UUID PRIMARY KEY,tenant_id UUID NOT NULL,person_id UUID NOT NULL,
                employee_number VARCHAR(40) NOT NULL,employment_status VARCHAR(20),employment_type VARCHAR(30),joining_date DATE,
                confirmation_date DATE,probation_end_date DATE,exit_date DATE,exit_reason VARCHAR(500),work_email VARCHAR(320),
                branch_id UUID,department_id UUID,designation_id UUID,grade_id UUID,cost_centre_id UUID,reporting_manager_employment_id UUID,
                payment_mode VARCHAR(20),bank_account_name VARCHAR(160),bank_account_number VARCHAR(50),bank_name VARCHAR(160),
                bank_branch VARCHAR(160),bank_ifsc VARCHAR(11),pan VARCHAR(10),aadhaar_last_four VARCHAR(4),uan VARCHAR(12),
                pf_number VARCHAR(40),esi_number VARCHAR(30),ticket_number VARCHAR(40),retirement_date DATE,
                pf_joining_date DATE,pran VARCHAR(20),group_joining_date DATE,cc_email VARCHAR(320),division VARCHAR(160),
                unit VARCHAR(160),category VARCHAR(160),project VARCHAR(160),updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(tenant_id,employee_number),UNIQUE(id,tenant_id),
                FOREIGN KEY(person_id,tenant_id) REFERENCES person(id,tenant_id),
                FOREIGN KEY(branch_id,tenant_id) REFERENCES branch(id,tenant_id))
                """).update();
        db.sql("CREATE TABLE employee_user_link(id UUID PRIMARY KEY,tenant_id UUID NOT NULL,user_id UUID NOT NULL,employment_id UUID NOT NULL,linked_by VARCHAR(320),linked_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,UNIQUE(tenant_id,user_id),UNIQUE(tenant_id,employment_id),FOREIGN KEY(employment_id,tenant_id) REFERENCES employment(id,tenant_id))").update();
        db.sql("CREATE TABLE employee_document(id UUID PRIMARY KEY,tenant_id UUID NOT NULL,employment_id UUID NOT NULL,document_type VARCHAR(40),file_name VARCHAR(255),content_type VARCHAR(150),size_bytes BIGINT,storage_key VARCHAR(500) UNIQUE,uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,FOREIGN KEY(employment_id,tenant_id) REFERENCES employment(id,tenant_id))").update();
    }
}
