package com.multitenanterp.platform.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantContextTest {
    @AfterEach void clearContext() { TenantContext.clear(); }

    @Test void storesAndReturnsActiveTenant() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.set(tenantId);
        assertThat(TenantContext.requireTenantId()).isEqualTo(tenantId);
    }

    @Test void failsClosedWhenTenantIsMissing() {
        assertThatThrownBy(TenantContext::requireTenantId).isInstanceOf(MissingTenantException.class);
    }

    @Test void storesAuthorizedTenantRoles() {
        TenantContext.set(UUID.randomUUID(), UUID.randomUUID(), Set.of("PAYROLL_MANAGER"));
        assertThat(TenantContext.roles()).containsExactly("PAYROLL_MANAGER");
    }
}
