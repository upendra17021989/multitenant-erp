package com.multitenanterp.attendance;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record SaveWorkShiftRequest(@NotBlank String code, @NotBlank String name,
        @NotNull LocalTime startTime, @NotNull LocalTime endTime,
        @Min(0) int breakMinutes, @Min(0) int graceInMinutes, @Min(0) int graceOutMinutes,
        @Min(1) int fullDayMinutes, @Min(1) int halfDayMinutes,
        @NotNull LocalDate effectiveFrom, LocalDate effectiveTo, String status) {}
