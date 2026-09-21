package com.multitenanterp.attendance;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/attendance/reviews")
@PreAuthorize("@tenantAuthorization.hasAnyRole('SYSTEM_ADMIN','GROUP_ADMIN','COMPANY_ADMIN','HR_MANAGER','HR_EXECUTIVE')")
public class AttendanceReviewController {
    private final AttendanceReviewService service;
    public AttendanceReviewController(AttendanceReviewService service){this.service=service;}
    @GetMapping public List<AttendanceReviewService.Review> list(){return service.list();}
    @PostMapping public AttendanceReviewService.Review request(@Valid @RequestBody AttendanceReviewService.Request request,Authentication actor){return service.request(request,actor.getName());}
    @PostMapping("/{id}/decision") @PreAuthorize("@tenantAuthorization.hasAnyRole('SYSTEM_ADMIN','GROUP_ADMIN','COMPANY_ADMIN','HR_MANAGER')")
    public AttendanceReviewService.Review decide(@PathVariable UUID id,@Valid @RequestBody AttendanceReviewService.Decision decision,Authentication actor){return service.decide(id,decision,actor.getName());}
}
