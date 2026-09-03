package com.multitenanterp.organization;

import java.util.UUID;

public record Department(UUID id, String code, String name, UUID parentDepartmentId, String status) {
}
