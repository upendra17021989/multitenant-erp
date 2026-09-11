package com.multitenanterp.payroll;
import jakarta.validation.constraints.*;
public record SavePayrollSettingRequest(@NotBlank String payFrequency,@Min(1) @Max(31) int attendanceCutoffDay,@Min(1) @Max(31) int paymentDay,@NotBlank String workingDayBasis,@Min(0) @Max(4) int roundingScale,@NotBlank String negativeSalaryPolicy){}
