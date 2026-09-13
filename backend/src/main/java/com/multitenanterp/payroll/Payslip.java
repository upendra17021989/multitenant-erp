package com.multitenanterp.payroll;
import java.time.Instant;import java.util.UUID;
public record Payslip(UUID id,UUID payrollRunId,UUID payrollEmployeeResultId,UUID employmentId,String employeeNumber,String employeeName,int year,int month,String fileName,long sizeBytes,String sha256,String status,String generatedBy,Instant generatedAt,String releasedBy,Instant releasedAt){}
