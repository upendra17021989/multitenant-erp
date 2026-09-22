package com.multitenanterp.payroll;

import com.multitenanterp.platform.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
public class PayslipEmailService {
    public record Attempt(UUID id, UUID payslipId, String recipient, String requestedBy,
                          Instant requestedAt, Instant completedAt, String status, String failureCode) {}
    private final JdbcClient db;
    private final PayslipService payslips;
    private final PayslipEmailSender sender;
    private final TransactionTemplate transaction;

    public PayslipEmailService(JdbcClient db, PayslipService payslips, PayslipEmailSender sender,
                              PlatformTransactionManager manager) {
        this.db = db;
        this.payslips = payslips;
        this.sender = sender;
        transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public boolean enabled() { return sender.enabled(); }

    public List<Attempt> history(int year, int month, UUID slip) {
        requirePayslip(year, month, slip);
        return attempts(" AND payslip_id=:slip ORDER BY requested_at DESC", slip);
    }

    public Attempt send(int year, int month, UUID slip, UUID request, String actor) {
        UUID tenant = TenantContext.requireTenantId();
        Payslip metadata = requirePayslip(year, month, slip);
        if (!enabled()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Payslip email is disabled");
        // Serialize claims per tenant, including reuse of a request key across different payslips.
        boolean claimed = Boolean.TRUE.equals(transaction.execute(status -> {
            db.sql("SELECT id FROM tenant WHERE id=:tenant FOR UPDATE").param("tenant", tenant).query(UUID.class).single();
            var existing = byRequest(request);
            if (existing.isPresent()) {
                if (!existing.get().payslipId().equals(slip)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email request belongs to another payslip");
                return false;
            }
            if (db.sql("SELECT COUNT(*) FROM payslip_delivery_attempt WHERE tenant_id=:tenant AND payslip_id=:slip AND status='SENDING'")
                    .param("tenant", tenant).param("slip", slip).query(Integer.class).single() > 0)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "An email attempt is still sending or has an unknown outcome; check its delivery history");
            String runStatus = db.sql("SELECT status FROM payroll_run WHERE id=:run AND tenant_id=:tenant FOR UPDATE")
                    .param("run", metadata.payrollRunId()).param("tenant", tenant).query(String.class).single();
            String slipStatus = db.sql("SELECT status FROM payslip WHERE id=:slip AND tenant_id=:tenant")
                    .param("slip", slip).param("tenant", tenant).query(String.class).single();
            if (!slipStatus.equals("RELEASED") || !Set.of("LOCKED", "PAID").contains(runStatus))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email requires a released payslip and locked or paid payroll");
            String recipient = db.sql("SELECT work_email FROM employment WHERE id=:employee AND tenant_id=:tenant")
                    .param("employee", metadata.employmentId()).param("tenant", tenant)
                    .query((r,n) -> Objects.toString(r.getString(1), "").trim()).single();
            db.sql("INSERT INTO payslip_delivery_attempt(id,tenant_id,payslip_id,request_id,recipient,requested_by,status) VALUES(:id,:tenant,:slip,:request,:recipient,:actor,'SENDING')")
                    .param("id", UUID.randomUUID()).param("tenant", tenant).param("slip", slip).param("request", request)
                    .param("recipient", recipient).param("actor", actor).update();
            return true;
        }));
        Attempt attempt = byRequest(request).orElseThrow();
        if (!claimed) return attempt;
        String failure = null;
        if (!SmtpPayslipEmailSender.validAddress(attempt.recipient())) failure = "INVALID_WORK_EMAIL";
        else {
            byte[] content = null;
            try (var input = payslips.downloadReleased(slip, metadata.employmentId()).resource().getInputStream()) {
                content = input.readAllBytes();
                if (content.length != metadata.sizeBytes() || !HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content)).equals(metadata.sha256()))
                    failure = "CONTENT_INTEGRITY_FAILED";
            } catch (Exception e) { failure = "CONTENT_UNAVAILABLE"; }
            if (failure == null) {
                try {
                    String company = db.sql("SELECT legal_name FROM tenant WHERE id=:tenant").param("tenant", tenant).query(String.class).single();
                    sender.send(attempt.recipient(), company, year, month, metadata.fileName(), content);
                } catch (Exception e) { failure = "EMAIL_SEND_FAILED"; }
            }
        }
        String result = failure;
        transaction.executeWithoutResult(status -> db.sql("UPDATE payslip_delivery_attempt SET status=:status,failure_code=:failure,completed_at=CURRENT_TIMESTAMP WHERE id=:id AND tenant_id=:tenant")
                .param("status", result == null ? "ACCEPTED" : "FAILED").param("failure", result)
                .param("id", attempt.id()).param("tenant", tenant).update());
        return byRequest(request).orElseThrow();
    }

    private Payslip requirePayslip(int year, int month, UUID id) {
        return payslips.list(year, month).stream().filter(p -> p.id().equals(id)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payslip not found"));
    }
    private Optional<Attempt> byRequest(UUID request) { return attempts(" AND request_id=:slip", request).stream().findFirst(); }
    private List<Attempt> attempts(String suffix, UUID id) {
        return db.sql("SELECT * FROM payslip_delivery_attempt WHERE tenant_id=:tenant" + suffix)
                .param("tenant", TenantContext.requireTenantId()).param("slip", id).query((r,n) -> new Attempt(
                        r.getObject("id", UUID.class), r.getObject("payslip_id", UUID.class), r.getString("recipient"),
                        r.getString("requested_by"), r.getTimestamp("requested_at").toInstant(),
                        r.getTimestamp("completed_at") == null ? null : r.getTimestamp("completed_at").toInstant(),
                        r.getString("status"), r.getString("failure_code"))).list();
    }
}
