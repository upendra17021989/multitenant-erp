package com.multitenanterp.payroll;
import java.time.Instant;import java.util.UUID;
public record PayrollRunTransition(UUID id,String fromStatus,String toStatus,String reason,String actedBy,Instant actedAt){}
