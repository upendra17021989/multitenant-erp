package com.multitenanterp.employee;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/employees/user-links")
@PreAuthorize("@tenantAuthorization.hasAnyRole('SYSTEM_ADMIN','GROUP_ADMIN','COMPANY_ADMIN','HR_MANAGER')")
public class EmployeeUserLinkController {
    private final EmployeeUserLinkService service;
    public EmployeeUserLinkController(EmployeeUserLinkService service){this.service=service;}
    @GetMapping public List<EmployeeUserLink> links(){return service.links();}
    @PutMapping("/{employmentId}") public EmployeeUserLink link(@PathVariable UUID employmentId,@Valid @RequestBody LinkEmployeeUserRequest request,Authentication authentication){return service.link(employmentId,request.email(),authentication.getName());}
    @DeleteMapping("/{employmentId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void unlink(@PathVariable UUID employmentId){service.unlink(employmentId);}
}

