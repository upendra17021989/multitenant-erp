package com.multitenanterp.attendance;

import java.util.List;

public record AttendanceImportResult(int totalRows, int acceptedRows, int rejectedRows,
                                     List<AttendanceImportRowResult> rows) {}
