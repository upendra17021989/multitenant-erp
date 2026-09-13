package com.multitenanterp.payroll;
import org.springframework.core.io.Resource;
public record PayslipDownload(Payslip metadata,Resource resource){}
