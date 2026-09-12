package com.multitenanterp.payroll;
import jakarta.validation.constraints.*;import java.math.BigDecimal;import java.util.UUID;
public record SavePayrollVariableInputRequest(@NotNull UUID employmentId,@Min(2000) @Max(2200) int year,@Min(1) @Max(12) int month,@NotBlank String code,@NotBlank String name,@NotBlank String inputType,@NotNull @DecimalMin("0") BigDecimal amount,@Size(max=500) String notes){}
