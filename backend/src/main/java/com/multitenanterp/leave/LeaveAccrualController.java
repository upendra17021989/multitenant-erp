package com.multitenanterp.leave;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;

@RestController @RequestMapping("/api/leave/accrual")
@PreAuthorize("@tenantAuthorization.hasAnyRole('SYSTEM_ADMIN','GROUP_ADMIN','COMPANY_ADMIN','HR_MANAGER')")
public class LeaveAccrualController {
    private final LeaveAccrualService service;
    public LeaveAccrualController(LeaveAccrualService service){this.service=service;}
    public record Configure(LocalDate accrualStart) {}
    @GetMapping public LeaveAccrualService.Settings settings(){return service.settings();}
    @PutMapping public LeaveAccrualService.Settings configure(@RequestBody Configure request){return service.configure(request.accrualStart());}
    @GetMapping("/events") public List<LeaveAccrualService.Event> events(){return service.events();}
    @PostMapping("/process") public Map<String,Integer> process(){return Map.of("processed",service.process(LocalDate.now(ZoneId.of(service.settings().timeZone()))));}
}
