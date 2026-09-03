package com.multitenanterp.organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SaveCostCentreRequest(
        @NotBlank @Size(max = 40) @Pattern(regexp = "[A-Za-z0-9_-]+") String code,
        @NotBlank @Size(max = 160) String name,
        @Size(max = 100) String accountingReference,
        @Pattern(regexp = "ACTIVE|INACTIVE") String status) {
}
