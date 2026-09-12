package com.multitenanterp.payroll;
import jakarta.validation.constraints.NotBlank;
public record ResolvePayrollExceptionRequest(@NotBlank String comment){}
