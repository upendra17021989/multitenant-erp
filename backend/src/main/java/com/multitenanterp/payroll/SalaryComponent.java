package com.multitenanterp.payroll;
import java.math.BigDecimal;import java.time.LocalDate;import java.util.UUID;
public record SalaryComponent(UUID id,String code,String name,String componentType,String calculationType,String calculationBaseCode,BigDecimal defaultValue,boolean taxable,boolean pfApplicable,boolean esiApplicable,boolean prorated,boolean includedInGross,boolean includedInCtc,boolean includedInNet,int roundingScale,LocalDate effectiveFrom,LocalDate effectiveTo,String status){}
