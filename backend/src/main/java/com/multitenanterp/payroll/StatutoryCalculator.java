package com.multitenanterp.payroll;
import java.math.*;import java.util.*;
final class StatutoryCalculator{
 static BigDecimal calculate(StatutoryRule rule,BigDecimal gross,Map<String,BigDecimal> components){if(rule.eligibilityCeiling()!=null&&gross.compareTo(rule.eligibilityCeiling())>0)return BigDecimal.ZERO;BigDecimal base;if(rule.basisType().equals("GROSS"))base=gross;else{base=BigDecimal.ZERO;for(String code:rule.baseComponentCodes()){BigDecimal amount=components.get(code.toUpperCase(Locale.ROOT));if(amount==null)throw new IllegalArgumentException("Statutory rule "+rule.code()+" references unknown component "+code);base=base.add(amount);}}if(rule.contributionCeiling()!=null&&base.compareTo(rule.contributionCeiling())>0)base=rule.contributionCeiling();return base.multiply(rule.ratePercent()).divide(BigDecimal.valueOf(100),rule.roundingScale(),RoundingMode.HALF_UP);}
}
