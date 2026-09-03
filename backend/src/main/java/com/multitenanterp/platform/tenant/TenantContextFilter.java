package com.multitenanterp.platform.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.multitenanterp.platform.security.TenantAccessService;

import java.io.IOException;
import java.util.UUID;

@Component
public class TenantContextFilter extends OncePerRequestFilter {
    public static final String TENANT_HEADER = "X-Tenant-Id";
    private final TenantAccessService tenantAccessService;

    public TenantContextFilter(TenantAccessService tenantAccessService) {
        this.tenantAccessService = tenantAccessService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getRequestURI().equals("/api/health")
                || request.getRequestURI().equals("/api/me")
                || request.getRequestURI().equals("/api/me/tenants")
                || HttpMethod.OPTIONS.matches(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        String value = request.getHeader(TENANT_HEADER);
        if (value == null || value.isBlank()) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Missing X-Tenant-Id header");
            return;
        }
        UUID tenantId;
        try {
            tenantId = UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Invalid X-Tenant-Id header");
            return;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Authentication required");
            return;
        }
        UUID authUserId;
        try {
            authUserId = UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException exception) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid authenticated user identifier");
            return;
        }
        var membership = tenantAccessService.membership(authUserId, tenantId);
        if (membership.isEmpty()) {
            response.sendError(HttpStatus.FORBIDDEN.value(), "User is not authorized for this tenant");
            return;
        }
        TenantContext.set(authUserId, tenantId, membership.get().roles());
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
