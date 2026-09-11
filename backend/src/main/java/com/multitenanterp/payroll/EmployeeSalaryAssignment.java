package com.multitenanterp.payroll;
import java.math.BigDecimal;import java.time.*;import java.util.UUID;
public record EmployeeSalaryAssignment(UUID id,UUID employmentId,UUID salaryStructureId,LocalDate effectiveFrom,LocalDate effectiveTo,BigDecimal annualCtc,String revisionReason,String createdBy,Instant createdAt){}
