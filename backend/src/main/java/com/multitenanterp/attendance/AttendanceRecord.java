package com.multitenanterp.attendance;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AttendanceRecord(UUID id, UUID employmentId, LocalDate attendanceDate, UUID shiftId,
        String status, Instant checkIn, Instant checkOut, Integer workedMinutes, int overtimeMinutes,
        String source, String notes) {}
