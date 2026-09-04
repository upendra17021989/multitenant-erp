package com.multitenanterp.employee;

import com.multitenanterp.platform.tenant.TenantContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class EmployeeService {
    private static final String SELECT = """
            SELECT e.id,e.employee_number,e.employment_status,e.employment_type,
              p.first_name,p.middle_name,p.last_name,p.date_of_birth,p.gender,p.personal_email,p.mobile_number,
              p.current_address,p.permanent_address,p.emergency_contact_name,p.emergency_contact_phone,
              e.joining_date,e.confirmation_date,e.probation_end_date,e.exit_date,e.exit_reason,e.work_email,
              e.branch_id,e.department_id,e.designation_id,e.grade_id,e.cost_centre_id,e.reporting_manager_employment_id,
              e.payment_mode,e.bank_account_name,e.bank_account_number,e.bank_name,e.bank_branch,e.bank_ifsc,
              e.pan,e.aadhaar_last_four,e.uan,e.pf_number,e.esi_number
            FROM employment e JOIN person p ON p.id=e.person_id AND p.tenant_id=e.tenant_id
            """;
    private final NamedParameterJdbcTemplate db;

    public EmployeeService(DataSource dataSource) { this.db = new NamedParameterJdbcTemplate(dataSource); }

    public List<Employee> employees(String status) {
        var params = new MapSqlParameterSource("tenant", tenant());
        String where = " WHERE e.tenant_id=:tenant";
        if (status != null && !status.isBlank()) {
            params.addValue("status", normalized(status));
            where += " AND e.employment_status=:status";
        }
        return db.query(SELECT + where + " ORDER BY p.last_name,p.first_name,e.employee_number", params, EmployeeService::map);
    }

    public Employee employee(UUID id) {
        List<Employee> found = db.query(SELECT + " WHERE e.id=:id AND e.tenant_id=:tenant",
                new MapSqlParameterSource("id", id).addValue("tenant", tenant()), EmployeeService::map);
        if (found.isEmpty()) throw missing();
        return found.getFirst();
    }

    @Transactional
    public Employee create(SaveEmployeeRequest request) {
        validateDates(request);
        UUID tenant = tenant(), personId = UUID.randomUUID(), employmentId = UUID.randomUUID();
        var p = parameters(request).addValue("tenant", tenant).addValue("personId", personId).addValue("id", employmentId);
        try {
            db.update("""
                    INSERT INTO person(id,tenant_id,first_name,middle_name,last_name,date_of_birth,gender,personal_email,
                      mobile_number,current_address,permanent_address,emergency_contact_name,emergency_contact_phone)
                    VALUES(:personId,:tenant,:firstName,:middleName,:lastName,:dateOfBirth,:gender,:personalEmail,
                      :mobileNumber,:currentAddress,:permanentAddress,:emergencyName,:emergencyPhone)
                    """, p);
            db.update("""
                    INSERT INTO employment(id,tenant_id,person_id,employee_number,employment_status,employment_type,
                      joining_date,confirmation_date,probation_end_date,exit_date,exit_reason,work_email,branch_id,
                      department_id,designation_id,grade_id,cost_centre_id,reporting_manager_employment_id,payment_mode,
                      bank_account_name,bank_account_number,bank_name,bank_branch,bank_ifsc,pan,aadhaar_last_four,uan,pf_number,esi_number)
                    VALUES(:id,:tenant,:personId,:employeeNumber,:employmentStatus,:employmentType,:joiningDate,
                      :confirmationDate,:probationEndDate,:exitDate,:exitReason,:workEmail,:branchId,:departmentId,
                      :designationId,:gradeId,:costCentreId,:managerId,:paymentMode,:bankAccountName,:bankAccountNumber,
                      :bankName,:bankBranch,:bankIfsc,:pan,:aadhaarLastFour,:uan,:pfNumber,:esiNumber)
                    """, p);
        } catch (DataIntegrityViolationException exception) {
            throw conflict("Employee number or organisation assignment is invalid for this company");
        }
        return employee(employmentId);
    }

    @Transactional
    public Employee update(UUID id, SaveEmployeeRequest request) {
        validateDates(request);
        var p = parameters(request).addValue("tenant", tenant()).addValue("id", id);
        try {
            int personUpdated = db.update("""
                    UPDATE person SET first_name=:firstName,middle_name=:middleName,last_name=:lastName,
                      date_of_birth=:dateOfBirth,gender=:gender,personal_email=:personalEmail,mobile_number=:mobileNumber,
                      current_address=:currentAddress,permanent_address=:permanentAddress,
                      emergency_contact_name=:emergencyName,emergency_contact_phone=:emergencyPhone,updated_at=CURRENT_TIMESTAMP
                    WHERE tenant_id=:tenant AND id=(SELECT person_id FROM employment WHERE id=:id AND tenant_id=:tenant)
                    """, p);
            if (personUpdated == 0) throw missing();
            int updated = db.update("""
                    UPDATE employment SET employee_number=:employeeNumber,employment_status=:employmentStatus,
                      employment_type=:employmentType,joining_date=:joiningDate,confirmation_date=:confirmationDate,
                      probation_end_date=:probationEndDate,exit_date=:exitDate,exit_reason=:exitReason,work_email=:workEmail,
                      branch_id=:branchId,department_id=:departmentId,designation_id=:designationId,grade_id=:gradeId,
                      cost_centre_id=:costCentreId,reporting_manager_employment_id=:managerId,payment_mode=:paymentMode,
                      bank_account_name=:bankAccountName,bank_account_number=:bankAccountNumber,bank_name=:bankName,
                      bank_branch=:bankBranch,bank_ifsc=:bankIfsc,pan=:pan,aadhaar_last_four=:aadhaarLastFour,
                      uan=:uan,pf_number=:pfNumber,esi_number=:esiNumber,updated_at=CURRENT_TIMESTAMP
                    WHERE id=:id AND tenant_id=:tenant
                    """, p);
            if (updated == 0) throw missing();
        } catch (DataIntegrityViolationException exception) {
            throw conflict("Employee number or organisation assignment is invalid for this company");
        }
        return employee(id);
    }

    private static MapSqlParameterSource parameters(SaveEmployeeRequest r) {
        return new MapSqlParameterSource()
                .addValue("employeeNumber", normalized(r.employeeNumber())).addValue("employmentStatus", normalized(r.employmentStatus()))
                .addValue("employmentType", normalized(r.employmentType())).addValue("firstName", clean(r.firstName()))
                .addValue("middleName", clean(r.middleName())).addValue("lastName", clean(r.lastName()))
                .addValue("dateOfBirth", r.dateOfBirth()).addValue("gender", clean(r.gender()))
                .addValue("personalEmail", lower(r.personalEmail())).addValue("mobileNumber", clean(r.mobileNumber()))
                .addValue("currentAddress", clean(r.currentAddress())).addValue("permanentAddress", clean(r.permanentAddress()))
                .addValue("emergencyName", clean(r.emergencyContactName())).addValue("emergencyPhone", clean(r.emergencyContactPhone()))
                .addValue("joiningDate", r.joiningDate()).addValue("confirmationDate", r.confirmationDate())
                .addValue("probationEndDate", r.probationEndDate()).addValue("exitDate", r.exitDate()).addValue("exitReason", clean(r.exitReason()))
                .addValue("workEmail", lower(r.workEmail())).addValue("branchId", r.branchId()).addValue("departmentId", r.departmentId())
                .addValue("designationId", r.designationId()).addValue("gradeId", r.gradeId()).addValue("costCentreId", r.costCentreId())
                .addValue("managerId", r.reportingManagerEmploymentId()).addValue("paymentMode", normalized(r.paymentMode()))
                .addValue("bankAccountName", clean(r.bankAccountName())).addValue("bankAccountNumber", clean(r.bankAccountNumber()))
                .addValue("bankName", clean(r.bankName())).addValue("bankBranch", clean(r.bankBranch())).addValue("bankIfsc", normalized(r.bankIfsc()))
                .addValue("pan", normalized(r.pan())).addValue("aadhaarLastFour", clean(r.aadhaarLastFour())).addValue("uan", clean(r.uan()))
                .addValue("pfNumber", clean(r.pfNumber())).addValue("esiNumber", clean(r.esiNumber()));
    }

    private static Employee map(ResultSet r, int row) throws SQLException {
        return new Employee(r.getObject("id",UUID.class),r.getString("employee_number"),r.getString("employment_status"),r.getString("employment_type"),
                r.getString("first_name"),r.getString("middle_name"),r.getString("last_name"),date(r,"date_of_birth"),r.getString("gender"),
                r.getString("personal_email"),r.getString("mobile_number"),r.getString("current_address"),r.getString("permanent_address"),
                r.getString("emergency_contact_name"),r.getString("emergency_contact_phone"),date(r,"joining_date"),date(r,"confirmation_date"),
                date(r,"probation_end_date"),date(r,"exit_date"),r.getString("exit_reason"),r.getString("work_email"),r.getObject("branch_id",UUID.class),
                r.getObject("department_id",UUID.class),r.getObject("designation_id",UUID.class),r.getObject("grade_id",UUID.class),
                r.getObject("cost_centre_id",UUID.class),r.getObject("reporting_manager_employment_id",UUID.class),r.getString("payment_mode"),
                r.getString("bank_account_name"),r.getString("bank_account_number"),r.getString("bank_name"),r.getString("bank_branch"),
                r.getString("bank_ifsc"),r.getString("pan"),r.getString("aadhaar_last_four"),r.getString("uan"),r.getString("pf_number"),r.getString("esi_number"));
    }

    private static LocalDate date(ResultSet r, String column) throws SQLException { var value=r.getDate(column); return value==null?null:value.toLocalDate(); }
    private static void validateDates(SaveEmployeeRequest r) {
        if (r.confirmationDate()!=null && r.confirmationDate().isBefore(r.joiningDate())) throw badRequest("Confirmation date cannot precede joining date");
        if (r.probationEndDate()!=null && r.probationEndDate().isBefore(r.joiningDate())) throw badRequest("Probation end date cannot precede joining date");
        if (r.exitDate()!=null && r.exitDate().isBefore(r.joiningDate())) throw badRequest("Exit date cannot precede joining date");
        if (r.dateOfBirth()!=null && !r.dateOfBirth().isBefore(r.joiningDate())) throw badRequest("Date of birth must precede joining date");
    }
    private static UUID tenant() { return TenantContext.requireTenantId(); }
    private static String clean(String value) { return value==null||value.isBlank()?null:value.trim(); }
    private static String normalized(String value) { var clean=clean(value); return clean==null?null:clean.toUpperCase(Locale.ROOT); }
    private static String lower(String value) { var clean=clean(value); return clean==null?null:clean.toLowerCase(Locale.ROOT); }
    private static ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND,"Employee not found"); }
    private static ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT,message); }
    private static ResponseStatusException badRequest(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,message); }
}
