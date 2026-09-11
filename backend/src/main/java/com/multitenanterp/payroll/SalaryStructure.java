package com.multitenanterp.payroll;
import java.time.LocalDate;import java.util.*;
public record SalaryStructure(UUID id,String code,String name,String description,LocalDate effectiveFrom,LocalDate effectiveTo,String status,List<SalaryStructureLine> components){}
