package com.multitenanterp.platform.security;

import com.multitenanterp.platform.tenant.TenantContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component("tenantAuthorization")
public class TenantAuthorization {
    public boolean hasAnyRole(String... roles) {
        return Arrays.stream(roles).anyMatch(TenantContext.roles()::contains);
    }
}
