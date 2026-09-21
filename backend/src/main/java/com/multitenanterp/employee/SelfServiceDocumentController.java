package com.multitenanterp.employee;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController @RequestMapping("/api/self/documents")
public class SelfServiceDocumentController {
    private final EmployeeUserLinkService links;
    private final EmployeeDocumentService documents;
    public SelfServiceDocumentController(EmployeeUserLinkService links,EmployeeDocumentService documents){this.links=links;this.documents=documents;}
    @PostMapping(value="/leave-support",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public EmployeeDocument leave(@RequestPart MultipartFile file){return documents.upload(links.currentEmploymentId(),"LEAVE_SUPPORT",file);}
    @PostMapping(value="/expense-receipt",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public EmployeeDocument receipt(@RequestPart MultipartFile file){return documents.upload(links.currentEmploymentId(),"EXPENSE_RECEIPT",file);}
}
