package com.multitenanterp.employee;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/hr-workflows")
@PreAuthorize("@tenantAuthorization.hasAnyRole('SYSTEM_ADMIN','GROUP_ADMIN','COMPANY_ADMIN','HR_MANAGER','PAYROLL_MANAGER')")
public class HrWorkflowController {
    private final HrWorkflowService service;
    public HrWorkflowController(HrWorkflowService service){this.service=service;}
    @GetMapping public List<HrWorkflowService.Workflow> list(){return service.list();}
    @PostMapping public HrWorkflowService.Workflow create(@Valid @RequestBody HrWorkflowService.Create request,Authentication actor){return service.create(request,actor.getName());}
    @PutMapping("/{id}/tasks/{index}") public HrWorkflowService.Workflow task(@PathVariable UUID id,@PathVariable int index,@Valid @RequestBody HrWorkflowService.TaskUpdate request,Authentication actor){return service.task(id,index,request,actor.getName());}
    @PostMapping("/{id}/transition") public HrWorkflowService.Workflow transition(@PathVariable UUID id,@Valid @RequestBody HrWorkflowService.Transition request,Authentication actor){return service.transition(id,request,actor.getName());}
    @GetMapping("/{id}/history") public List<HrWorkflowService.Event> history(@PathVariable UUID id){return service.history(id);}
}
