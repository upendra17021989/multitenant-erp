CREATE TABLE leave_approval_policy (
 tenant_id UUID NOT NULL REFERENCES tenant(id),
 step_number INTEGER NOT NULL CHECK(step_number BETWEEN 1 AND 3),
 approver_type VARCHAR(30) NOT NULL CHECK(approver_type IN ('REPORTING_MANAGER','HR_MANAGER','COMPANY_ADMIN')),
 updated_by VARCHAR(255) NOT NULL,
 updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(tenant_id,step_number), UNIQUE(tenant_id,approver_type)
);
CREATE TABLE leave_approval_step (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL,
 request_id UUID NOT NULL, step_number INTEGER NOT NULL,
 approver_type VARCHAR(30) NOT NULL CHECK(approver_type IN ('REPORTING_MANAGER','HR_MANAGER','COMPANY_ADMIN')),
 manager_employment_id UUID,
 status VARCHAR(20) NOT NULL CHECK(status IN ('WAITING','PENDING','APPROVED','REJECTED','CANCELLED')),
 decided_by VARCHAR(255), decided_at TIMESTAMP WITH TIME ZONE, comment VARCHAR(1000),
 UNIQUE(id,tenant_id), UNIQUE(tenant_id,request_id,step_number),
 FOREIGN KEY(request_id,tenant_id) REFERENCES leave_request(id,tenant_id),
 FOREIGN KEY(manager_employment_id,tenant_id) REFERENCES employment(id,tenant_id)
);
CREATE INDEX ix_leave_approval_pending ON leave_approval_step(tenant_id,status,request_id);
CREATE TABLE leave_notification (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL,
 request_id UUID NOT NULL, user_id UUID NOT NULL REFERENCES app_user(id),
 message VARCHAR(500) NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 read_at TIMESTAMP WITH TIME ZONE,
 FOREIGN KEY(request_id,tenant_id) REFERENCES leave_request(id,tenant_id)
);
CREATE INDEX ix_leave_notification_user ON leave_notification(tenant_id,user_id,created_at);
-- Existing pending requests retain a single HR approval stage.
INSERT INTO leave_approval_step(id,tenant_id,request_id,step_number,approver_type,status)
 SELECT id,tenant_id,id,1,'HR_MANAGER','PENDING' FROM leave_request WHERE status='PENDING';
