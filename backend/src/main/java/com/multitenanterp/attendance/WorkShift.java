package com.multitenanterp.attendance;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record WorkShift(UUID id, String code, String name, LocalTime startTime, LocalTime endTime,
                        int breakMinutes, int graceInMinutes, int graceOutMinutes, int fullDayMinutes,
                        int halfDayMinutes, LocalDate effectiveFrom, LocalDate effectiveTo, String status) {}
