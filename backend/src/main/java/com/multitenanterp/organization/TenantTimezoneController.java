package com.multitenanterp.organization;

import com.multitenanterp.platform.tenant.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.ZoneId;
import java.time.DateTimeException;

@RestController
@RequestMapping("/api/organization/timezone")
public class TenantTimezoneController {
    private final JdbcClient db;
    public TenantTimezoneController(JdbcClient db) {this.db=db;}
    public record Timezone(@NotBlank String timeZone) {}
    @GetMapping public Timezone get() {return new Timezone(db.sql("SELECT time_zone FROM tenant WHERE id=:tenant").param("tenant",TenantContext.requireTenantId()).query(String.class).single());}
    @PutMapping @PreAuthorize("@tenantAuthorization.hasAnyRole('SYSTEM_ADMIN','GROUP_ADMIN','COMPANY_ADMIN')")
    public Timezone save(@Valid @RequestBody Timezone request) {
        try {ZoneId.of(request.timeZone());} catch(DateTimeException e) {throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unknown IANA timezone");}
        db.sql("UPDATE tenant SET time_zone=:zone,updated_at=CURRENT_TIMESTAMP WHERE id=:tenant").param("zone",request.timeZone()).param("tenant",TenantContext.requireTenantId()).update();
        return get();
    }
}
