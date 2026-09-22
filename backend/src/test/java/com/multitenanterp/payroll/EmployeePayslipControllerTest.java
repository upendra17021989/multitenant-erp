package com.multitenanterp.payroll;

import com.multitenanterp.employee.EmployeeUserLinkService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmployeePayslipControllerTest {
    private final PayslipService service = mock(PayslipService.class);
    private final EmployeeUserLinkService links = mock(EmployeeUserLinkService.class);
    private final EmployeePayslipController controller = new EmployeePayslipController(service, links);

    @Test void acknowledgementUsesLinkedEmployeeAndAuthenticatedActor() {
        UUID employee = UUID.randomUUID(), slip = UUID.randomUUID();
        when(links.currentEmploymentId()).thenReturn(employee);
        controller.acknowledge(slip, new TestingAuthenticationToken("auth-subject", ""));
        verify(service).acknowledge(slip, employee, "auth-subject");
    }

    @Test void unlinkedUserCannotAcknowledge() {
        when(links.currentEmploymentId()).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> controller.acknowledge(UUID.randomUUID(),
                new TestingAuthenticationToken("auth-subject", ""))).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(service);
    }
}