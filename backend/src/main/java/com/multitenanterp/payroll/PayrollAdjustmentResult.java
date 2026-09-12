package com.multitenanterp.payroll;
import java.math.BigDecimal;import java.util.UUID;
public record PayrollAdjustmentResult(UUID id,String sourceType,UUID sourceId,String code,String name,String resultType,BigDecimal amount){}
