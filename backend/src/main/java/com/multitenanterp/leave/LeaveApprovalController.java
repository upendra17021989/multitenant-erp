package com.multitenanterp.leave;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/leave")
public class LeaveApprovalController {
    private final LeaveApprovalService service;
    public LeaveApprovalController(LeaveApprovalService service){this.service=service;}
    public record Policy(List<String> stages) {}
    @GetMapping("/approval-policy") public Policy policy(){return new Policy(service.policy());}
    @PutMapping("/approval-policy")
    @PreAuthorize("@tenantAuthorization.hasAnyRole('SYSTEM_ADMIN','GROUP_ADMIN','COMPANY_ADMIN','HR_MANAGER')")
    public Policy configure(@RequestBody Policy policy,Authentication actor){return new Policy(service.configure(policy.stages(),actor.getName()));}
    @GetMapping("/approval-inbox") public List<LeaveApprovalService.Inbox> inbox(){return service.inbox();}
    @GetMapping("/requests/{id}/history") public List<LeaveApprovalService.Step> history(@PathVariable UUID id){return service.history(id);}
    @GetMapping("/notifications") public List<LeaveApprovalService.Notification> notifications(){return service.notifications();}
    @PostMapping("/notifications/{id}/read") public Map<String,Boolean> read(@PathVariable UUID id){service.markRead(id);return Map.of("read",true);}
}
