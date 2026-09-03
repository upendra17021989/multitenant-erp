package com.multitenanterp.organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record SaveHolidayRequest(
        UUID branchId,
        @NotNull LocalDate holidayDate,
        @NotBlank @Size(max = 160) String name,
        boolean optional) {
}
