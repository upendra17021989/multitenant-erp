package com.multitenanterp.employee;
import com.multitenanterp.platform.security.TenantAuthorization;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController @RequestMapping("/api/financial-requests")
public class FinancialRequestController {
    private static final String FINANCE="@tenantAuthorization.hasAnyRole('SYSTEM_ADMIN','GROUP_ADMIN','COMPANY_ADMIN','PAYROLL_MANAGER')";
    private final FinancialRequestService service;private final EmployeeUserLinkService links;private final TenantAuthorization authorization;private final EmployeeDocumentService documents;
    public FinancialRequestController(FinancialRequestService service,EmployeeUserLinkService links,TenantAuthorization authorization,EmployeeDocumentService documents){this.service=service;this.links=links;this.authorization=authorization;this.documents=documents;}
    private boolean admin(){return authorization.hasAnyRole("SYSTEM_ADMIN","GROUP_ADMIN","COMPANY_ADMIN","PAYROLL_MANAGER");}
    @GetMapping public List<FinancialRequestService.Request> list(){return service.list(admin()?null:links.currentEmploymentId());}
    @PostMapping public FinancialRequestService.Request submit(@Valid @RequestBody FinancialRequestService.Submit request,Authentication actor){
        if(!admin()&&!links.currentEmploymentId().equals(request.employmentId()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only your own financial requests can be submitted");
        return service.submit(request,actor.getName());
    }
    @GetMapping("/{id}/recoveries") public List<FinancialRequestService.Recovery> recoveries(@PathVariable UUID id){return service.recoveries(id,admin()?null:links.currentEmploymentId());}
    @PostMapping(value="/employees/{employeeId}/receipt",consumes="multipart/form-data") @PreAuthorize(FINANCE)
    public EmployeeDocument receipt(@PathVariable UUID employeeId,@RequestPart org.springframework.web.multipart.MultipartFile file){return documents.upload(employeeId,"EXPENSE_RECEIPT",file);}
    @GetMapping("/{id}/receipt")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> downloadReceipt(@PathVariable UUID id){
        var request=list().stream().filter(r->r.id().equals(id)).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Request not found"));
        if(request.receiptId()==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Receipt not found");
        var download=documents.download(request.employmentId(),request.receiptId());
        return org.springframework.http.ResponseEntity.ok().contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
            .header("Content-Disposition",org.springframework.http.ContentDisposition.attachment().filename(download.metadata().fileName()).build().toString())
            .cacheControl(org.springframework.http.CacheControl.noStore()).body(download.resource());
    }
    @PostMapping("/{id}/decision") @PreAuthorize(FINANCE)
    public FinancialRequestService.Request decide(@PathVariable UUID id,@Valid @RequestBody FinancialRequestService.Decision request,Authentication actor){return service.decide(id,request,actor.getName());}
    @PostMapping("/{id}/payment") @PreAuthorize(FINANCE)
    public FinancialRequestService.Request payment(@PathVariable UUID id,@Valid @RequestBody FinancialRequestService.Payment request,Authentication actor){return service.recordPayment(id,request.reference(),actor.getName());}
}
