package com.multitenanterp.organization;

import java.util.UUID;

public record Designation(UUID id, String code, String title, UUID gradeId, String status) {
}
