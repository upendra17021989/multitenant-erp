package com.multitenanterp.payroll;
import jakarta.validation.constraints.NotBlank;import jakarta.validation.constraints.Size;
public record PayrollActionRequest(@NotBlank @Size(max=1000) String reason){}
