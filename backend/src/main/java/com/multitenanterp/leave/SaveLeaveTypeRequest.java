package com.multitenanterp.leave;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record SaveLeaveTypeRequest(@NotBlank String code,@NotBlank String name,boolean paid,
 @NotNull @DecimalMin("0") BigDecimal annualEntitlement,@NotBlank String accrualFrequency,
 @NotNull @DecimalMin(value="0",inclusive=false) BigDecimal minimumDays,@DecimalMin(value="0",inclusive=false) BigDecimal maximumDays,
 boolean halfDayAllowed,@NotNull @DecimalMin("0") BigDecimal carryForwardLimit,boolean negativeBalanceAllowed,
 boolean supportingDocumentRequired,String status) {}
