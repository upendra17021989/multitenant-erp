package com.multitenanterp.platform.tenant;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class TenantContext {
    private static final ThreadLocal<AuthorizedTenant> ACTIVE_TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(UUID tenantId) { ACTIVE_TENANT.set(new AuthorizedTenant(null, tenantId, Set.of())); }

    public static void set(UUID authUserId, UUID tenantId, Set<String> roles) {
        ACTIVE_TENANT.set(new AuthorizedTenant(authUserId, tenantId, roles));
    }

    public static Optional<UUID> currentTenantId() {
        return Optional.ofNullable(ACTIVE_TENANT.get()).map(AuthorizedTenant::tenantId);
    }

    public static UUID requireTenantId() {
        return currentTenantId().orElseThrow(() -> new MissingTenantException("No active tenant context"));
    }

    public static void clear() { ACTIVE_TENANT.remove(); }

    public static Set<String> roles() {
        return Optional.ofNullable(ACTIVE_TENANT.get()).map(AuthorizedTenant::roles).orElse(Set.of());
    }

    private record AuthorizedTenant(UUID authUserId, UUID tenantId, Set<String> roles) {
        private AuthorizedTenant {
            roles = Set.copyOf(roles);
        }
    }
}
