CREATE TABLE company_profile (
    tenant_id UUID PRIMARY KEY REFERENCES tenant(id),
    registered_address TEXT,
    pan VARCHAR(10),
    tan VARCHAR(10),
    gstin VARCHAR(15),
    pf_registration VARCHAR(80),
    esi_registration VARCHAR(80),
    logo_path VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE branch (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    code VARCHAR(40) NOT NULL,
    name VARCHAR(160) NOT NULL,
    address TEXT,
    state_code VARCHAR(2),
    professional_tax_applicable BOOLEAN NOT NULL DEFAULT FALSE,
    labour_welfare_fund_applicable BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_branch_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT uq_branch_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT ck_branch_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX ix_branch_tenant ON branch (tenant_id);

CREATE TABLE department (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    parent_department_id UUID REFERENCES department(id),
    code VARCHAR(40) NOT NULL,
    name VARCHAR(160) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_department_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT uq_department_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT ck_department_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT fk_department_parent_same_tenant
        FOREIGN KEY (parent_department_id, tenant_id) REFERENCES department(id, tenant_id)
);

CREATE INDEX ix_department_tenant ON department (tenant_id);

CREATE TABLE grade (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    code VARCHAR(40) NOT NULL,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_grade_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT uq_grade_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT ck_grade_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX ix_grade_tenant ON grade (tenant_id);

CREATE TABLE designation (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    grade_id UUID,
    code VARCHAR(40) NOT NULL,
    title VARCHAR(160) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_designation_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT ck_designation_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT fk_designation_grade_same_tenant
        FOREIGN KEY (grade_id, tenant_id) REFERENCES grade(id, tenant_id)
);

CREATE INDEX ix_designation_tenant ON designation (tenant_id);

CREATE TABLE cost_centre (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    code VARCHAR(40) NOT NULL,
    name VARCHAR(160) NOT NULL,
    accounting_reference VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_cost_centre_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT ck_cost_centre_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX ix_cost_centre_tenant ON cost_centre (tenant_id);

CREATE TABLE holiday (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    branch_id UUID,
    holiday_date DATE NOT NULL,
    name VARCHAR(160) NOT NULL,
    optional BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_holiday_tenant_branch_date UNIQUE NULLS NOT DISTINCT (tenant_id, branch_id, holiday_date),
    CONSTRAINT fk_holiday_branch_same_tenant
        FOREIGN KEY (branch_id, tenant_id) REFERENCES branch(id, tenant_id)
);

CREATE INDEX ix_holiday_tenant_date ON holiday (tenant_id, holiday_date);
