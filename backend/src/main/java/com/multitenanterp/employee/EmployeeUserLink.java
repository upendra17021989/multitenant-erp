package com.multitenanterp.employee;

import java.time.Instant;
import java.util.UUID;

public record EmployeeUserLink(UUID id, UUID employmentId, String employeeNumber, String employeeName,
                               UUID userId, UUID authUserId, String userEmail, String linkedBy, Instant linkedAt) {}

