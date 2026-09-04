package com.multitenanterp.employee;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/employees/{employeeId}/documents")
@PreAuthorize("@tenantAuthorization.hasAnyRole('SYSTEM_ADMIN','GROUP_ADMIN','COMPANY_ADMIN','HR_MANAGER')")
public class EmployeeDocumentController {
    private final EmployeeDocumentService service;
    public EmployeeDocumentController(EmployeeDocumentService service){this.service=service;}

    @GetMapping public List<EmployeeDocument> documents(@PathVariable UUID employeeId){return service.documents(employeeId);}
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public EmployeeDocument upload(@PathVariable UUID employeeId,@RequestParam String documentType,@RequestPart MultipartFile file){return service.upload(employeeId,documentType,file);}
    @GetMapping("/{documentId}/content")
    public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable UUID employeeId,@PathVariable UUID documentId){
        var download=service.download(employeeId,documentId); var metadata=download.metadata();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(metadata.contentType())).contentLength(metadata.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(metadata.fileName()).build().toString()).body(download.resource());
    }
}
