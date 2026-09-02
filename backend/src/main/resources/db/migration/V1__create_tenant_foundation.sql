CREATE TABLE tenant (
    id UUID PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    legal_name VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_tenant_status CHECK (status IN ('DRAFT', 'ACTIVE', 'SUSPENDED'))
);

CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_app_user_status CHECK (status IN ('INVITED', 'ACTIVE', 'LOCKED', 'DISABLED'))
);

CREATE TABLE user_tenant_role (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    user_id UUID NOT NULL REFERENCES app_user(id),
    role_code VARCHAR(80) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMPTZ,
    CONSTRAINT uq_user_tenant_role UNIQUE (tenant_id, user_id, role_code)
);

CREATE INDEX ix_user_tenant_role_user ON user_tenant_role(user_id, tenant_id);
