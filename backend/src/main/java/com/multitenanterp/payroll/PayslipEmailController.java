package com.multitenanterp.payroll;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/payroll/runs/{year}/{month}/payslips")
@PreAuthorize("@tenantAuthorization.hasAnyRole('SYSTEM_ADMIN','GROUP_ADMIN','COMPANY_ADMIN','PAYROLL_MANAGER','PAYROLL_EXECUTIVE')")
public class PayslipEmailController {
    private final PayslipEmailService service;
    public PayslipEmailController(PayslipEmailService service) { this.service = service; }
    public record SendRequest(@NotNull UUID requestId) {}
    @GetMapping("/email-settings")
    public Map<String,Boolean> settings() { return Map.of("enabled", service.enabled()); }
    @GetMapping("/{payslipId}/email-attempts")
    public List<PayslipEmailService.Attempt> history(@PathVariable int year, @PathVariable int month, @PathVariable UUID payslipId) {
        return service.history(year, month, payslipId);
    }
    @PostMapping("/{payslipId}/email")
    @PreAuthorize("@tenantAuthorization.hasAnyRole('SYSTEM_ADMIN','GROUP_ADMIN','COMPANY_ADMIN','PAYROLL_MANAGER')")
    public PayslipEmailService.Attempt send(@PathVariable int year, @PathVariable int month, @PathVariable UUID payslipId,
            @Valid @RequestBody SendRequest request, Authentication authentication) {
        return service.send(year, month, payslipId, request.requestId(), authentication.getName());
    }
}
