package com.multitenanterp.employee;

import java.util.UUID;

public record EmployeeImportRowResult(int rowNumber, boolean accepted, UUID employeeId, String employeeNumber,
                                      String error, boolean skipped, String message) {}
