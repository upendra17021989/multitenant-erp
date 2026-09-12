package com.multitenanterp.payroll;
import java.math.BigDecimal;import java.util.UUID;
public record PayrollVariance(UUID employmentId,BigDecimal currentGross,BigDecimal previousGross,BigDecimal grossChange,BigDecimal grossChangePercent,BigDecimal currentNet,BigDecimal previousNet,BigDecimal netChange,BigDecimal netChangePercent,String comparisonStatus){}
