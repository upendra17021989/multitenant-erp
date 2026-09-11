package com.multitenanterp.payroll;
import jakarta.validation.constraints.*;import java.math.BigDecimal;import java.util.UUID;
public record SaveSalaryStructureLineRequest(@NotNull UUID salaryComponentId,@Min(1) int sequenceNumber,@DecimalMin("0") BigDecimal value,@Size(max=1000) String formulaExpression){}
