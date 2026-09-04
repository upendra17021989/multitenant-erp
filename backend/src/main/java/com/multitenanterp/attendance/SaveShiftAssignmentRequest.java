package com.multitenanterp.attendance;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record SaveShiftAssignmentRequest(@NotNull UUID employmentId, @NotNull UUID shiftId,
                                         @NotNull LocalDate effectiveFrom, LocalDate effectiveTo) {}
