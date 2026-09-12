package com.multitenanterp.payroll;
import java.time.Instant;import java.util.UUID;
public record PayrollException(UUID id,UUID employmentId,String code,String severity,String message,boolean resolved,String resolutionComment,String resolvedBy,Instant resolvedAt){}
