package com.multitenanterp.organization;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/organization")
public class OrganizationController {
    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping("/company")
    public CompanyProfile companyProfile() {
        return organizationService.companyProfile();
    }

    @PutMapping("/company")
    @PreAuthorize("@tenantAuthorization.hasAnyRole('SYSTEM_ADMIN', 'GROUP_ADMIN', 'COMPANY_ADMIN')")
    public CompanyProfile updateCompanyProfile(@Valid @RequestBody UpdateCompanyProfileRequest request) {
        return organizationService.updateCompanyProfile(request);
    }

    @GetMapping("/branches")
    public List<Branch> branches() {
        return organizationService.branches();
    }

    @PostMapping("/branches")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@tenantAuthorization.hasAnyRole('SYSTEM_ADMIN', 'GROUP_ADMIN', 'COMPANY_ADMIN', 'HR_MANAGER')")
    public Branch createBranch(@Valid @RequestBody SaveBranchRequest request) {
        return organizationService.createBranch(request);
    }

    @PutMapping("/branches/{id}")
    @PreAuthorize("@tenantAuthorization.hasAnyRole('SYSTEM_ADMIN', 'GROUP_ADMIN', 'COMPANY_ADMIN', 'HR_MANAGER')")
    public Branch updateBranch(@PathVariable UUID id, @Valid @RequestBody SaveBranchRequest request) {
        return organizationService.updateBranch(id, request);
    }
}
