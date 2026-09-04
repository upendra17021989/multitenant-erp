package com.multitenanterp.attendance;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SaveAttendanceRequest(@NotNull UUID employmentId, @NotNull LocalDate attendanceDate,
        UUID shiftId, @NotBlank String status, Instant checkIn, Instant checkOut,
        @Min(0) Integer workedMinutes, @Min(0) int overtimeMinutes, String source, String notes) {}
