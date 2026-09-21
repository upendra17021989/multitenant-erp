package com.multitenanterp.reporting;

import com.multitenanterp.platform.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.*;

@Service
public class ReportingService {
    private static final Set<String> ADMIN = Set.of("SYSTEM_ADMIN", "GROUP_ADMIN", "COMPANY_ADMIN");
    private static final Set<String> HR = Set.of("HR_MANAGER", "HR_EXECUTIVE");
    private static final Set<String> PAYROLL = Set.of("PAYROLL_MANAGER", "PAYROLL_EXECUTIVE");
    private final JdbcClient db;
    public ReportingService(JdbcClient db) { this.db = db; }
    public record Report(String name, UUID tenantId, String company, LocalDate from, LocalDate to,
                         List<String> columns, List<List<String>> rows) {}
    private record Query(String sql, boolean payroll) {}

    public Report report(String name, LocalDate from, LocalDate to) {
        if(from == null || to == null || to.isBefore(from) || to.isAfter(from.plusYears(1)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a date range of at most one year");
        Query query = query(name);
        requireAccess(query.payroll());
        UUID tenant = TenantContext.requireTenantId();
        String company = db.sql("SELECT legal_name FROM tenant WHERE id=:tenant").param("tenant", tenant).query(String.class).single();
        List<String> columns = new ArrayList<>();
        List<List<String>> rows = db.sql(query.sql()).param("tenant", tenant).param("from", from).param("to", to)
                .query((rs, n) -> {
                    int count = rs.getMetaData().getColumnCount();
                    if(columns.isEmpty()) for(int i=1; i<=count; i++) columns.add(rs.getMetaData().getColumnLabel(i).toLowerCase(Locale.ROOT));
                    List<String> row = new ArrayList<>();
                    for(int i=1; i<=count; i++) {Object value=rs.getObject(i);row.add(value instanceof java.math.BigDecimal decimal ? decimal.toPlainString() : Objects.toString(value, ""));}
                    return row;
                }).list();
        return new Report(name, tenant, company, from, to, columns, rows);
    }

    public Map<String, Object> dashboard(LocalDate date) {
        requireAccess(false);
        UUID tenant = TenantContext.requireTenantId();
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("date", date);
        result.put("employees", count("SELECT COUNT(*) FROM employment WHERE tenant_id=:tenant", tenant));
        result.put("activeEmployees", count("SELECT COUNT(*) FROM employment WHERE tenant_id=:tenant AND employment_status IN ('ACTIVE','PROBATION','NOTICE')", tenant));
        result.put("pendingLeave", count("SELECT COUNT(*) FROM leave_request WHERE tenant_id=:tenant AND status='PENDING'", tenant));
        result.put("attendanceToday", db.sql("SELECT COUNT(*) FROM attendance_record WHERE tenant_id=:tenant AND attendance_date=:date AND status IN ('PRESENT','HALF_DAY','WORK_FROM_HOME','ON_DUTY')").param("tenant",tenant).param("date",date).query(Long.class).single());
        result.put("onLeave", db.sql("SELECT COUNT(DISTINCT employment_id) FROM leave_request WHERE tenant_id=:tenant AND status='APPROVED' AND start_date<=:date AND end_date>=:date").param("tenant",tenant).param("date",date).query(Long.class).single());
        if(canPayroll()) result.put("monthlyPayrollCost", db.sql("SELECT COALESCE(SUM(r.gross_pay+r.employer_contributions),0) FROM payroll_employee_result r JOIN payroll_run pr ON pr.id=r.payroll_run_id AND pr.tenant_id=r.tenant_id WHERE r.tenant_id=:tenant AND pr.payroll_year=:year AND pr.payroll_month=:month AND pr.status IN ('APPROVED','LOCKED','PAID')").param("tenant",tenant).param("year",date.getYear()).param("month",date.getMonthValue()).query(java.math.BigDecimal.class).single());
        return result;
    }
    private Long count(String sql, UUID tenant) { return db.sql(sql).param("tenant",tenant).query(Long.class).single(); }
    private static boolean has(Set<String> roles) { return TenantContext.roles().stream().anyMatch(roles::contains); }
    private static boolean canPayroll() { return has(ADMIN) || has(PAYROLL); }
    private static void requireAccess(boolean payroll) {
        if(!(payroll ? canPayroll() : canPayroll() || has(HR))) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Report access is not permitted for your role");
    }

    private static Query query(String name) {
        String employee = " JOIN employment e ON e.id=r.employment_id AND e.tenant_id=r.tenant_id ";
        String payroll = " FROM payroll_employee_result r JOIN payroll_run pr ON pr.id=r.payroll_run_id AND pr.tenant_id=r.tenant_id" + employee;
        String period = " WHERE r.tenant_id=:tenant AND pr.status IN ('APPROVED','LOCKED','PAID') AND (pr.payroll_year*100+pr.payroll_month) BETWEEN (EXTRACT(YEAR FROM CAST(:from AS DATE))*100+EXTRACT(MONTH FROM CAST(:from AS DATE))) AND (EXTRACT(YEAR FROM CAST(:to AS DATE))*100+EXTRACT(MONTH FROM CAST(:to AS DATE)))";
        return switch(name) {
            case "employees" -> new Query("SELECT e.employee_number,p.first_name,p.last_name,e.employment_status,e.employment_type,e.joining_date,e.confirmation_date,e.exit_date FROM employment e JOIN person p ON p.id=e.person_id AND p.tenant_id=e.tenant_id WHERE e.tenant_id=:tenant AND e.joining_date<=:to AND (e.exit_date IS NULL OR e.exit_date>=:from) ORDER BY e.employee_number", false);
            case "employee-movements" -> new Query("SELECT employee_number,employment_status,joining_date,confirmation_date,exit_date FROM employment WHERE tenant_id=:tenant AND (joining_date BETWEEN :from AND :to OR confirmation_date BETWEEN :from AND :to OR exit_date BETWEEN :from AND :to) ORDER BY employee_number", false);
            case "attendance" -> new Query("SELECT e.employee_number,r.attendance_date,r.status,r.check_in,r.check_out,r.worked_minutes,r.overtime_minutes,r.source FROM attendance_record r"+employee+"WHERE r.tenant_id=:tenant AND r.attendance_date BETWEEN :from AND :to ORDER BY r.attendance_date,e.employee_number", false);
            case "attendance-exceptions" -> new Query("SELECT e.employee_number,r.attendance_date,r.status,r.notes FROM attendance_record r"+employee+"WHERE r.tenant_id=:tenant AND r.attendance_date BETWEEN :from AND :to AND r.status IN ('MISSING_PUNCH','ABSENT','HALF_DAY') ORDER BY r.attendance_date,e.employee_number", false);
            case "attendance-summary" -> new Query("SELECT e.employee_number,r.status,COUNT(*) days,COALESCE(SUM(r.worked_minutes),0) worked_minutes,COALESCE(SUM(r.overtime_minutes),0) overtime_minutes FROM attendance_record r"+employee+"WHERE r.tenant_id=:tenant AND r.attendance_date BETWEEN :from AND :to GROUP BY e.employee_number,r.status ORDER BY e.employee_number,r.status", false);
            case "leave-balances" -> new Query("SELECT e.employee_number,t.code,r.leave_year,r.opening_balance,r.accrued,r.adjusted,r.used,(r.opening_balance+r.accrued+r.adjusted-r.used) available FROM leave_balance r"+employee+"JOIN leave_type t ON t.id=r.leave_type_id AND t.tenant_id=r.tenant_id WHERE r.tenant_id=:tenant AND r.leave_year BETWEEN EXTRACT(YEAR FROM CAST(:from AS DATE)) AND EXTRACT(YEAR FROM CAST(:to AS DATE)) ORDER BY e.employee_number,t.code", false);
            case "leave-transactions" -> new Query("SELECT e.employee_number,t.code,r.start_date,r.end_date,r.requested_days,r.status,r.requested_by,r.requested_at,r.decided_by,r.decided_at,r.decision_comment FROM leave_request r"+employee+"JOIN leave_type t ON t.id=r.leave_type_id AND t.tenant_id=r.tenant_id WHERE r.tenant_id=:tenant AND r.start_date<=:to AND r.end_date>=:from ORDER BY r.start_date,e.employee_number", false);
            case "salary-register" -> new Query("SELECT pr.payroll_year,pr.payroll_month,e.employee_number,r.gross_pay,r.deductions,r.employer_contributions,r.net_pay,r.ctc,r.payable_days"+payroll+period+" ORDER BY pr.payroll_year,pr.payroll_month,e.employee_number", true);
            case "bank-statement" -> new Query("SELECT pr.payroll_year,pr.payroll_month,e.employee_number,e.bank_account_name,e.bank_account_number,e.bank_ifsc,r.net_pay"+payroll+period+" AND e.payment_mode='BANK_TRANSFER' ORDER BY pr.payroll_year,pr.payroll_month,e.employee_number", true);
            case "payroll-components" -> new Query("SELECT pr.payroll_year,pr.payroll_month,e.employee_number,c.component_code,c.component_name,c.component_type,c.amount"+payroll+"JOIN payroll_component_result c ON c.payroll_employee_result_id=r.id AND c.tenant_id=r.tenant_id"+period+" ORDER BY pr.payroll_year,pr.payroll_month,e.employee_number,c.component_code", true);
            case "payroll-adjustments" -> new Query("SELECT pr.payroll_year,pr.payroll_month,e.employee_number,c.source_type,c.code,c.name,c.result_type,c.amount"+payroll+"JOIN payroll_adjustment_result c ON c.payroll_employee_result_id=r.id AND c.tenant_id=r.tenant_id"+period+" ORDER BY pr.payroll_year,pr.payroll_month,e.employee_number,c.code", true);
            case "department-payroll" -> new Query("SELECT pr.payroll_year,pr.payroll_month,COALESCE(d.name,'Unassigned') department,SUM(r.gross_pay) gross_pay,SUM(r.deductions) deductions,SUM(r.employer_contributions) employer_contributions,SUM(r.net_pay) net_pay"+payroll+"LEFT JOIN department d ON d.id=e.department_id AND d.tenant_id=e.tenant_id"+period+" GROUP BY pr.payroll_year,pr.payroll_month,d.name ORDER BY pr.payroll_year,pr.payroll_month,d.name", true);
            case "payslip-register" -> new Query("SELECT pr.payroll_year,pr.payroll_month,e.employee_number,p.status,p.generated_at,p.released_at"+payroll+"JOIN payslip p ON p.payroll_employee_result_id=r.id AND p.tenant_id=r.tenant_id"+period+" ORDER BY pr.payroll_year,pr.payroll_month,e.employee_number", true);
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown report");
        };
    }

    public static String csv(Report report) {
        StringBuilder out = new StringBuilder();
        List<String> header = new ArrayList<>(List.of("company", "tenant_id", "from", "to")); header.addAll(report.columns());
        append(out, header);
        for(List<String> row : report.rows()) {
            List<String> values = new ArrayList<>(List.of(report.company(), report.tenantId().toString(), report.from().toString(), report.to().toString()));
            values.addAll(row); append(out, values);
        }
        return out.toString();
    }
    private static void append(StringBuilder out, List<String> cells) {
        out.append(String.join(",", cells.stream().map(ReportingService::csvCell).toList())).append("\r\n");
    }
    static String csvCell(String value) {
        // Neutralize spreadsheet formulas, including values preceded by whitespace/control characters.
        String stripped = value.stripLeading();
        if(!stripped.isEmpty() && "=+-@".indexOf(stripped.charAt(0)) >= 0 || value.startsWith("\t") || value.startsWith("\r") || value.startsWith("\n")) value = "'" + value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
