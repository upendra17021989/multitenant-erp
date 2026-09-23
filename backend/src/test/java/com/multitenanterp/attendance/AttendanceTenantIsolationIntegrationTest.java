package com.multitenanterp.attendance;

import com.multitenanterp.platform.tenant.TenantContext;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttendanceTenantIsolationIntegrationTest {
    private final UUID tenantA=UUID.randomUUID(),tenantB=UUID.randomUUID();
    private final UUID employeeA=UUID.randomUUID(),employeeB=UUID.randomUUID();
    private JdbcClient db;
    private AttendanceService service;

    @BeforeEach void setUp(){
        JdbcDataSource ds=new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        db=JdbcClient.create(ds);
        db.sql("CREATE TABLE tenant(id UUID PRIMARY KEY,time_zone VARCHAR(80) NOT NULL DEFAULT 'UTC')").update();
        db.sql("CREATE TABLE employment(id UUID PRIMARY KEY,tenant_id UUID NOT NULL,UNIQUE(id,tenant_id))").update();
        db.sql("""
                CREATE TABLE work_shift(id UUID PRIMARY KEY,tenant_id UUID NOT NULL,code VARCHAR(40),name VARCHAR(160),
                  start_time TIME,end_time TIME,break_minutes INT,grace_in_minutes INT,grace_out_minutes INT,
                  full_day_minutes INT,half_day_minutes INT,effective_from DATE,effective_to DATE,status VARCHAR(20),
                  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,UNIQUE(tenant_id,code,effective_from),UNIQUE(id,tenant_id))
                """).update();
        db.sql("""
                CREATE TABLE employee_shift_assignment(id UUID PRIMARY KEY,tenant_id UUID NOT NULL,employment_id UUID NOT NULL,
                  shift_id UUID NOT NULL,effective_from DATE,effective_to DATE,UNIQUE(tenant_id,employment_id,effective_from),
                  FOREIGN KEY(employment_id,tenant_id) REFERENCES employment(id,tenant_id),
                  FOREIGN KEY(shift_id,tenant_id) REFERENCES work_shift(id,tenant_id))
                """).update();
        db.sql("""
                CREATE TABLE attendance_record(id UUID PRIMARY KEY,tenant_id UUID NOT NULL,employment_id UUID NOT NULL,
                  attendance_date DATE NOT NULL,shift_id UUID,status VARCHAR(30),check_in TIMESTAMP WITH TIME ZONE,
                  check_out TIMESTAMP WITH TIME ZONE,worked_minutes INT,overtime_minutes INT DEFAULT 0,
                  approved_overtime_minutes INT DEFAULT 0,source VARCHAR(30),notes VARCHAR(500),updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  UNIQUE(tenant_id,employment_id,attendance_date),FOREIGN KEY(employment_id,tenant_id) REFERENCES employment(id,tenant_id),
                  FOREIGN KEY(shift_id,tenant_id) REFERENCES work_shift(id,tenant_id))
                """).update();
        db.sql("""
                CREATE TABLE attendance_month_lock(id UUID PRIMARY KEY,tenant_id UUID NOT NULL,attendance_month DATE NOT NULL,
                  locked_by VARCHAR(255) NOT NULL,locked_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                  UNIQUE(tenant_id,attendance_month))
                """).update();
        db.sql("INSERT INTO tenant(id) VALUES(:a),(:b)").param("a",tenantA).param("b",tenantB).update();
        db.sql("INSERT INTO employment(id,tenant_id) VALUES(:a,:ta),(:b,:tb)")
                .param("a",employeeA).param("ta",tenantA).param("b",employeeB).param("tb",tenantB).update();
        service=new AttendanceService(db);
    }

    @AfterEach void clear(){TenantContext.clear();}

    @Test void shiftsAndAssignmentsAreTenantIsolated(){
        TenantContext.set(tenantA);
        WorkShift shiftA=createShift("DAY");
        service.assignShift(new SaveShiftAssignmentRequest(employeeA,shiftA.id(),LocalDate.of(2026,1,1),null));
        TenantContext.set(tenantB);
        WorkShift shiftB=createShift("DAY");
        assertThat(service.shifts(null)).extracting(WorkShift::id).containsExactly(shiftB.id());
        assertThat(service.assignments(employeeA)).isEmpty();
        assertThatThrownBy(()->service.assignShift(new SaveShiftAssignmentRequest(employeeB,shiftA.id(),LocalDate.of(2026,1,1),null)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("company scope");
    }

    @Test void attendanceCannotBeReadUpdatedOrCreatedAcrossTenants(){
        LocalDate day=LocalDate.of(2026,9,15);
        TenantContext.set(tenantA);
        WorkShift shiftA=createShift("DAY");
        AttendanceRecord recordA=service.saveAttendance(null,request(employeeA,shiftA.id(),day));
        TenantContext.set(tenantB);
        WorkShift shiftB=createShift("DAY");
        assertThat(service.attendance(day,day,null)).isEmpty();
        assertThatThrownBy(()->service.saveAttendance(recordA.id(),request(employeeB,shiftB.id(),day)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("not found");
        assertThatThrownBy(()->service.saveAttendance(null,request(employeeB,shiftA.id(),day)))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(db.sql("SELECT tenant_id FROM attendance_record WHERE id=:id").param("id",recordA.id())
                .query(UUID.class).single()).isEqualTo(tenantA);
    }

    @Test void monthLocksOnlyAffectTheirOwnTenant(){
        YearMonth month=YearMonth.of(2026,9);
        LocalDate day=month.atDay(15);
        TenantContext.set(tenantA);
        service.lockMonth(month,"admin-a");
        TenantContext.set(tenantB);
        WorkShift shiftB=createShift("DAY");
        assertThat(service.saveAttendance(null,request(employeeB,shiftB.id(),day))).isNotNull();
        assertThatThrownBy(()->service.reopenMonth(month))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("not found");
        TenantContext.set(tenantA);
        WorkShift shiftA=createShift("DAY");
        assertThatThrownBy(()->service.saveAttendance(null,request(employeeA,shiftA.id(),day)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("locked");
    }

    private WorkShift createShift(String code){
        return service.saveShift(null,new SaveWorkShiftRequest(code,"Day",LocalTime.of(9,0),LocalTime.of(18,0),
                60,10,10,480,240,LocalDate.of(2026,1,1),null,"ACTIVE"));
    }

    private static SaveAttendanceRequest request(UUID employee,UUID shift,LocalDate day){
        return new SaveAttendanceRequest(employee,day,shift,"PRESENT",null,null,480,0,"MANUAL",null);
    }
}
