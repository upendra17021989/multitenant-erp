package com.multitenanterp.leave;

import com.multitenanterp.platform.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class LeaveAccrualService {
    private final JdbcClient db;
    private final LeaveService leave;
    public LeaveAccrualService(JdbcClient db,LeaveService leave){this.db=db;this.leave=leave;}
    public record Settings(LocalDate accrualStart,String timeZone) {}
    public record Event(UUID employmentId,UUID leaveTypeId,int leaveYear,String eventType,LocalDate periodEnd,BigDecimal amount) {}
    private record Employment(UUID id,LocalDate joining,LocalDate exit) {}
    public Settings settings(){return db.sql("SELECT leave_accrual_start,time_zone FROM tenant WHERE id=:tenant").param("tenant",tenant()).query((r,n)->new Settings(r.getDate(1)==null?null:r.getDate(1).toLocalDate(),r.getString(2))).single();}
    @Transactional public Settings configure(LocalDate start) {
        lock();
        if(start!=null && (start.isBefore(LocalDate.now().minusYears(1)) || start.isAfter(LocalDate.now().plusYears(1)))) throw bad("Accrual start must be within one year of today");
        if(db.sql("SELECT COUNT(*) FROM leave_processing_event WHERE tenant_id=:tenant").param("tenant",tenant()).query(Integer.class).single()>0 && !Objects.equals(start,settings().accrualStart())) throw bad("Accrual start cannot change after processing; use balance adjustments for corrections");
        db.sql("UPDATE tenant SET leave_accrual_start=:start WHERE id=:tenant").param("start",start).param("tenant",tenant()).update();return settings();
    }
    public List<Event> events(){return db.sql("SELECT * FROM leave_processing_event WHERE tenant_id=:tenant ORDER BY processed_at DESC").param("tenant",tenant()).query((r,n)->new Event(r.getObject("employment_id",UUID.class),r.getObject("leave_type_id",UUID.class),r.getInt("leave_year"),r.getString("event_type"),r.getDate("period_end").toLocalDate(),r.getBigDecimal("amount"))).list();}
    @Transactional public int process(LocalDate asOf) {
        lock();Settings settings=settings();
        if(settings.accrualStart()==null) return 0;
        if(asOf.isAfter(LocalDate.now(ZoneId.of(settings.timeZone())))) throw bad("Cannot process future leave accrual");
        List<Employment> employees=db.sql("SELECT id,joining_date,exit_date FROM employment WHERE tenant_id=:tenant AND joining_date<=:date").param("tenant",tenant()).param("date",asOf).query((r,n)->new Employment(r.getObject(1,UUID.class),r.getDate(2).toLocalDate(),r.getDate(3)==null?null:r.getDate(3).toLocalDate())).list();
        int count=0;
        for(LeaveType type:leave.types()) {
            if(!type.status().equals("ACTIVE"))continue;
            for(Employment employee:employees) {
                LocalDate start=max(settings.accrualStart(),employee.joining());
                for(int year=start.getYear();year<=asOf.getYear();year++) {
                    if(year>start.getYear() && !asOf.isBefore(LocalDate.of(year,1,1))) {
                        BigDecimal available=leave.balances(employee.id(),year-1).stream().filter(b->b.leaveTypeId().equals(type.id())).map(LeaveBalance::available).findFirst().orElse(BigDecimal.ZERO);
                        if(employee.exit()==null || !employee.exit().isBefore(LocalDate.of(year,1,1)))
                            count+=credit(employee.id(),type.id(),year,"CARRY_FORWARD",LocalDate.of(year,1,1),available.max(BigDecimal.ZERO).min(type.carryForwardLimit()));
                    }
                    int months=switch(type.accrualFrequency()){case "MONTHLY"->1;case "QUARTERLY"->3;case "ANNUAL"->12;default->0;};
                    if(months==0)continue;
                    BigDecimal previousTarget=BigDecimal.ZERO;
                    for(int month=months;month<=12;month+=months) {
                        LocalDate end=YearMonth.of(year,month).atEndOfMonth();
                        if(!end.isBefore(asOf))break;
                        LocalDate employmentEnd=employee.exit()==null?end:min(end,employee.exit());
                        long eligible=employmentEnd.isBefore(max(start,LocalDate.of(year,1,1)))?0:ChronoUnit.DAYS.between(max(start,LocalDate.of(year,1,1)),employmentEnd)+1;
                        BigDecimal target=type.annualEntitlement().multiply(BigDecimal.valueOf(eligible)).divide(BigDecimal.valueOf(Year.isLeap(year)?366:365),2,RoundingMode.HALF_UP);
                        BigDecimal amount=target.subtract(previousTarget);previousTarget=target;
                        if(!end.isBefore(start))count+=credit(employee.id(),type.id(),year,"ACCRUAL",end,amount);
                    }
                }
            }
        }
        return count;
    }
    private int credit(UUID employee,UUID type,int year,String event,LocalDate end,BigDecimal amount) {
        if(db.sql("SELECT COUNT(*) FROM leave_processing_event WHERE tenant_id=:tenant AND employment_id=:employee AND leave_type_id=:type AND event_type=:event AND period_end=:end").param("tenant",tenant()).param("employee",employee).param("type",type).param("event",event).param("end",end).query(Integer.class).single()>0)return 0;
        UUID balance=db.sql("SELECT id FROM leave_balance WHERE tenant_id=:tenant AND employment_id=:employee AND leave_type_id=:type AND leave_year=:year").param("tenant",tenant()).param("employee",employee).param("type",type).param("year",year).query(UUID.class).optional().orElse(null);
        if(balance==null){balance=UUID.randomUUID();db.sql("INSERT INTO leave_balance(id,tenant_id,employment_id,leave_type_id,leave_year) VALUES(:id,:tenant,:employee,:type,:year)").param("id",balance).param("tenant",tenant()).param("employee",employee).param("type",type).param("year",year).update();}
        String column=event.equals("ACCRUAL")?"accrued":"opening_balance";
        db.sql("UPDATE leave_balance SET "+column+"="+column+"+:amount,updated_at=CURRENT_TIMESTAMP WHERE id=:id AND tenant_id=:tenant").param("amount",amount).param("id",balance).param("tenant",tenant()).update();
        db.sql("INSERT INTO leave_processing_event(id,tenant_id,employment_id,leave_type_id,leave_year,event_type,period_end,amount) VALUES(:id,:tenant,:employee,:type,:year,:event,:end,:amount)").param("id",UUID.randomUUID()).param("tenant",tenant()).param("employee",employee).param("type",type).param("year",year).param("event",event).param("end",end).param("amount",amount).update();
        return 1;
    }
    private void lock(){db.sql("SELECT id FROM tenant WHERE id=:tenant FOR UPDATE").param("tenant",tenant()).query(UUID.class).single();}
    private static LocalDate max(LocalDate a,LocalDate b){return a.isAfter(b)?a:b;}
    private static LocalDate min(LocalDate a,LocalDate b){return a.isBefore(b)?a:b;}
    private static UUID tenant(){return TenantContext.requireTenantId();}
    private static ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}
