package com.multitenanterp.employee;

import java.util.List;

public record EmployeeImportResult(int totalRows, int acceptedRows, int rejectedRows,
                                   List<EmployeeImportRowResult> rows) {}

