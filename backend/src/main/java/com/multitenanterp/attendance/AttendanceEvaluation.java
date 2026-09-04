package com.multitenanterp.attendance;

public record AttendanceEvaluation(String status, int workedMinutes, int overtimeMinutes,
                                   boolean lateArrival, boolean earlyDeparture) {}
