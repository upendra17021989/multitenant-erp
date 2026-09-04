package com.multitenanterp.attendance;

import com.multitenanterp.platform.tenant.TenantContext;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AttendanceTenantIsolationIntegrationTest {
    @AfterEach void clear(){TenantContext.clear();}

    @Test void shiftsAreTenantIsolated(){
        UUID tenantA=UUID.randomUUID(),tenantB=UUID.randomUUID();
        JdbcDataSource ds=new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        JdbcClient db=JdbcClient.create(ds);
        db.sql("CREATE TABLE work_shift(id UUID PRIMARY KEY,tenant_id UUID NOT NULL,code VARCHAR(40),name VARCHAR(160),start_time TIME,end_time TIME,break_minutes INT,grace_in_minutes INT,grace_out_minutes INT,full_day_minutes INT,half_day_minutes INT,effective_from DATE,effective_to DATE,status VARCHAR(20),updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,UNIQUE(tenant_id,code,effective_from))").update();
        AttendanceService service=new AttendanceService(db);
        TenantContext.set(tenantA);
        WorkShift shift=service.saveShift(null,new SaveWorkShiftRequest("DAY","Day",LocalTime.of(9,0),LocalTime.of(18,0),60,10,10,480,240,LocalDate.of(2026,1,1),null,"ACTIVE"));
        assertThat(service.shifts(null)).extracting(WorkShift::id).containsExactly(shift.id());
        TenantContext.set(tenantB);
        assertThat(service.shifts(null)).isEmpty();
    }
}
