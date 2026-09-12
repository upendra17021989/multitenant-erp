package com.multitenanterp.payroll;
import java.math.BigDecimal;import java.time.LocalDate;import java.util.*;
public record StatutoryRule(UUID id,String code,String name,String resultType,String basisType,List<String> baseComponentCodes,BigDecimal ratePercent,BigDecimal eligibilityCeiling,BigDecimal contributionCeiling,int roundingScale,LocalDate effectiveFrom,LocalDate effectiveTo,String status){}
