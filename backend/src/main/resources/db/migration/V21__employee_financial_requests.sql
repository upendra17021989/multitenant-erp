CREATE TABLE employee_financial_request (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    employment_id UUID NOT NULL,
    request_type VARCHAR(20) NOT NULL CHECK(request_type IN ('EXPENSE','LOAN','ADVANCE')),
    amount NUMERIC(14,2) NOT NULL CHECK(amount>0),
    installments INTEGER NOT NULL CHECK(installments BETWEEN 1 AND 60),
    first_recovery_month DATE,
    purpose VARCHAR(1000) NOT NULL,
    receipt_id UUID,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','APPROVED','REJECTED','PAID','DISBURSED')),
    requested_by VARCHAR(255) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_by VARCHAR(255),
    decided_at TIMESTAMPTZ,
    decision_comment VARCHAR(1000),
    payment_reference VARCHAR(200),
    paid_by VARCHAR(255),
    paid_at TIMESTAMPTZ,
    UNIQUE(id,tenant_id),
    FOREIGN KEY(employment_id,tenant_id) REFERENCES employment(id,tenant_id),
    FOREIGN KEY(receipt_id,tenant_id,employment_id) REFERENCES employee_document(id,tenant_id,employment_id),
    CHECK(request_type<>'EXPENSE' OR receipt_id IS NOT NULL),
    CHECK(request_type='EXPENSE' OR first_recovery_month IS NOT NULL)
);
ALTER TABLE payroll_variable_input ADD COLUMN financial_request_id UUID;
ALTER TABLE payroll_variable_input ADD CONSTRAINT fk_variable_financial_request FOREIGN KEY(financial_request_id,tenant_id) REFERENCES employee_financial_request(id,tenant_id);
CREATE INDEX ix_financial_request_employee ON employee_financial_request(tenant_id,employment_id,status);
