package com.multitenanterp.attendance;

import java.time.Instant;
import java.time.LocalDate;

public record AttendanceMonthLock(LocalDate attendanceMonth, String lockedBy, Instant lockedAt) {}
