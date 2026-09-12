package com.multitenanterp.payroll;
import java.math.BigDecimal;
public record PayrollProration(BigDecimal periodDays,BigDecimal eligibleDays,BigDecimal unpaidLeaveDays,BigDecimal absentDays,BigDecimal payableDays,BigDecimal factor,int overtimeMinutes){}
