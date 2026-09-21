package com.multitenanterp.employee;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/self")
public class EmployeeSelfServiceController {
    private final EmployeeUserLinkService links;
    public EmployeeSelfServiceController(EmployeeUserLinkService links) { this.links = links; }
    @GetMapping("/employment")
    public Map<String, UUID> employment() { return Map.of("employmentId", links.currentEmploymentId()); }
}
