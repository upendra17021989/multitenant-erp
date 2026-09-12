package com.multitenanterp.payroll;
import java.math.BigDecimal;import java.util.*;
public record PayrollEmployeeResult(UUID id,UUID employmentId,UUID salaryAssignmentId,BigDecimal grossPay,BigDecimal deductions,BigDecimal employerContributions,BigDecimal netPay,BigDecimal ctc,List<PayrollComponentResult> components){}
