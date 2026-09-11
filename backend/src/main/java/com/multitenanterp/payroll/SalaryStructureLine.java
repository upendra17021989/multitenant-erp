package com.multitenanterp.payroll;
import java.math.BigDecimal;import java.util.UUID;
public record SalaryStructureLine(UUID id,UUID salaryComponentId,int sequenceNumber,BigDecimal value,String formulaExpression){}
