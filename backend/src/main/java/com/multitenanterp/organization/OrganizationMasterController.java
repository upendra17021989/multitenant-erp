package com.multitenanterp.organization;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/organization")
public class OrganizationMasterController {
    private static final String CAN_EDIT = "@tenantAuthorization.hasAnyRole('SYSTEM_ADMIN','GROUP_ADMIN','COMPANY_ADMIN','HR_MANAGER')";
    private final OrganizationMasterService service;

    public OrganizationMasterController(OrganizationMasterService service) { this.service = service; }

    @GetMapping("/departments") public List<Department> departments(){return service.departments();}
    @PostMapping("/departments") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize(CAN_EDIT)
    public Department createDepartment(@Valid @RequestBody SaveDepartmentRequest request){return service.saveDepartment(null,request);}
    @PutMapping("/departments/{id}") @PreAuthorize(CAN_EDIT)
    public Department updateDepartment(@PathVariable UUID id,@Valid @RequestBody SaveDepartmentRequest request){return service.saveDepartment(id,request);}

    @GetMapping("/grades") public List<Grade> grades(){return service.grades();}
    @PostMapping("/grades") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize(CAN_EDIT)
    public Grade createGrade(@Valid @RequestBody SaveGradeRequest request){return service.saveGrade(null,request);}
    @PutMapping("/grades/{id}") @PreAuthorize(CAN_EDIT)
    public Grade updateGrade(@PathVariable UUID id,@Valid @RequestBody SaveGradeRequest request){return service.saveGrade(id,request);}

    @GetMapping("/designations") public List<Designation> designations(){return service.designations();}
    @PostMapping("/designations") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize(CAN_EDIT)
    public Designation createDesignation(@Valid @RequestBody SaveDesignationRequest request){return service.saveDesignation(null,request);}
    @PutMapping("/designations/{id}") @PreAuthorize(CAN_EDIT)
    public Designation updateDesignation(@PathVariable UUID id,@Valid @RequestBody SaveDesignationRequest request){return service.saveDesignation(id,request);}

    @GetMapping("/cost-centres") public List<CostCentre> costCentres(){return service.costCentres();}
    @PostMapping("/cost-centres") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize(CAN_EDIT)
    public CostCentre createCostCentre(@Valid @RequestBody SaveCostCentreRequest request){return service.saveCostCentre(null,request);}
    @PutMapping("/cost-centres/{id}") @PreAuthorize(CAN_EDIT)
    public CostCentre updateCostCentre(@PathVariable UUID id,@Valid @RequestBody SaveCostCentreRequest request){return service.saveCostCentre(id,request);}

    @GetMapping("/holidays") public List<Holiday> holidays(@RequestParam(required=false) Integer year){return service.holidays(year);}
    @PostMapping("/holidays") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize(CAN_EDIT)
    public Holiday createHoliday(@Valid @RequestBody SaveHolidayRequest request){return service.saveHoliday(null,request);}
    @PutMapping("/holidays/{id}") @PreAuthorize(CAN_EDIT)
    public Holiday updateHoliday(@PathVariable UUID id,@Valid @RequestBody SaveHolidayRequest request){return service.saveHoliday(id,request);}
}