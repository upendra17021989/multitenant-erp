package com.multitenanterp.employee;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/employees")
@PreAuthorize("@tenantAuthorization.hasAnyRole('SYSTEM_ADMIN','GROUP_ADMIN','COMPANY_ADMIN','HR_MANAGER','PAYROLL_MANAGER')")
public class EmployeeController {
    private static final String CAN_EDIT = "@tenantAuthorization.hasAnyRole('SYSTEM_ADMIN','GROUP_ADMIN','COMPANY_ADMIN','HR_MANAGER')";
    private final EmployeeService service;

    public EmployeeController(EmployeeService service) { this.service = service; }

    @GetMapping public List<Employee> employees(@RequestParam(required=false) String status) { return service.employees(status); }
    @GetMapping("/{id}") public Employee employee(@PathVariable UUID id) { return service.employee(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize(CAN_EDIT)
    public Employee create(@Valid @RequestBody SaveEmployeeRequest request) { return service.create(request); }
    @PutMapping("/{id}") @PreAuthorize(CAN_EDIT)
    public Employee update(@PathVariable UUID id, @Valid @RequestBody SaveEmployeeRequest request) { return service.update(id, request); }
}
