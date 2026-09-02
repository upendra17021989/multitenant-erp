package com.multitenanterp.platform.tenant;

import java.util.Optional;
import java.util.UUID;

public final class TenantContext {
    private static final ThreadLocal<UUID> ACTIVE_TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(UUID tenantId) { ACTIVE_TENANT.set(tenantId); }

    public static Optional<UUID> currentTenantId() { return Optional.ofNullable(ACTIVE_TENANT.get()); }

    public static UUID requireTenantId() {
        return currentTenantId().orElseThrow(() -> new MissingTenantException("No active tenant context"));
    }

    public static void clear() { ACTIVE_TENANT.remove(); }
}
