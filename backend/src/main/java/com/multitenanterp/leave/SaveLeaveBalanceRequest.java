package com.multitenanterp.leave;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
public record SaveLeaveBalanceRequest(@NotNull UUID employmentId,@NotNull UUID leaveTypeId,int leaveYear,
                                      @NotNull BigDecimal openingBalance,@NotNull BigDecimal accrued,@NotNull BigDecimal adjusted) {}
