package com.multitenanterp.platform.security;

import java.util.Set;
import java.util.UUID;

public record TenantMembership(UUID tenantId, String companyCode, String legalName, Set<String> roles) {
    public TenantMembership {
        roles = Set.copyOf(roles);
    }
}
