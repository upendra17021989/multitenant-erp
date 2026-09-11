package com.multitenanterp.payroll;
import jakarta.validation.constraints.*;import java.math.BigDecimal;import java.time.LocalDate;import java.util.UUID;
public record SaveEmployeeSalaryAssignmentRequest(@NotNull UUID employmentId,@NotNull UUID salaryStructureId,@NotNull LocalDate effectiveFrom,LocalDate effectiveTo,@NotNull @DecimalMin("0") BigDecimal annualCtc,@NotBlank @Size(max=500) String revisionReason){}
