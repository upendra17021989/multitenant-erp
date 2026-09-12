package com.multitenanterp.payroll;
import java.time.Instant;import java.util.*;
public record PayrollRun(UUID id,int year,int month,String status,Instant calculatedAt,String calculatedBy,List<PayrollEmployeeResult> employees){}
