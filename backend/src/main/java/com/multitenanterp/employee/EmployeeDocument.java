package com.multitenanterp.employee;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EmployeeDocument(UUID id, UUID employmentId, String documentType, String fileName,
                               String contentType, long sizeBytes, OffsetDateTime uploadedAt) {
}
