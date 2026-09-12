package com.multitenanterp.payroll;
import java.math.BigDecimal;import java.time.Instant;import java.util.UUID;
public record PayrollVariableInput(UUID id,UUID employmentId,int year,int month,String code,String name,String inputType,BigDecimal amount,String notes,String createdBy,Instant createdAt){}
