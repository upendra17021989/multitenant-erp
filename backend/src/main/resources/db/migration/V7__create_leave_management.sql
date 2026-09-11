CREATE TABLE leave_type (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    code VARCHAR(40) NOT NULL,
    name VARCHAR(160) NOT NULL,
    paid BOOLEAN NOT NULL DEFAULT TRUE,
    annual_entitlement NUMERIC(7,2) NOT NULL DEFAULT 0,
    accrual_frequency VARCHAR(20) NOT NULL DEFAULT 'ANNUAL',
    minimum_days NUMERIC(7,2) NOT NULL DEFAULT 0.5,
    maximum_days NUMERIC(7,2),
    half_day_allowed BOOLEAN NOT NULL DEFAULT TRUE,
    carry_forward_limit NUMERIC(7,2) NOT NULL DEFAULT 0,
    negative_balance_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    supporting_document_required BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_leave_type_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT uq_leave_type_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT ck_leave_type_frequency CHECK (accrual_frequency IN ('MONTHLY','QUARTERLY','ANNUAL','NONE')),
    CONSTRAINT ck_leave_type_status CHECK (status IN ('ACTIVE','INACTIVE')),
    CONSTRAINT ck_leave_type_days CHECK (annual_entitlement >= 0 AND minimum_days > 0 AND (maximum_days IS NULL OR maximum_days >= minimum_days) AND carry_forward_limit >= 0)
);

CREATE TABLE leave_balance (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    employment_id UUID NOT NULL,
    leave_type_id UUID NOT NULL,
    leave_year INTEGER NOT NULL,
    opening_balance NUMERIC(7,2) NOT NULL DEFAULT 0,
    accrued NUMERIC(7,2) NOT NULL DEFAULT 0,
    adjusted NUMERIC(7,2) NOT NULL DEFAULT 0,
    used NUMERIC(7,2) NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_leave_balance UNIQUE (tenant_id, employment_id, leave_type_id, leave_year),
    CONSTRAINT fk_leave_balance_employee FOREIGN KEY (employment_id, tenant_id) REFERENCES employment(id, tenant_id),
    CONSTRAINT fk_leave_balance_type FOREIGN KEY (leave_type_id, tenant_id) REFERENCES leave_type(id, tenant_id)
);

CREATE TABLE leave_request (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    employment_id UUID NOT NULL,
    leave_type_id UUID NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    requested_days NUMERIC(7,2) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    requested_by VARCHAR(255) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_by VARCHAR(255),
    decided_at TIMESTAMPTZ,
    decision_comment VARCHAR(1000),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_leave_request_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT ck_leave_request_dates CHECK (end_date >= start_date),
    CONSTRAINT ck_leave_request_days CHECK (requested_days > 0),
    CONSTRAINT ck_leave_request_status CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED')),
    CONSTRAINT fk_leave_request_employee FOREIGN KEY (employment_id, tenant_id) REFERENCES employment(id, tenant_id),
    CONSTRAINT fk_leave_request_type FOREIGN KEY (leave_type_id, tenant_id) REFERENCES leave_type(id, tenant_id)
);
CREATE INDEX ix_leave_request_tenant_status ON leave_request (tenant_id, status, start_date);
CREATE INDEX ix_leave_request_employee ON leave_request (tenant_id, employment_id, start_date);
