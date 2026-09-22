package com.multitenanterp.attendance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.multitenanterp.platform.tenant.TenantContext;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class AttendanceReviewIntegrationTest {
    private JdbcClient db;
    private AttendanceService attendance;
    private AttendanceReviewService reviews;
    private UUID tenant, other, employee;
    private final LocalDate day=LocalDate.of(2026,9,21);
    @BeforeEach void setup() {
        var ds=new JdbcDataSource(); ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=PostgreSQL;DB_CLOSE_DELAY=-1"); // Reject the Instant binding that H2 accepts but PostgreSQL does not support.
        db=JdbcClient.create(new DelegatingDataSource(ds) {
            @Override public Connection getConnection() throws SQLException {
                return strictJdbc(Connection.class, super.getConnection());
            }
        });
        db.sql("CREATE TABLE tenant(id UUID PRIMARY KEY,time_zone VARCHAR DEFAULT 'Asia/Kolkata')").update();
        db.sql("CREATE TABLE attendance_record(id UUID PRIMARY KEY,tenant_id UUID,employment_id UUID,attendance_date DATE,shift_id UUID,status VARCHAR,check_in TIMESTAMP WITH TIME ZONE,check_out TIMESTAMP WITH TIME ZONE,worked_minutes INT,overtime_minutes INT,approved_overtime_minutes INT DEFAULT 0,source VARCHAR,notes VARCHAR,updated_at TIMESTAMP,UNIQUE(tenant_id,employment_id,attendance_date))").update();
        db.sql("CREATE TABLE attendance_month_lock(id UUID PRIMARY KEY,tenant_id UUID,attendance_month DATE,locked_by VARCHAR,locked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,UNIQUE(tenant_id,attendance_month))").update();
        db.sql("CREATE TABLE attendance_review(id UUID PRIMARY KEY,tenant_id UUID,attendance_id UUID,review_type VARCHAR,original_record VARCHAR,proposed_check_in TIMESTAMP WITH TIME ZONE,proposed_check_out TIMESTAMP WITH TIME ZONE,overtime_minutes INT,reason VARCHAR,status VARCHAR DEFAULT 'PENDING',requested_by VARCHAR,requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,decided_by VARCHAR,decided_at TIMESTAMP,decision_comment VARCHAR)").update();
        db.sql("CREATE TABLE work_shift(id UUID PRIMARY KEY,tenant_id UUID,code VARCHAR,name VARCHAR,start_time TIME,end_time TIME,break_minutes INT,grace_in_minutes INT,grace_out_minutes INT,full_day_minutes INT,half_day_minutes INT,effective_from DATE,effective_to DATE,status VARCHAR,updated_at TIMESTAMP)").update();
        tenant=UUID.randomUUID(); other=UUID.randomUUID(); employee=UUID.randomUUID();
        db.sql("INSERT INTO tenant(id) VALUES(?),(?)").params(tenant,other).update(); TenantContext.set(tenant);
        attendance=new AttendanceService(db); reviews=new AttendanceReviewService(db,attendance,new ObjectMapper().findAndRegisterModules());
    }
    @AfterEach void clear(){TenantContext.clear();}
    private static <T> T strictJdbc(Class<T> type, T delegate) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if (method.getName().equals("setObject") && args[1] instanceof Instant) {
                throw new SQLException("Cannot infer SQL type for java.time.Instant", "07006");
            }
            try {
                Object result = method.invoke(delegate, args);
                return result instanceof PreparedStatement statement
                        ? strictJdbc(PreparedStatement.class, statement) : result;
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }));
    }
    @Test void punchTimesRoundTripOnCreateUpdateAndClear() {
        Instant checkIn=OffsetDateTime.parse("2026-09-21T09:00:00+05:30").toInstant();
        Instant checkOut=checkIn.plusSeconds(8*3600);
        AttendanceRecord created=attendance.saveAttendance(null,new SaveAttendanceRequest(employee,day,null,"PRESENT",checkIn,checkOut,480,0,"MANUAL",null));
        assertThat(created.checkIn()).isEqualTo(checkIn);
        assertThat(created.checkOut()).isEqualTo(checkOut);
        AttendanceRecord updated=attendance.saveAttendance(created.id(),new SaveAttendanceRequest(employee,day,null,"PRESENT",checkIn,checkOut.plusSeconds(3600),540,60,"MANUAL",null));
        assertThat(updated.checkOut()).isEqualTo(checkOut.plusSeconds(3600));
        AttendanceRecord cleared=attendance.saveAttendance(created.id(),new SaveAttendanceRequest(employee,day,null,"ABSENT",null,null,null,0,"MANUAL",null));
        assertThat(cleared.checkIn()).isNull();
        assertThat(cleared.checkOut()).isNull();
    }
    private AttendanceRecord record(){return attendance.saveAttendance(null,new SaveAttendanceRequest(employee,day,null,"PRESENT",null,null,540,60,"MANUAL",null));}
    @Test void overtimeRequiresSeparateApproverAndResetsAfterEdit() {
        AttendanceRecord record=record();
        var review=reviews.request(new AttendanceReviewService.Request(record.id(),"OVERTIME",null,null,45,"Additional work"),"maker");
        assertThatThrownBy(()->reviews.decide(review.id(),new AttendanceReviewService.Decision("APPROVED","Checked"),"maker")).isInstanceOf(ResponseStatusException.class);
        reviews.decide(review.id(),new AttendanceReviewService.Decision("APPROVED","Checked"),"checker");
        assertThat(db.sql("SELECT approved_overtime_minutes FROM attendance_record WHERE id=?").param(record.id()).query(Integer.class).single()).isEqualTo(45);
        attendance.saveAttendance(record.id(),new SaveAttendanceRequest(employee,day,null,"PRESENT",null,null,480,0,"MANUAL",null));
        assertThat(db.sql("SELECT approved_overtime_minutes FROM attendance_record WHERE id=?").param(record.id()).query(Integer.class).single()).isZero();
    }
    @Test void monthLockAndTenantScopeProtectRecordsAndReviews() {
        AttendanceRecord record=record();
        var review=reviews.request(new AttendanceReviewService.Request(record.id(),"OVERTIME",null,null,30,"Additional work"),"maker");
        attendance.lockMonth(YearMonth.from(day),"manager");
        assertThatThrownBy(()->reviews.decide(review.id(),new AttendanceReviewService.Decision("APPROVED","Checked"),"checker")).isInstanceOf(ResponseStatusException.class).hasMessageContaining("locked");
        assertThatThrownBy(()->attendance.saveAttendance(record.id(),new SaveAttendanceRequest(employee,day.plusMonths(1),null,"PRESENT",null,null,480,0,"MANUAL",null))).isInstanceOf(ResponseStatusException.class).hasMessageContaining("locked");
        TenantContext.set(other);
        assertThat(attendance.attendance(day,day,null)).isEmpty(); assertThat(reviews.list()).isEmpty();
        assertThatThrownBy(()->reviews.decide(review.id(),new AttendanceReviewService.Decision("APPROVED","Checked"),"checker")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->attendance.reopenMonth(YearMonth.from(day))).isInstanceOf(ResponseStatusException.class);
        attendance.lockMonth(YearMonth.from(day),"other manager");
        TenantContext.set(tenant); attendance.reopenMonth(YearMonth.from(day));
        reviews.decide(review.id(),new AttendanceReviewService.Decision("APPROVED","Checked"),"checker");
    }
    @Test void correctionReevaluatesPunchesInCompanyTimezone() {
        WorkShift shift=attendance.saveShift(null,new SaveWorkShiftRequest("DAY","Day",LocalTime.of(9,0),LocalTime.of(18,0),60,0,0,480,240,day,null,"ACTIVE"));
        AttendanceRecord record=attendance.saveAttendance(null,new SaveAttendanceRequest(employee,day,shift.id(),"MISSING_PUNCH",Instant.parse("2026-09-21T03:30:00Z"),null,null,0,"MANUAL",null));
        var review=reviews.request(new AttendanceReviewService.Request(record.id(),"CORRECTION",Instant.parse("2026-09-21T03:30:00Z"),Instant.parse("2026-09-21T12:30:00Z"),0,"Forgot check out"),"maker");
        reviews.decide(review.id(),new AttendanceReviewService.Decision("APPROVED","Verified"),"checker");
        assertThat(attendance.attendance(day,day,employee).getFirst().status()).isEqualTo("PRESENT");
        assertThat(attendance.attendance(day,day,employee).getFirst().workedMinutes()).isEqualTo(480);
    }
    @Test void staleReviewCannotOverwriteChangedAttendance() {
        AttendanceRecord record=record();
        var review=reviews.request(new AttendanceReviewService.Request(record.id(),"OVERTIME",null,null,30,"Work"),"maker");
        attendance.saveAttendance(record.id(),new SaveAttendanceRequest(employee,day,null,"PRESENT",null,null,500,20,"MANUAL",null));
        assertThatThrownBy(()->reviews.decide(review.id(),new AttendanceReviewService.Decision("APPROVED","Checked"),"checker")).isInstanceOf(ResponseStatusException.class).hasMessageContaining("changed");
    }
}
