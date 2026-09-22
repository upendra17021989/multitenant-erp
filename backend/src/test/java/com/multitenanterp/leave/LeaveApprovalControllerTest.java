package com.multitenanterp.leave;

import com.multitenanterp.employee.EmployeeUserLinkService;
import com.multitenanterp.platform.security.TenantAuthorization;
import com.multitenanterp.platform.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class LeaveApprovalControllerTest {
    @Configuration @EnableMethodSecurity
    static class Config {
        @Bean LeaveApprovalService service(){return mock(LeaveApprovalService.class);}
        @Bean LeaveApprovalController controller(LeaveApprovalService service){return new LeaveApprovalController(service);}
        @Bean TenantAuthorization tenantAuthorization(){return new TenantAuthorization(mock(EmployeeUserLinkService.class));}
    }
    @Test void onlyAuthorizedAdministratorsCanChangeCompanyApprovalPolicy(){
        try(var context=new AnnotationConfigApplicationContext(Config.class)){
            var controller=context.getBean(LeaveApprovalController.class);var service=context.getBean(LeaveApprovalService.class);
            var actor=new TestingAuthenticationToken(UUID.randomUUID().toString(),"","ROLE_USER");
            SecurityContextHolder.getContext().setAuthentication(actor);
            var policy=new LeaveApprovalController.Policy(List.of("REPORTING_MANAGER","HR_MANAGER"));
            for(String role:List.of("EMPLOYEE","HR_EXECUTIVE","PAYROLL_MANAGER")){
                TenantContext.set(UUID.randomUUID(),UUID.randomUUID(),Set.of(role));
                assertThatThrownBy(()->controller.configure(policy,actor)).isInstanceOf(AccessDeniedException.class);
            }
            verifyNoInteractions(service);
            TenantContext.set(UUID.randomUUID(),UUID.randomUUID(),Set.of("HR_MANAGER"));
            controller.configure(policy,actor);
            verify(service).configure(policy.stages(),actor.getName());
        }finally{TenantContext.clear();SecurityContextHolder.clearContext();}
    }
}
