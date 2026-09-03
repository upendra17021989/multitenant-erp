package com.multitenanterp.platform.tenant;

import com.multitenanterp.platform.security.TenantAccessService;
import com.multitenanterp.platform.security.TenantMembership;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantContextFilterTest {
    private TenantAccessService tenantAccessService;
    private TenantContextFilter filter;

    @BeforeEach
    void setUp() {
        tenantAccessService = mock(TenantAccessService.class);
        filter = new TenantContextFilter(tenantAccessService);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void authorizesAssignedTenantAndExposesRolesDuringRequest() throws Exception {
        UUID authUserId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        authenticate(authUserId);
        when(tenantAccessService.membership(authUserId, tenantId)).thenReturn(Optional.of(
                new TenantMembership(tenantId, "COMPANY1", "Company One", Set.of("HR_MANAGER"))));

        MockHttpServletRequest request = requestFor(tenantId);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean();
        FilterChain chain = (servletRequest, servletResponse) -> {
            invoked.set(true);
            assertThat(TenantContext.requireTenantId()).isEqualTo(tenantId);
            assertThat(TenantContext.roles()).containsExactly("HR_MANAGER");
        };

        filter.doFilter(request, response, chain);

        assertThat(invoked).isTrue();
        assertThat(TenantContext.currentTenantId()).isEmpty();
    }

    @Test
    void rejectsTenantNotAssignedToAuthenticatedUser() throws Exception {
        UUID authUserId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        authenticate(authUserId);
        when(tenantAccessService.membership(authUserId, tenantId)).thenReturn(Optional.empty());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(requestFor(tenantId), response, (request, result) -> {});

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(TenantContext.currentTenantId()).isEmpty();
    }

    @Test
    void rejectsProtectedRequestWithoutTenantHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/employees");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {});

        assertThat(response.getStatus()).isEqualTo(400);
    }

    private void authenticate(UUID authUserId) {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(authUserId.toString(), null, "ROLE_AUTHENTICATED"));
    }

    private MockHttpServletRequest requestFor(UUID tenantId) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/employees");
        request.addHeader(TenantContextFilter.TENANT_HEADER, tenantId.toString());
        return request;
    }
}
