package com.multitenanterp.leave;

import com.multitenanterp.employee.EmployeeUserLinkService;
import com.multitenanterp.platform.security.TenantAuthorization;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class LeaveControllerTest {
    private final LeaveService service = mock(LeaveService.class);
    private final EmployeeUserLinkService links = mock(EmployeeUserLinkService.class);
    private final TenantAuthorization authorization = mock(TenantAuthorization.class);
    private final LeaveController controller = new LeaveController(service, links, authorization);
    @Test void employeeQueriesAlwaysUseLinkedEmployment() {
        UUID own = UUID.randomUUID(); when(links.currentEmploymentId()).thenReturn(own);
        controller.balances(null, 2026); controller.requests(null, "PENDING");
        verify(service).balances(own, 2026); verify(service).requests(own, "PENDING");
    }
    @Test void rejectsOtherEmployeesInQueriesAndPayloads() {
        when(links.currentEmploymentId()).thenReturn(UUID.randomUUID());
        UUID other = UUID.randomUUID();
        assertThatThrownBy(() -> controller.balances(other, 2026)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> controller.requests(other, null)).isInstanceOf(ResponseStatusException.class);
        var request = new CreateLeaveRequest(other, UUID.randomUUID(), LocalDate.now(), LocalDate.now(), BigDecimal.ONE, "Personal");
        assertThatThrownBy(() -> controller.request(request, new TestingAuthenticationToken("user", ""))).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(service);
    }
    @Test void cancellationPassesOwnershipScope() {
        UUID own = UUID.randomUUID(), request = UUID.randomUUID(); when(links.currentEmploymentId()).thenReturn(own);
        controller.cancel(request, new TestingAuthenticationToken("user", ""));
        verify(service).cancelOwned(request, "user", own);
    }
}
