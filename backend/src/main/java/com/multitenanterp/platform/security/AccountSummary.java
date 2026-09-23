package com.multitenanterp.platform.security;

import java.util.Set;
import java.util.UUID;

public record AccountSummary(UUID id,UUID authUserId,String email,String displayName,String status,
                             Set<String> roles,UUID employmentId,String employeeNumber,String employeeName) {}
