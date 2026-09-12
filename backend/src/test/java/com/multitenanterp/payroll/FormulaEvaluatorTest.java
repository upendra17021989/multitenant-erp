package com.multitenanterp.payroll;
import org.junit.jupiter.api.Test;import java.math.BigDecimal;import java.util.Map;import static org.assertj.core.api.Assertions.*;
class FormulaEvaluatorTest{
 @Test void evaluatesArithmeticAndComponentReferences(){BigDecimal result=FormulaEvaluator.evaluate("(BASIC + HRA) * 0.10",Map.of("BASIC",new BigDecimal("25000"),"HRA",new BigDecimal("10000")));assertThat(result).isEqualByComparingTo("3500");}
 @Test void extractsDependenciesCaseInsensitively(){assertThat(FormulaEvaluator.references("basic + MONTHLY_CTC - pf")).containsExactly("BASIC","MONTHLY_CTC","PF");}
 @Test void rejectsUnsafeOrInvalidSyntax(){assertThatThrownBy(()->FormulaEvaluator.evaluate("java.lang.Runtime",Map.of())).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->FormulaEvaluator.evaluate("10 / 0",Map.of())).hasMessageContaining("zero");}
}
