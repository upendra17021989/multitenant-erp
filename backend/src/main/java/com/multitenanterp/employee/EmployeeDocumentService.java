package com.multitenanterp.employee;

import com.multitenanterp.platform.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class EmployeeDocumentService {
    private static final long MAX_SIZE = 10 * 1024 * 1024;
    private final JdbcClient db;
    private final DocumentStorage storage;

    public EmployeeDocumentService(JdbcClient db, DocumentStorage storage) { this.db = db; this.storage = storage; }

    public List<EmployeeDocument> documents(UUID employmentId) {
        requireEmployee(employmentId);
        return db.sql("""
                SELECT id,employment_id,document_type,file_name,content_type,size_bytes,uploaded_at
                FROM employee_document WHERE tenant_id=:tenant AND employment_id=:employment ORDER BY uploaded_at DESC
                """).param("tenant", tenant()).param("employment", employmentId).query(EmployeeDocumentService::map).list();
    }

    @Transactional
    public EmployeeDocument upload(UUID employmentId, String documentType, MultipartFile file) {
        requireEmployee(employmentId);
        if (file.isEmpty()) throw badRequest("Document must not be empty");
        if (file.getSize() > MAX_SIZE) throw badRequest("Document must not exceed 10 MB");
        String type = normalizeType(documentType);
        String fileName = safeFileName(file.getOriginalFilename());
        String contentType = file.getContentType()==null ? "application/octet-stream" : file.getContentType();
        UUID tenant = tenant(), id = UUID.randomUUID();
        String key = tenant + "/" + employmentId + "/" + id;
        try {
            storage.store(key, file.getInputStream());
            db.sql("""
                    INSERT INTO employee_document(id,tenant_id,employment_id,document_type,file_name,content_type,size_bytes,storage_key)
                    VALUES(:id,:tenant,:employment,:type,:name,:contentType,:size,:key)
                    """).param("id",id).param("tenant",tenant).param("employment",employmentId).param("type",type)
                    .param("name",fileName).param("contentType",contentType).param("size",file.getSize()).param("key",key).update();
        } catch (Exception exception) {
            try { storage.delete(key); } catch (IOException ignored) { }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Unable to store document",exception);
        }
        return document(id);
    }

    public EmployeeDocumentDownload download(UUID employmentId, UUID documentId) {
        requireEmployee(employmentId);
        var row = db.sql("""
                SELECT id,employment_id,document_type,file_name,content_type,size_bytes,uploaded_at,storage_key
                FROM employee_document WHERE id=:id AND employment_id=:employment AND tenant_id=:tenant
                """).param("id",documentId).param("employment",employmentId).param("tenant",tenant())
                .query((r,n) -> new StoredDocument(map(r,n),r.getString("storage_key"))).optional().orElseThrow(EmployeeDocumentService::missing);
        var resource = storage.load(row.storageKey());
        if (!resource.exists()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Document content not found");
        return new EmployeeDocumentDownload(row.metadata(),resource);
    }

    private EmployeeDocument document(UUID id) {
        return db.sql("SELECT id,employment_id,document_type,file_name,content_type,size_bytes,uploaded_at FROM employee_document WHERE id=:id AND tenant_id=:tenant")
                .param("id",id).param("tenant",tenant()).query(EmployeeDocumentService::map).optional().orElseThrow(EmployeeDocumentService::missing);
    }
    private void requireEmployee(UUID id) {
        int count=db.sql("SELECT COUNT(*) FROM employment WHERE id=:id AND tenant_id=:tenant").param("id",id).param("tenant",tenant()).query(Integer.class).single();
        if(count==0) throw missing();
    }
    private static EmployeeDocument map(ResultSet r,int row) throws SQLException { return new EmployeeDocument(r.getObject("id",UUID.class),r.getObject("employment_id",UUID.class),r.getString("document_type"),r.getString("file_name"),r.getString("content_type"),r.getLong("size_bytes"),r.getObject("uploaded_at",java.time.OffsetDateTime.class)); }
    private static String normalizeType(String value) { if(value==null||!value.matches("[A-Za-z0-9_-]{1,40}")) throw badRequest("Invalid document type"); return value.toUpperCase(Locale.ROOT); }
    private static String safeFileName(String value) { if(value==null||value.isBlank()) return "document"; String name=PathName.basename(value.trim()); return name.length()>255?name.substring(name.length()-255):name; }
    private static UUID tenant(){return TenantContext.requireTenantId();}
    private static ResponseStatusException missing(){return new ResponseStatusException(HttpStatus.NOT_FOUND,"Employee document not found");}
    private static ResponseStatusException badRequest(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
    private record StoredDocument(EmployeeDocument metadata,String storageKey){}
    private static final class PathName { private static String basename(String value){return value.replace('\\','/').substring(value.replace('\\','/').lastIndexOf('/')+1);} }
}
