package com.multitenanterp.attendance;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/attendance/imports")
public class AttendanceImportController {
    private static final String CAN_IMPORT="@tenantAuthorization.hasAnyRole('SYSTEM_ADMIN','GROUP_ADMIN','COMPANY_ADMIN','HR_MANAGER','HR_EXECUTIVE')";
    private final AttendanceCsvImportService service;
    public AttendanceImportController(AttendanceCsvImportService service){this.service=service;}

    @PostMapping(value="/csv",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(CAN_IMPORT)
    public AttendanceImportResult importCsv(@RequestPart("file") MultipartFile file){return service.importCsv(file);}
}
