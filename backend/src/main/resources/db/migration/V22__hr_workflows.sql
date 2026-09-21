CREATE TABLE hr_workflow (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    employment_id UUID,
    workflow_type VARCHAR(30) NOT NULL CHECK(workflow_type IN ('RECRUITMENT','ONBOARDING','TRAINING','APPRAISAL','PROMOTION','SALARY_REVISION','EXIT')),
    title VARCHAR(200) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    effective_date DATE NOT NULL,
    configuration TEXT NOT NULL,
    tasks TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS' CHECK(status IN ('IN_PROGRESS','UNDER_REVIEW','APPROVED','REJECTED','COMPLETED')),
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(id,tenant_id),
    FOREIGN KEY(employment_id,tenant_id) REFERENCES employment(id,tenant_id)
);
CREATE TABLE hr_workflow_event (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    workflow_id UUID NOT NULL,
    action VARCHAR(30) NOT NULL,
    actor VARCHAR(255) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    before_value TEXT,
    after_value TEXT NOT NULL,
    acted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(workflow_id,tenant_id) REFERENCES hr_workflow(id,tenant_id)
);
CREATE INDEX ix_hr_workflow_status ON hr_workflow(tenant_id,workflow_type,status);
