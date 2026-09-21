package com.multitenanterp.payroll;

import com.multitenanterp.employee.EmployeeUserLinkService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/self/payslips")
public class EmployeePayslipController {
    private final PayslipService service;
    private final EmployeeUserLinkService links;
    public EmployeePayslipController(PayslipService service, EmployeeUserLinkService links) {
        this.service = service;
        this.links = links;
    }
    @GetMapping
    public List<Payslip> list() { return service.releasedForEmployee(links.currentEmploymentId()); }
    @GetMapping("/{id}/content")
    public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable UUID id) {
        var download = service.downloadReleased(id, links.currentEmploymentId());
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore())
                .contentLength(download.metadata().sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.metadata().fileName()).build().toString())
                .body(download.resource());
    }
}
