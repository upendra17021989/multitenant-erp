package com.multitenanterp.platform.security;

import com.multitenanterp.platform.tenant.TenantContext;
import com.multitenanterp.employee.EmployeeUserLinkService;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component("tenantAuthorization")
public class TenantAuthorization {
    private final EmployeeUserLinkService employeeUserLinks;
    public TenantAuthorization(EmployeeUserLinkService employeeUserLinks){this.employeeUserLinks=employeeUserLinks;}
    public boolean hasAnyRole(String... roles) {
        return Arrays.stream(roles).anyMatch(TenantContext.roles()::contains);
    }

    public boolean isCurrentEmployee(java.util.UUID employmentId){return employeeUserLinks.isCurrentEmployee(employmentId);}
}
