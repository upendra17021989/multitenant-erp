package com.multitenanterp.attendance;

import java.util.UUID;

public record AttendanceImportRowResult(int rowNumber, boolean accepted, UUID attendanceId, String error) {}
