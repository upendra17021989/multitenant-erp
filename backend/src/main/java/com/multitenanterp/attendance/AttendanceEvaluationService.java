package com.multitenanterp.attendance;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
public class AttendanceEvaluationService {
    public AttendanceEvaluation evaluate(WorkShift shift, LocalDate attendanceDate,
                                         Instant checkIn, Instant checkOut, ZoneId zone) {
        if (checkIn == null || checkOut == null) {
            return new AttendanceEvaluation("MISSING_PUNCH", 0, 0, false, false);
        }
        if (checkOut.isBefore(checkIn)) throw badRequest("Check-out cannot precede check-in");

        int elapsed = Math.toIntExact(Duration.between(checkIn, checkOut).toMinutes());
        int worked = Math.max(0, elapsed - shift.breakMinutes());
        ZonedDateTime scheduledStart = attendanceDate.atTime(shift.startTime()).atZone(zone);
        LocalDate scheduledEndDate = !shift.endTime().isAfter(shift.startTime())
                ? attendanceDate.plusDays(1) : attendanceDate;
        ZonedDateTime scheduledEnd = scheduledEndDate.atTime(shift.endTime()).atZone(zone);
        boolean late = checkIn.isAfter(scheduledStart.plusMinutes(shift.graceInMinutes()).toInstant());
        boolean early = checkOut.isBefore(scheduledEnd.minusMinutes(shift.graceOutMinutes()).toInstant());

        String status;
        if (worked >= shift.fullDayMinutes()) status = "PRESENT";
        else if (worked >= shift.halfDayMinutes()) status = "HALF_DAY";
        else status = "ABSENT";
        int overtime = Math.max(0, worked - shift.fullDayMinutes());
        return new AttendanceEvaluation(status, worked, overtime, late, early);
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
