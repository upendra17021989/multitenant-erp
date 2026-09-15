CREATE TABLE employee_user_link (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    user_id UUID NOT NULL REFERENCES app_user(id),
    employment_id UUID NOT NULL,
    linked_by VARCHAR(320) NOT NULL,
    linked_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_employee_user_link_user UNIQUE (tenant_id, user_id),
    CONSTRAINT uq_employee_user_link_employment UNIQUE (tenant_id, employment_id),
    CONSTRAINT fk_employee_user_link_employment FOREIGN KEY (employment_id, tenant_id)
        REFERENCES employment(id, tenant_id)
);

CREATE INDEX ix_employee_user_link_employment ON employee_user_link (tenant_id, employment_id);

