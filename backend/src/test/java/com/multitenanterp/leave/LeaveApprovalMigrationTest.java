package com.multitenanterp.leave;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class LeaveApprovalMigrationTest {
    @Test void migrationAddsDefaultStageOnlyToExistingPendingRequests(){
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        var db=JdbcClient.create(ds);
        db.sql("CREATE TABLE tenant(id UUID PRIMARY KEY)").update();
        db.sql("CREATE TABLE employment(id UUID,tenant_id UUID,UNIQUE(id,tenant_id))").update();
        db.sql("CREATE TABLE app_user(id UUID PRIMARY KEY)").update();
        db.sql("CREATE TABLE leave_request(id UUID PRIMARY KEY,tenant_id UUID,status VARCHAR,UNIQUE(id,tenant_id))").update();
        UUID tenant=UUID.randomUUID(),pending=UUID.randomUUID();
        db.sql("INSERT INTO tenant VALUES(?)").param(tenant).update();
        db.sql("INSERT INTO leave_request VALUES(?,?,'PENDING'),(?,?,'APPROVED'),(?,?,'CANCELLED')")
                .params(pending,tenant,UUID.randomUUID(),tenant,UUID.randomUUID(),tenant).update();
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V25__leave_approval_chains.sql")).execute(ds);
        assertThat(db.sql("SELECT request_id FROM leave_approval_step").query(UUID.class).list()).containsExactly(pending);
        assertThat(db.sql("SELECT approver_type FROM leave_approval_step").query(String.class).single()).isEqualTo("HR_MANAGER");
        assertThat(db.sql("SELECT status FROM leave_approval_step").query(String.class).single()).isEqualTo("PENDING");
    }
}
