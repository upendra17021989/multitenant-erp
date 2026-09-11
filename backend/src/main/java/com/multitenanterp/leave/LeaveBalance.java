package com.multitenanterp.leave;
import java.math.BigDecimal;
import java.util.UUID;
public record LeaveBalance(UUID id,UUID employmentId,UUID leaveTypeId,int leaveYear,BigDecimal openingBalance,
                           BigDecimal accrued,BigDecimal adjusted,BigDecimal used,BigDecimal available) {}
