package com.multitenanterp.platform.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/me")
public class CurrentUserController {
    private final TenantAccessService tenantAccessService;

    public CurrentUserController(TenantAccessService tenantAccessService) {
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping
    CurrentUserResponse currentUser(@AuthenticationPrincipal Jwt jwt) {
        return new CurrentUserResponse(UUID.fromString(jwt.getSubject()), jwt.getClaimAsString("email"));
    }

    @GetMapping("/tenants")
    List<TenantMembership> tenants(@AuthenticationPrincipal Jwt jwt) {
        return tenantAccessService.memberships(UUID.fromString(jwt.getSubject()));
    }

    record CurrentUserResponse(UUID id, String email) {}
}
