package com.multitenanterp.payroll;
import jakarta.validation.constraints.*;
public record PayrollCalculationRequest(@Min(2000) @Max(2200) int year,@Min(1) @Max(12) int month){}
