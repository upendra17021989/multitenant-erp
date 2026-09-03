package com.multitenanterp.organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SaveBranchRequest(
        @NotBlank @Size(max = 40) @Pattern(regexp = "[A-Za-z0-9_-]+") String code,
        @NotBlank @Size(max = 160) String name,
        @Size(max = 4000) String address,
        @Pattern(regexp = "^$|[A-Z]{2}$", message = "must be a two-letter state code") String stateCode,
        boolean professionalTaxApplicable,
        boolean labourWelfareFundApplicable,
        @Pattern(regexp = "ACTIVE|INACTIVE") String status) {
}
