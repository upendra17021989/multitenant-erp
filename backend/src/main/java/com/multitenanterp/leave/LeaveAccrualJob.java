package com.multitenanterp.leave;

import com.multitenanterp.platform.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.*;
import java.util.UUID;

@Configuration @EnableScheduling
public class LeaveAccrualJob {
    private static final Logger log=LoggerFactory.getLogger(LeaveAccrualJob.class);
    private final JdbcClient db;private final LeaveAccrualService service;
    public LeaveAccrualJob(JdbcClient db,LeaveAccrualService service){this.db=db;this.service=service;}
    @Scheduled(cron="0 15 * * * *",zone="UTC")
    public void run(){
        for(UUID tenant:db.sql("SELECT id FROM tenant WHERE status='ACTIVE' AND leave_accrual_start IS NOT NULL").query(UUID.class).list()) {
            try {TenantContext.set(tenant);service.process(LocalDate.now(ZoneId.of(service.settings().timeZone())));}
            catch(RuntimeException e){log.error("Leave accrual failed for tenant {}",tenant,e);}
            finally {TenantContext.clear();}
        }
    }
}
