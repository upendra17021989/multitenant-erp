package com.multitenanterp.attendance;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {
    private static final String CAN_EDIT="@tenantAuthorization.hasAnyRole('SYSTEM_ADMIN','GROUP_ADMIN','COMPANY_ADMIN','HR_MANAGER','HR_EXECUTIVE')";
    private static final String CAN_LOCK="@tenantAuthorization.hasAnyRole('SYSTEM_ADMIN','GROUP_ADMIN','COMPANY_ADMIN','HR_MANAGER','PAYROLL_MANAGER')";
    private final AttendanceService service;
    public AttendanceController(AttendanceService service){this.service=service;}

    @GetMapping("/shifts") public List<WorkShift> shifts(@RequestParam(required=false) LocalDate onDate){return service.shifts(onDate);}
    @PostMapping("/shifts") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize(CAN_EDIT)
    public WorkShift createShift(@Valid @RequestBody SaveWorkShiftRequest r){return service.saveShift(null,r);}
    @PutMapping("/shifts/{id}") @PreAuthorize(CAN_EDIT)
    public WorkShift updateShift(@PathVariable UUID id,@Valid @RequestBody SaveWorkShiftRequest r){return service.saveShift(id,r);}
    @GetMapping("/shift-assignments") public List<ShiftAssignment> assignments(@RequestParam UUID employmentId){return service.assignments(employmentId);}
    @PostMapping("/shift-assignments") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize(CAN_EDIT)
    public ShiftAssignment assign(@Valid @RequestBody SaveShiftAssignmentRequest r){return service.assignShift(r);}
    @GetMapping("/records") public List<AttendanceRecord> records(@RequestParam LocalDate from,@RequestParam LocalDate to,@RequestParam(required=false) UUID employmentId){return service.attendance(from,to,employmentId);}
    @PostMapping("/records") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize(CAN_EDIT)
    public AttendanceRecord createRecord(@Valid @RequestBody SaveAttendanceRequest r){return service.saveAttendance(null,r);}
    @PutMapping("/records/{id}") @PreAuthorize(CAN_EDIT)
    public AttendanceRecord updateRecord(@PathVariable UUID id,@Valid @RequestBody SaveAttendanceRequest r){return service.saveAttendance(id,r);}
    @PostMapping("/months/{month}/lock") @PreAuthorize(CAN_LOCK)
    public AttendanceMonthLock lock(@PathVariable YearMonth month,Authentication auth){return service.lockMonth(month,auth.getName());}
    @DeleteMapping("/months/{month}/lock") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize(CAN_LOCK)
    public void reopen(@PathVariable YearMonth month){service.reopenMonth(month);}
}
