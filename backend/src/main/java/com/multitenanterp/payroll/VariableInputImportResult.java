package com.multitenanterp.payroll;
import java.util.List;
public record VariableInputImportResult(int totalRows,int acceptedRows,int rejectedRows,List<VariableInputImportRowResult> rows){}
