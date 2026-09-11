package com.multitenanterp.payroll;
import java.util.UUID;
public record PayrollSetting(UUID tenantId,String payFrequency,int attendanceCutoffDay,int paymentDay,String workingDayBasis,int roundingScale,String negativeSalaryPolicy){}
