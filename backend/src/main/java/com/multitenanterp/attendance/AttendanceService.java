package com.multitenanterp.attendance;

import com.multitenanterp.platform.tenant.TenantContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AttendanceService {
    private final JdbcClient db;
    private final AttendanceEvaluationService evaluationService = new AttendanceEvaluationService();
    public AttendanceService(JdbcClient db) { this.db = db; }

    public List<WorkShift> shifts(LocalDate onDate) {
        String sql = "SELECT * FROM work_shift WHERE tenant_id=:tenant" +
                (onDate == null ? "" : " AND effective_from<=:onDate AND (effective_to IS NULL OR effective_to>=:onDate)") +
                " ORDER BY code,effective_from DESC";
        var query = db.sql(sql).param("tenant", tenant());
        if (onDate != null) query.param("onDate", onDate);
        return query.query(AttendanceService::mapShift).list();
    }

    public WorkShift saveShift(UUID id, SaveWorkShiftRequest r) {
        validateDates(r.effectiveFrom(), r.effectiveTo());
        if (r.halfDayMinutes() > r.fullDayMinutes()) throw badRequest("Half-day minutes cannot exceed full-day minutes");
        boolean create = id == null; id = create ? UUID.randomUUID() : id;
        try {
            int count = create ? db.sql("""
                    INSERT INTO work_shift(id,tenant_id,code,name,start_time,end_time,break_minutes,grace_in_minutes,
                      grace_out_minutes,full_day_minutes,half_day_minutes,effective_from,effective_to,status)
                    VALUES(:id,:tenant,:code,:name,:start,:end,:break,:graceIn,:graceOut,:fullDay,:halfDay,:from,:to,:status)
                    """).param("id",id).param("tenant",tenant()).param("code",normalize(r.code())).param("name",r.name().trim())
                    .param("start",r.startTime()).param("end",r.endTime()).param("break",r.breakMinutes())
                    .param("graceIn",r.graceInMinutes()).param("graceOut",r.graceOutMinutes()).param("fullDay",r.fullDayMinutes())
                    .param("halfDay",r.halfDayMinutes()).param("from",r.effectiveFrom()).param("to",r.effectiveTo())
                    .param("status",r.status()==null?"ACTIVE":normalize(r.status())).update()
                    : db.sql("""
                    UPDATE work_shift SET code=:code,name=:name,start_time=:start,end_time=:end,break_minutes=:break,
                      grace_in_minutes=:graceIn,grace_out_minutes=:graceOut,full_day_minutes=:fullDay,
                      half_day_minutes=:halfDay,effective_from=:from,effective_to=:to,status=:status,updated_at=CURRENT_TIMESTAMP
                    WHERE id=:id AND tenant_id=:tenant
                    """).param("id",id).param("tenant",tenant()).param("code",normalize(r.code())).param("name",r.name().trim())
                    .param("start",r.startTime()).param("end",r.endTime()).param("break",r.breakMinutes())
                    .param("graceIn",r.graceInMinutes()).param("graceOut",r.graceOutMinutes()).param("fullDay",r.fullDayMinutes())
                    .param("halfDay",r.halfDayMinutes()).param("from",r.effectiveFrom()).param("to",r.effectiveTo())
                    .param("status",r.status()==null?"ACTIVE":normalize(r.status())).update();
            if (count == 0) throw missing("Shift");
        } catch (DataIntegrityViolationException e) { throw conflict("Shift code, dates, or company scope is invalid"); }
        return shift(id);
    }

    public List<ShiftAssignment> assignments(UUID employmentId) {
        return db.sql("SELECT * FROM employee_shift_assignment WHERE tenant_id=:tenant AND employment_id=:employee ORDER BY effective_from DESC")
                .param("tenant",tenant()).param("employee",employmentId).query((r,n)->new ShiftAssignment(r.getObject("id",UUID.class),
                        r.getObject("employment_id",UUID.class),r.getObject("shift_id",UUID.class),r.getDate("effective_from").toLocalDate(),date(r,"effective_to"))).list();
    }

    public ShiftAssignment assignShift(SaveShiftAssignmentRequest r) {
        validateDates(r.effectiveFrom(),r.effectiveTo());
        UUID id=UUID.randomUUID();
        try { db.sql("INSERT INTO employee_shift_assignment(id,tenant_id,employment_id,shift_id,effective_from,effective_to) VALUES(:id,:tenant,:employee,:shift,:from,:to)")
                .param("id",id).param("tenant",tenant()).param("employee",r.employmentId()).param("shift",r.shiftId())
                .param("from",r.effectiveFrom()).param("to",r.effectiveTo()).update();
        } catch (DataIntegrityViolationException e) { throw conflict("Employee, shift, dates, or company scope is invalid"); }
        return assignments(r.employmentId()).stream().filter(a->a.id().equals(id)).findFirst().orElseThrow();
    }

    public List<AttendanceRecord> attendance(LocalDate from, LocalDate to, UUID employmentId) {
        if (from.isAfter(to)) throw badRequest("From date cannot follow to date");
        String employeeFilter=employmentId==null?"":" AND employment_id=:employee";
        var query=db.sql("SELECT * FROM attendance_record WHERE tenant_id=:tenant AND attendance_date BETWEEN :from AND :to"+employeeFilter+" ORDER BY attendance_date,employment_id")
                .param("tenant",tenant()).param("from",from).param("to",to);
        if(employmentId!=null)query.param("employee",employmentId);
        return query.query(AttendanceService::mapAttendance).list();
    }

    @Transactional
    public AttendanceRecord saveAttendance(UUID id, SaveAttendanceRequest r) {
        requireUnlocked(r.attendanceDate());

        if (id != null) {
            LocalDate existingDate = db.sql("""
                    SELECT attendance_date
                    FROM attendance_record
                    WHERE id=:id AND tenant_id=:tenant
                    """)
                    .param("id", id)
                    .param("tenant", tenant())
                    .query(LocalDate.class)
                    .optional()
                    .orElseThrow(() -> missing("Attendance record"));
            requireUnlocked(existingDate);
        }

        if (r.checkOut() != null && (r.checkIn() == null || r.checkOut().isBefore(r.checkIn())))
            throw badRequest("Check-out cannot precede check-in");

        EvaluatedAttendance evaluated = evaluate(r);
        boolean create=id==null; id=create?UUID.randomUUID():id;
        try {
            int count=create?db.sql("""
                    INSERT INTO attendance_record(id,tenant_id,employment_id,attendance_date,shift_id,status,check_in,check_out,worked_minutes,overtime_minutes,source,notes)
                    VALUES(:id,:tenant,:employee,:date,:shift,:status,:in,:out,:worked,:overtime,:source,:notes)
                    """).param("id",id).param("tenant",tenant()).param("employee",r.employmentId()).param("date",r.attendanceDate()).param("shift",r.shiftId())
                    .param("status",evaluated.status()).param("in",r.checkIn()).param("out",r.checkOut()).param("worked",evaluated.workedMinutes())
                    .param("overtime",evaluated.overtimeMinutes()).param("source",r.source()==null?"MANUAL":normalize(r.source())).param("notes",clean(r.notes())).update()
                    :db.sql("""
                    UPDATE attendance_record SET employment_id=:employee,attendance_date=:date,shift_id=:shift,status=:status,
                      check_in=:in,check_out=:out,worked_minutes=:worked,overtime_minutes=:overtime,source=:source,notes=:notes,updated_at=CURRENT_TIMESTAMP
                    WHERE id=:id AND tenant_id=:tenant
                    """).param("id",id).param("tenant",tenant()).param("employee",r.employmentId()).param("date",r.attendanceDate()).param("shift",r.shiftId())
                    .param("status",evaluated.status()).param("in",r.checkIn()).param("out",r.checkOut()).param("worked",evaluated.workedMinutes())
                    .param("overtime",evaluated.overtimeMinutes()).param("source",r.source()==null?"MANUAL":normalize(r.source())).param("notes",clean(r.notes())).update();
            if(count==0)throw missing("Attendance record");
        } catch(DataIntegrityViolationException e){throw conflict("Attendance already exists or references another company");}
        UUID saved=id; return attendance(r.attendanceDate(),r.attendanceDate(),r.employmentId()).stream().filter(a->a.id().equals(saved)).findFirst().orElseThrow();
    }

    public AttendanceMonthLock lockMonth(YearMonth month, String actor) {
        LocalDate first=month.atDay(1);
        try { db.sql("INSERT INTO attendance_month_lock(id,tenant_id,attendance_month,locked_by) VALUES(:id,:tenant,:month,:actor)")
                .param("id",UUID.randomUUID()).param("tenant",tenant()).param("month",first).param("actor",actor).update();
        } catch(DataIntegrityViolationException e){throw conflict("Attendance month is already locked");}
        return monthLock(first);
    }

    public void reopenMonth(YearMonth month) {
        int count=db.sql("DELETE FROM attendance_month_lock WHERE tenant_id=:tenant AND attendance_month=:month")
                .param("tenant",tenant()).param("month",month.atDay(1)).update();
        if(count==0)throw missing("Attendance month lock");
    }

    private EvaluatedAttendance evaluate(SaveAttendanceRequest request) {
        if (request.shiftId() == null
                || (request.checkIn() == null && request.checkOut() == null)) {
            return new EvaluatedAttendance(
                    normalize(request.status()),
                    request.workedMinutes(),
                    request.overtimeMinutes()
            );
        }

        AttendanceEvaluation result = evaluationService.evaluate(
                shift(request.shiftId()),
                request.attendanceDate(),
                request.checkIn(),
                request.checkOut(),
                java.time.ZoneId.systemDefault()
        );

        return new EvaluatedAttendance(
                result.status(), result.workedMinutes(), result.overtimeMinutes()
        );
    }

    private record EvaluatedAttendance(String status, Integer workedMinutes, int overtimeMinutes) {}

    private WorkShift shift(UUID id){return shifts(null).stream().filter(s->s.id().equals(id)).findFirst().orElseThrow(()->missing("Shift"));}
    private AttendanceMonthLock monthLock(LocalDate month){return db.sql("SELECT attendance_month,locked_by,locked_at FROM attendance_month_lock WHERE tenant_id=:tenant AND attendance_month=:month")
            .param("tenant",tenant()).param("month",month).query((r,n)->new AttendanceMonthLock(r.getDate(1).toLocalDate(),r.getString(2),r.getTimestamp(3).toInstant())).single();}
    private void requireUnlocked(LocalDate date){if(db.sql("SELECT COUNT(*) FROM attendance_month_lock WHERE tenant_id=:tenant AND attendance_month=:month")
            .param("tenant",tenant()).param("month",date.withDayOfMonth(1)).query(Integer.class).single()>0)throw conflict("Attendance month is locked");}
    private static WorkShift mapShift(ResultSet r,int n)throws SQLException{return new WorkShift(r.getObject("id",UUID.class),r.getString("code"),r.getString("name"),r.getTime("start_time").toLocalTime(),r.getTime("end_time").toLocalTime(),r.getInt("break_minutes"),r.getInt("grace_in_minutes"),r.getInt("grace_out_minutes"),r.getInt("full_day_minutes"),r.getInt("half_day_minutes"),r.getDate("effective_from").toLocalDate(),date(r,"effective_to"),r.getString("status"));}
    private static AttendanceRecord mapAttendance(ResultSet r,int n)throws SQLException{return new AttendanceRecord(r.getObject("id",UUID.class),r.getObject("employment_id",UUID.class),r.getDate("attendance_date").toLocalDate(),r.getObject("shift_id",UUID.class),r.getString("status"),instant(r,"check_in"),instant(r,"check_out"),(Integer)r.getObject("worked_minutes"),r.getInt("overtime_minutes"),r.getString("source"),r.getString("notes"));}
    private static LocalDate date(ResultSet r,String c)throws SQLException{var v=r.getDate(c);return v==null?null:v.toLocalDate();}
    private static java.time.Instant instant(ResultSet r,String c)throws SQLException{var v=r.getTimestamp(c);return v==null?null:v.toInstant();}
    private static void validateDates(LocalDate from,LocalDate to){if(to!=null&&to.isBefore(from))throw badRequest("Effective-to date cannot precede effective-from date");}
    private static UUID tenant(){return TenantContext.requireTenantId();}
    private static String normalize(String v){return v.trim().toUpperCase(Locale.ROOT);}
    private static String clean(String v){return v==null||v.isBlank()?null:v.trim();}
    private static ResponseStatusException badRequest(String m){return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}
    private static ResponseStatusException conflict(String m){return new ResponseStatusException(HttpStatus.CONFLICT,m);}
    private static ResponseStatusException missing(String n){return new ResponseStatusException(HttpStatus.NOT_FOUND,n+" not found");}
}
