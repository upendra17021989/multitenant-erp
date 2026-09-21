ALTER TABLE tenant ADD COLUMN leave_accrual_start DATE;
CREATE TABLE leave_processing_event (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    employment_id UUID NOT NULL,
    leave_type_id UUID NOT NULL,
    leave_year INTEGER NOT NULL,
    event_type VARCHAR(20) NOT NULL CHECK(event_type IN ('ACCRUAL','CARRY_FORWARD')),
    period_end DATE NOT NULL,
    amount NUMERIC(7,2) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id,employment_id,leave_type_id,event_type,period_end),
    FOREIGN KEY(employment_id,tenant_id) REFERENCES employment(id,tenant_id),
    FOREIGN KEY(leave_type_id,tenant_id) REFERENCES leave_type(id,tenant_id)
);
CREATE INDEX ix_leave_processing_tenant ON leave_processing_event(tenant_id,processed_at);
