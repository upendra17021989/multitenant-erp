package com.multitenanterp.payroll;
import org.junit.jupiter.api.Test;import java.math.BigDecimal;import java.time.LocalDate;import java.util.*;import static org.assertj.core.api.Assertions.*;
class StatutoryCalculatorTest{
 @Test void appliesRateToCappedComponentBase(){StatutoryRule rule=rule("COMPONENTS",List.of("BASIC"),null,new BigDecimal("15000"),new BigDecimal("12"),0);assertThat(StatutoryCalculator.calculate(rule,new BigDecimal("30000"),Map.of("BASIC",new BigDecimal("25000")))).isEqualByComparingTo("1800");}
 @Test void excludesEmployeeAboveEligibilityCeiling(){StatutoryRule rule=rule("GROSS",List.of(),new BigDecimal("21000"),null,new BigDecimal("0.75"),0);assertThat(StatutoryCalculator.calculate(rule,new BigDecimal("21001"),Map.of())).isZero();}
 @Test void calculatesEligibleGrossWithConfiguredRounding(){StatutoryRule rule=rule("GROSS",List.of(),new BigDecimal("21000"),null,new BigDecimal("0.75"),0);assertThat(StatutoryCalculator.calculate(rule,new BigDecimal("20001"),Map.of())).isEqualByComparingTo("150");}
 @Test void rejectsUnknownBaseComponent(){StatutoryRule rule=rule("COMPONENTS",List.of("BASIC"),null,null,new BigDecimal("12"),0);assertThatThrownBy(()->StatutoryCalculator.calculate(rule,BigDecimal.ZERO,Map.of())).hasMessageContaining("BASIC");}
 private StatutoryRule rule(String basis,List<String> codes,BigDecimal eligibility,BigDecimal cap,BigDecimal rate,int scale){return new StatutoryRule(UUID.randomUUID(),"RULE","Rule","DEDUCTION",basis,codes,rate,eligibility,cap,scale,LocalDate.of(2026,1,1),null,"ACTIVE");}
}
