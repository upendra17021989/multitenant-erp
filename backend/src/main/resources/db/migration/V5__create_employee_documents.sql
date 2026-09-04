CREATE TABLE employee_document (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    employment_id UUID NOT NULL,
    document_type VARCHAR(40) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(150) NOT NULL,
    size_bytes BIGINT NOT NULL,
    storage_key VARCHAR(500) NOT NULL UNIQUE,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_employee_document_size CHECK (size_bytes > 0),
    CONSTRAINT fk_employee_document_employment_same_tenant
        FOREIGN KEY (employment_id, tenant_id) REFERENCES employment(id, tenant_id)
);

CREATE INDEX ix_employee_document_tenant_employment
    ON employee_document (tenant_id, employment_id, uploaded_at DESC);

