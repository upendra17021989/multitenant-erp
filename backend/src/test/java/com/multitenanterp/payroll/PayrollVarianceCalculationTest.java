package com.multitenanterp.payroll;
import org.junit.jupiter.api.Test;import java.lang.reflect.Method;import java.math.BigDecimal;import java.util.UUID;import static org.assertj.core.api.Assertions.*;
class PayrollVarianceCalculationTest{
 @Test void calculatesAmountAndPercentageChanges()throws Exception{PayrollVariance v=compare(new BigDecimal("11000"),new BigDecimal("10000"),new BigDecimal("8800"),new BigDecimal("8000"),"EXISTING");assertThat(v.grossChange()).isEqualByComparingTo("1000");assertThat(v.grossChangePercent()).isEqualByComparingTo("10");assertThat(v.netChangePercent()).isEqualByComparingTo("10");}
 @Test void leavesPercentageEmptyWithoutPreviousPayroll()throws Exception{PayrollVariance v=compare(new BigDecimal("10000"),BigDecimal.ZERO,new BigDecimal("8000"),BigDecimal.ZERO,"NEW");assertThat(v.grossChangePercent()).isNull();assertThat(v.comparisonStatus()).isEqualTo("NEW");}
 private static PayrollVariance compare(BigDecimal currentGross,BigDecimal previousGross,BigDecimal currentNet,BigDecimal previousNet,String status)throws Exception{Method method=PayrollCalculationService.class.getDeclaredMethod("variance",UUID.class,BigDecimal.class,BigDecimal.class,BigDecimal.class,BigDecimal.class,String.class);method.setAccessible(true);return (PayrollVariance)method.invoke(null,UUID.randomUUID(),currentGross,previousGross,currentNet,previousNet,status);}
}
