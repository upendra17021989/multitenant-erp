package com.multitenanterp.organization;

import java.time.LocalDate;
import java.util.UUID;

public record Holiday(UUID id, UUID branchId, LocalDate holidayDate, String name, boolean optional) {
}
