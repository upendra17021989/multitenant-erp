package com.multitenanterp.attendance;

import java.time.LocalDate;
import java.util.UUID;

public record ShiftAssignment(UUID id, UUID employmentId, UUID shiftId, LocalDate effectiveFrom, LocalDate effectiveTo) {}
