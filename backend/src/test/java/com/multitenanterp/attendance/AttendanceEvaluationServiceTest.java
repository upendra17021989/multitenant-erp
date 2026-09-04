package com.multitenanterp.attendance;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttendanceEvaluationServiceTest {
    private final AttendanceEvaluationService service = new AttendanceEvaluationService();
    private final ZoneId zone = ZoneId.of("Asia/Kolkata");
    private final LocalDate day = LocalDate.of(2026, 9, 4);

    @Test void evaluatesFullHalfAbsentAndMissingPunch() {
        WorkShift shift = shift(LocalTime.of(9,0), LocalTime.of(18,0));
        assertThat(evaluate(shift,9,5,18,5)).extracting(AttendanceEvaluation::status,
                AttendanceEvaluation::workedMinutes).containsExactly("PRESENT",480);
        assertThat(evaluate(shift,9,0,14,0).status()).isEqualTo("HALF_DAY");
        assertThat(evaluate(shift,9,0,12,0).status()).isEqualTo("ABSENT");
        assertThat(service.evaluate(shift,day,null,null,zone).status()).isEqualTo("MISSING_PUNCH");
    }

    @Test void appliesGraceOvertimeAndOvernightSchedule() {
        AttendanceEvaluation result=evaluate(shift(LocalTime.of(9,0),LocalTime.of(18,0)),9,11,19,0);
        assertThat(result.lateArrival()).isTrue();
        assertThat(result.overtimeMinutes()).isEqualTo(49);
        WorkShift night=shift(LocalTime.of(22,0),LocalTime.of(6,0));
        Instant in=day.atTime(22,0).atZone(zone).toInstant();
        Instant out=day.plusDays(1).atTime(6,0).atZone(zone).toInstant();
        assertThat(service.evaluate(night,day,in,out,zone).earlyDeparture()).isFalse();
    }

    @Test void rejectsReversePunches() {
        WorkShift shift=shift(LocalTime.of(9,0),LocalTime.of(18,0));
        Instant in=day.atTime(18,0).atZone(zone).toInstant();
        Instant out=day.atTime(9,0).atZone(zone).toInstant();
        assertThatThrownBy(()->service.evaluate(shift,day,in,out,zone)).isInstanceOf(ResponseStatusException.class);
    }

    private AttendanceEvaluation evaluate(WorkShift shift,int inHour,int inMinute,int outHour,int outMinute){
        return service.evaluate(shift,day,day.atTime(inHour,inMinute).atZone(zone).toInstant(),day.atTime(outHour,outMinute).atZone(zone).toInstant(),zone);
    }
    private WorkShift shift(LocalTime start,LocalTime end){
        return new WorkShift(UUID.randomUUID(),"SHIFT","Shift",start,end,60,10,10,480,240,day,null,"ACTIVE");
    }
}
