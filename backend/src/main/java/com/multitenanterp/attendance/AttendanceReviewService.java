package com.multitenanterp.attendance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.multitenanterp.platform.tenant.TenantContext;
import jakarta.validation.constraints.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;

@Service
public class AttendanceReviewService {
    private final JdbcClient db;
    private final AttendanceService attendance;
    private final ObjectMapper json;
    public AttendanceReviewService(JdbcClient db,AttendanceService attendance,ObjectMapper json) {this.db=db;this.attendance=attendance;this.json=json;}
    public record Request(@NotNull UUID attendanceId,@Pattern(regexp="CORRECTION|OVERTIME") @NotBlank String reviewType,
                          Instant checkIn,Instant checkOut,@Min(0) int overtimeMinutes,@NotBlank @Size(max=1000) String reason) {}
    public record Decision(@NotBlank @Pattern(regexp="APPROVED|REJECTED") String decision,@NotBlank @Size(max=1000) String comment) {}
    public record Review(UUID id,UUID attendanceId,String reviewType,String status,String reason,int overtimeMinutes,
                         Instant checkIn,Instant checkOut,String requestedBy,String decidedBy,String comment) {}
    public List<Review> list() {return db.sql("SELECT * FROM attendance_review WHERE tenant_id=:tenant ORDER BY requested_at DESC").param("tenant",tenant()).query((r,n)->new Review(r.getObject("id",UUID.class),r.getObject("attendance_id",UUID.class),r.getString("review_type"),r.getString("status"),r.getString("reason"),r.getInt("overtime_minutes"),instant(r.getTimestamp("proposed_check_in")),instant(r.getTimestamp("proposed_check_out")),r.getString("requested_by"),r.getString("decided_by"),r.getString("decision_comment"))).list();}
    @Transactional public Review request(Request request,String actor) {
        attendance.lockTenant();
        AttendanceRecord record=record(request.attendanceId()); attendance.requireUnlocked(record.attendanceDate());
        if(request.reviewType().equals("CORRECTION") && (record.shiftId()==null || request.checkIn()==null || request.checkOut()==null || !request.checkOut().isAfter(request.checkIn()))) throw bad("Correction requires a shift and valid check-in/check-out times");
        if(request.reviewType().equals("OVERTIME") && (request.overtimeMinutes()<=0 || request.overtimeMinutes()>record.overtimeMinutes())) throw bad("Requested overtime must be positive and cannot exceed recorded overtime");
        UUID id=UUID.randomUUID();
        try {db.sql("INSERT INTO attendance_review(id,tenant_id,attendance_id,review_type,original_record,proposed_check_in,proposed_check_out,overtime_minutes,reason,requested_by) VALUES(:id,:tenant,:attendance,:type,:original,:in,:out,:minutes,:reason,:actor)")
            .param("id",id).param("tenant",tenant()).param("attendance",record.id()).param("type",request.reviewType()).param("original",encode(record))
            .param("in",request.checkIn()).param("out",request.checkOut()).param("minutes",request.overtimeMinutes()).param("reason",request.reason().trim()).param("actor",actor).update();}
        catch(DataIntegrityViolationException e) {throw conflict("A review of this type is already pending");}
        return find(id);
    }
    @Transactional public Review decide(UUID id,Decision decision,String actor) {
        attendance.lockTenant();
        Review review=find(id);
        if(!review.status().equals("PENDING")) throw conflict("Only pending reviews can be decided");
        if(review.requestedBy().equals(actor)) throw conflict("A different user must decide the review");
        AttendanceRecord current=record(review.attendanceId());
        if(decision.decision().equals("APPROVED")) {
            attendance.requireUnlocked(current.attendanceDate());
            String original=db.sql("SELECT original_record FROM attendance_review WHERE id=:id AND tenant_id=:tenant").param("id",id).param("tenant",tenant()).query(String.class).single();
            if(!original.equals(encode(current))) throw conflict("Attendance changed after submission; reject and submit a new review");
            if(review.reviewType().equals("CORRECTION")) attendance.saveAttendance(current.id(),new SaveAttendanceRequest(current.employmentId(),current.attendanceDate(),current.shiftId(),current.status(),review.checkIn(),review.checkOut(),current.workedMinutes(),0,"MANUAL",current.notes()));
            else db.sql("UPDATE attendance_record SET approved_overtime_minutes=:minutes,updated_at=CURRENT_TIMESTAMP WHERE id=:id AND tenant_id=:tenant").param("minutes",review.overtimeMinutes()).param("id",current.id()).param("tenant",tenant()).update();
        }
        db.sql("UPDATE attendance_review SET status=:status,decided_by=:actor,decided_at=CURRENT_TIMESTAMP,decision_comment=:comment WHERE id=:id AND tenant_id=:tenant")
                .param("status",decision.decision()).param("actor",actor).param("comment",decision.comment().trim()).param("id",id).param("tenant",tenant()).update();
        return find(id);
    }
    private AttendanceRecord record(UUID id) {
        var date=db.sql("SELECT attendance_date FROM attendance_record WHERE id=:id AND tenant_id=:tenant").param("id",id).param("tenant",tenant()).query(java.time.LocalDate.class).optional().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Attendance record not found"));
        return attendance.attendance(date,date,null).stream().filter(r->r.id().equals(id)).findFirst().orElseThrow();
    }
    private Review find(UUID id) {return list().stream().filter(r->r.id().equals(id)).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Attendance review not found"));}
    private String encode(AttendanceRecord value) {try{return json.writeValueAsString(value);}catch(JsonProcessingException e){throw new IllegalStateException(e);}}
    private static Instant instant(java.sql.Timestamp value) {return value==null?null:value.toInstant();}
    private static UUID tenant() {return TenantContext.requireTenantId();}
    private static ResponseStatusException bad(String message) {return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
    private static ResponseStatusException conflict(String message) {return new ResponseStatusException(HttpStatus.CONFLICT,message);}
}
