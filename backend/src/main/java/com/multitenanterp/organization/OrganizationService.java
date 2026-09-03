package com.multitenanterp.organization;

import com.multitenanterp.platform.tenant.TenantContext;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class OrganizationService {
    private final JdbcClient jdbcClient;

    public OrganizationService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public CompanyProfile companyProfile() {
        UUID tenantId = TenantContext.requireTenantId();
        return jdbcClient.sql("""
                        SELECT t.code, t.legal_name, t.status, p.registered_address, p.pan, p.tan,
                               p.gstin, p.pf_registration, p.esi_registration, p.logo_path
                        FROM tenant t
                        LEFT JOIN company_profile p ON p.tenant_id = t.id
                        WHERE t.id = :tenantId
                        """)
                .param("tenantId", tenantId)
                .query((rs, rowNum) -> new CompanyProfile(
                        rs.getString("code"), rs.getString("legal_name"), rs.getString("status"),
                        rs.getString("registered_address"), rs.getString("pan"), rs.getString("tan"),
                        rs.getString("gstin"), rs.getString("pf_registration"),
                        rs.getString("esi_registration"), rs.getString("logo_path")))
                .optional()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
    }

    public CompanyProfile updateCompanyProfile(UpdateCompanyProfileRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        jdbcClient.sql("""
                        INSERT INTO company_profile
                            (tenant_id, registered_address, pan, tan, gstin, pf_registration, esi_registration, logo_path)
                        VALUES (:tenantId, :address, :pan, :tan, :gstin, :pf, :esi, :logo)
                        ON CONFLICT (tenant_id) DO UPDATE SET
                            registered_address = EXCLUDED.registered_address,
                            pan = EXCLUDED.pan, tan = EXCLUDED.tan, gstin = EXCLUDED.gstin,
                            pf_registration = EXCLUDED.pf_registration,
                            esi_registration = EXCLUDED.esi_registration,
                            logo_path = EXCLUDED.logo_path, updated_at = CURRENT_TIMESTAMP
                        """)
                .param("tenantId", tenantId)
                .param("address", blankToNull(request.registeredAddress()))
                .param("pan", normalized(request.pan()))
                .param("tan", normalized(request.tan()))
                .param("gstin", normalized(request.gstin()))
                .param("pf", blankToNull(request.pfRegistration()))
                .param("esi", blankToNull(request.esiRegistration()))
                .param("logo", blankToNull(request.logoPath()))
                .update();
        return companyProfile();
    }

    public List<Branch> branches() {
        return jdbcClient.sql("""
                        SELECT id, code, name, address, state_code, professional_tax_applicable,
                               labour_welfare_fund_applicable, status
                        FROM branch WHERE tenant_id = :tenantId ORDER BY name
                        """)
                .param("tenantId", TenantContext.requireTenantId())
                .query((rs, rowNum) -> mapBranch(rs))
                .list();
    }

    public Branch createBranch(SaveBranchRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID id = UUID.randomUUID();
        try {
            jdbcClient.sql("""
                            INSERT INTO branch
                                (id, tenant_id, code, name, address, state_code,
                                 professional_tax_applicable, labour_welfare_fund_applicable, status)
                            VALUES (:id, :tenantId, :code, :name, :address, :stateCode, :pt, :lwf, :status)
                            """)
                    .param("id", id).param("tenantId", tenantId)
                    .param("code", request.code().trim().toUpperCase())
                    .param("name", request.name().trim())
                    .param("address", blankToNull(request.address()))
                    .param("stateCode", normalized(request.stateCode()))
                    .param("pt", request.professionalTaxApplicable())
                    .param("lwf", request.labourWelfareFundApplicable())
                    .param("status", request.status() == null ? "ACTIVE" : request.status())
                    .update();
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Branch code already exists in this company");
        }
        return branch(id);
    }

    public Branch updateBranch(UUID id, SaveBranchRequest request) {
        int updated;
        try {
            updated = jdbcClient.sql("""
                            UPDATE branch SET code = :code, name = :name, address = :address,
                                state_code = :stateCode, professional_tax_applicable = :pt,
                                labour_welfare_fund_applicable = :lwf, status = :status,
                                updated_at = CURRENT_TIMESTAMP
                            WHERE id = :id AND tenant_id = :tenantId
                            """)
                    .param("id", id).param("tenantId", TenantContext.requireTenantId())
                    .param("code", request.code().trim().toUpperCase())
                    .param("name", request.name().trim())
                    .param("address", blankToNull(request.address()))
                    .param("stateCode", normalized(request.stateCode()))
                    .param("pt", request.professionalTaxApplicable())
                    .param("lwf", request.labourWelfareFundApplicable())
                    .param("status", request.status() == null ? "ACTIVE" : request.status())
                    .update();
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Branch code already exists in this company");
        }
        if (updated == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found");
        return branch(id);
    }

    private Branch branch(UUID id) {
        return jdbcClient.sql("""
                        SELECT id, code, name, address, state_code, professional_tax_applicable,
                               labour_welfare_fund_applicable, status
                        FROM branch WHERE id = :id AND tenant_id = :tenantId
                        """)
                .param("id", id).param("tenantId", TenantContext.requireTenantId())
                .query((rs, rowNum) -> mapBranch(rs)).optional()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    }

    private static Branch mapBranch(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Branch(rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("name"),
                rs.getString("address"), rs.getString("state_code"),
                rs.getBoolean("professional_tax_applicable"),
                rs.getBoolean("labour_welfare_fund_applicable"), rs.getString("status"));
    }

    private static String normalized(String value) {
        String clean = blankToNull(value);
        return clean == null ? null : clean.toUpperCase();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
