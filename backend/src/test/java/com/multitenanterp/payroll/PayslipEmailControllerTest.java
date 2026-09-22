package com.multitenanterp.payroll;

import com.multitenanterp.employee.EmployeeUserLinkService;
import com.multitenanterp.platform.security.TenantAuthorization;
import com.multitenanterp.platform.tenant.TenantContext;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PayslipEmailControllerTest {
    @Configuration @EnableMethodSecurity
    static class Config {
        @Bean PayslipEmailService service() { return mock(PayslipEmailService.class); }
        @Bean PayslipEmailController controller(PayslipEmailService service) { return new PayslipEmailController(service); }
        @Bean TenantAuthorization tenantAuthorization() { return new TenantAuthorization(mock(EmployeeUserLinkService.class)); }
    }
    @Test void managerCanSendExecutiveCanReadAndEmployeeCannotAccess() {
        try(var context=new AnnotationConfigApplicationContext(Config.class)) {
            var controller=context.getBean(PayslipEmailController.class);
            var service=context.getBean(PayslipEmailService.class);
            var auth=new TestingAuthenticationToken("server-authenticated-user", "", "ROLE_USER");
            SecurityContextHolder.getContext().setAuthentication(auth);
            UUID tenant=UUID.randomUUID(), slip=UUID.randomUUID(), request=UUID.randomUUID();
            TenantContext.set(UUID.randomUUID(),tenant,Set.of("EMPLOYEE"));
            assertThatThrownBy(()->controller.history(2026,9,slip)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(()->controller.send(2026,9,slip,new PayslipEmailController.SendRequest(request),auth)).isInstanceOf(AccessDeniedException.class);
            TenantContext.set(UUID.randomUUID(),tenant,Set.of("PAYROLL_EXECUTIVE"));
            controller.history(2026,9,slip);
            assertThatThrownBy(()->controller.send(2026,9,slip,new PayslipEmailController.SendRequest(request),auth)).isInstanceOf(AccessDeniedException.class);
            TenantContext.set(UUID.randomUUID(),tenant,Set.of("PAYROLL_MANAGER"));
            controller.send(2026,9,slip,new PayslipEmailController.SendRequest(request),auth);
            verify(service).send(2026,9,slip,request,"server-authenticated-user");
        } finally { TenantContext.clear(); SecurityContextHolder.clearContext(); }
    }
}
