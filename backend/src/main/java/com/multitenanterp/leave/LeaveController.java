package com.multitenanterp.leave;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/leave")
public class LeaveController {
 private static final String ADMIN="@tenantAuthorization.hasAnyRole('SYSTEM_ADMIN','GROUP_ADMIN','COMPANY_ADMIN','HR_MANAGER','HR_EXECUTIVE')";
 private final LeaveService service;
 private final com.multitenanterp.employee.EmployeeUserLinkService links;
 private final com.multitenanterp.platform.security.TenantAuthorization authorization;
 public LeaveController(LeaveService service,com.multitenanterp.employee.EmployeeUserLinkService links,com.multitenanterp.platform.security.TenantAuthorization authorization){this.service=service;this.links=links;this.authorization=authorization;}
 private UUID scope(UUID requested){
  if(isAdmin())return requested;
  UUID own=links.currentEmploymentId();
  if(requested!=null&&!requested.equals(own))throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN,"Only your own leave records are accessible");
  return own;
 }
 private boolean isAdmin(){return authorization.hasAnyRole("SYSTEM_ADMIN","GROUP_ADMIN","COMPANY_ADMIN","HR_MANAGER","HR_EXECUTIVE");}
 @GetMapping("/types") public List<LeaveType> types(){return service.types();}
 @PostMapping("/types") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize(ADMIN) public LeaveType createType(@Valid @RequestBody SaveLeaveTypeRequest r){return service.saveType(null,r);}
 @PutMapping("/types/{id}") @PreAuthorize(ADMIN) public LeaveType updateType(@PathVariable UUID id,@Valid @RequestBody SaveLeaveTypeRequest r){return service.saveType(id,r);}
 @GetMapping("/balances") public List<LeaveBalance> balances(@RequestParam(required=false) UUID employmentId,@RequestParam(required=false) Integer year){return service.balances(scope(employmentId),year);}
 @PutMapping("/balances") @PreAuthorize(ADMIN) public LeaveBalance balance(@Valid @RequestBody SaveLeaveBalanceRequest r){return service.saveBalance(r);}
 @GetMapping("/requests") public List<LeaveRequest> requests(@RequestParam(required=false) UUID employmentId,@RequestParam(required=false) String status){return service.requests(scope(employmentId),status);}
 @PostMapping("/requests") @ResponseStatus(HttpStatus.CREATED) public LeaveRequest request(@Valid @RequestBody CreateLeaveRequest r,Authentication a){scope(r.employmentId());return service.request(r,a.getName());}
 @PostMapping("/requests/{id}/decision") public LeaveRequest decide(@PathVariable UUID id,@Valid @RequestBody DecideLeaveRequest r,Authentication a){return service.decide(id,r,a.getName());}
 @PostMapping("/requests/{id}/cancel") public LeaveRequest cancel(@PathVariable UUID id,Authentication a){return service.cancelOwned(id,a.getName(),scope(null));}
}
