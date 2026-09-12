package com.multitenanterp.payroll;
import java.util.UUID;
public record VariableInputImportRowResult(int rowNumber,boolean accepted,UUID variableInputId,String error){}
