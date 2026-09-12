package com.multitenanterp.payroll;
import java.math.BigDecimal;import java.util.UUID;
public record PayrollComponentResult(UUID id,UUID salaryComponentId,String code,String name,String componentType,BigDecimal amount,int sequenceNumber){}
