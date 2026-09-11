package com.multitenanterp.leave;
import java.math.BigDecimal;
import java.util.UUID;
public record LeaveType(UUID id,String code,String name,boolean paid,BigDecimal annualEntitlement,String accrualFrequency,
                        BigDecimal minimumDays,BigDecimal maximumDays,boolean halfDayAllowed,BigDecimal carryForwardLimit,
                        boolean negativeBalanceAllowed,boolean supportingDocumentRequired,String status) {}
