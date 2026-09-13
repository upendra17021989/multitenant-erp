package com.multitenanterp.payroll;
import java.time.Instant;import java.util.*;
public record PayrollRun(UUID id,int year,int month,String status,Instant calculatedAt,String calculatedBy,Instant reviewStartedAt,String reviewStartedBy,Instant approvedAt,String approvedBy,Instant lockedAt,String lockedBy,Instant reversedAt,String reversedBy,String reversalReason,List<PayrollEmployeeResult> employees,List<PayrollException> exceptions,List<PayrollRunTransition> transitions){}
