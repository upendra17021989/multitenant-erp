package com.multitenanterp.platform.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class TenantContextFilter extends OncePerRequestFilter {
    public static final String TENANT_HEADER = "X-Tenant-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getRequestURI().equals("/api/health")
                || HttpMethod.OPTIONS.matches(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        String value = request.getHeader(TENANT_HEADER);
        if (value == null || value.isBlank()) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Missing X-Tenant-Id header");
            return;
        }
        try {
            TenantContext.set(UUID.fromString(value));
            chain.doFilter(request, response);
        } catch (IllegalArgumentException exception) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Invalid X-Tenant-Id header");
        } finally {
            TenantContext.clear();
        }
    }
}
