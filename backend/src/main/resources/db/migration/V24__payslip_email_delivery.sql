CREATE TABLE payslip_delivery_attempt (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    payslip_id UUID NOT NULL,
    request_id UUID NOT NULL,
    recipient VARCHAR(320),
    requested_by VARCHAR(255) NOT NULL,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL CHECK (status IN ('SENDING','ACCEPTED','FAILED')),
    failure_code VARCHAR(80),
    UNIQUE(tenant_id, request_id),
    FOREIGN KEY(payslip_id,tenant_id) REFERENCES payslip(id,tenant_id)
);
CREATE INDEX ix_payslip_delivery_history ON payslip_delivery_attempt(tenant_id,payslip_id,requested_at);
