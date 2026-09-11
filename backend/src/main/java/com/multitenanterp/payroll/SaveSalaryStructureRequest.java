package com.multitenanterp.payroll;
import jakarta.validation.Valid;import jakarta.validation.constraints.*;import java.time.LocalDate;import java.util.List;
public record SaveSalaryStructureRequest(@NotBlank String code,@NotBlank String name,String description,@NotNull LocalDate effectiveFrom,LocalDate effectiveTo,String status,@NotEmpty List<@Valid SaveSalaryStructureLineRequest> components){}
