package com.multitenanterp.organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SaveDepartmentRequest(
        @NotBlank @Size(max = 40) @Pattern(regexp = "[A-Za-z0-9_-]+") String code,
        @NotBlank @Size(max = 160) String name,
        UUID parentDepartmentId,
        @Pattern(regexp = "ACTIVE|INACTIVE") String status) {
}
