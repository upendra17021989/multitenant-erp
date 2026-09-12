package com.multitenanterp.payroll;
import jakarta.validation.constraints.*;import java.math.BigDecimal;import java.time.LocalDate;import java.util.List;
public record SaveStatutoryRuleRequest(@NotBlank String code,@NotBlank String name,@NotBlank String resultType,@NotBlank String basisType,List<String> baseComponentCodes,@NotNull @DecimalMin("0") BigDecimal ratePercent,@DecimalMin("0") BigDecimal eligibilityCeiling,@DecimalMin("0") BigDecimal contributionCeiling,@Min(0) @Max(4) int roundingScale,@NotNull LocalDate effectiveFrom,LocalDate effectiveTo,String status){}
