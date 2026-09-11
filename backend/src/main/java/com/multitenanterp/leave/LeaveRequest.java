package com.multitenanterp.leave;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
public record LeaveRequest(UUID id,UUID employmentId,UUID leaveTypeId,LocalDate startDate,LocalDate endDate,
 BigDecimal requestedDays,String reason,String status,String requestedBy,Instant requestedAt,String decidedBy,Instant decidedAt,String decisionComment) {}
