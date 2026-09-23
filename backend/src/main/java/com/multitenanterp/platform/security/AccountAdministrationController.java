package com.multitenanterp.platform.security;

import com.multitenanterp.platform.tenant.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@PreAuthorize("@tenantAuthorization.hasAnyRole('SYSTEM_ADMIN','GROUP_ADMIN','COMPANY_ADMIN')")
public class AccountAdministrationController {
    private final AccountAdministrationService service;
    public AccountAdministrationController(AccountAdministrationService service){this.service=service;}
    @GetMapping public List<AccountSummary> accounts(){return service.accounts();}
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public AccountSummary invite(@Valid @RequestBody CreateAccountRequest request,Authentication authentication){return service.invite(request,authentication.getName());}
    @PutMapping("/{userId}/status") public AccountSummary status(@PathVariable UUID userId,@Valid @RequestBody StatusRequest request){
        return service.setStatus(userId,request.status(),TenantContext.requireAuthUserId());
    }
    public record StatusRequest(@NotBlank String status){}
}
