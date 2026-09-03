package com.multitenanterp.platform.security;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class TenantAccessService {
    private final JdbcClient jdbcClient;

    public TenantAccessService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<TenantMembership> memberships(UUID authUserId) {
        List<MembershipRow> rows = jdbcClient.sql("""
                        SELECT t.id AS tenant_id, t.code, t.legal_name, utr.role_code
                        FROM app_user u
                        JOIN user_tenant_role utr ON utr.user_id = u.id AND utr.revoked_at IS NULL
                        JOIN tenant t ON t.id = utr.tenant_id AND t.status = 'ACTIVE'
                        WHERE u.auth_user_id = :authUserId AND u.status = 'ACTIVE'
                        ORDER BY t.legal_name, utr.role_code
                        """)
                .param("authUserId", authUserId)
                .query((resultSet, rowNumber) -> new MembershipRow(
                        resultSet.getObject("tenant_id", UUID.class),
                        resultSet.getString("code"),
                        resultSet.getString("legal_name"),
                        resultSet.getString("role_code")))
                .list();

        Map<UUID, MembershipBuilder> grouped = new LinkedHashMap<>();
        for (MembershipRow row : rows) {
            grouped.computeIfAbsent(row.tenantId(), ignored ->
                            new MembershipBuilder(row.tenantId(), row.companyCode(), row.legalName()))
                    .roles().add(row.roleCode());
        }
        return grouped.values().stream().map(MembershipBuilder::build).toList();
    }

    public Optional<TenantMembership> membership(UUID authUserId, UUID tenantId) {
        return memberships(authUserId).stream()
                .filter(membership -> membership.tenantId().equals(tenantId))
                .findFirst();
    }

    private record MembershipRow(UUID tenantId, String companyCode, String legalName, String roleCode) {}

    private record MembershipBuilder(UUID tenantId, String companyCode, String legalName, Set<String> roles) {
        private MembershipBuilder(UUID tenantId, String companyCode, String legalName) {
            this(tenantId, companyCode, legalName, new LinkedHashSet<>());
        }

        private TenantMembership build() {
            return new TenantMembership(tenantId, companyCode, legalName, roles);
        }
    }
}
