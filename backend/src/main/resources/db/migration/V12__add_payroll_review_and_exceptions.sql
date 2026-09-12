ALTER TABLE payroll_run ADD COLUMN review_started_at TIMESTAMPTZ;
ALTER TABLE payroll_run ADD COLUMN review_started_by VARCHAR(255);
CREATE TABLE payroll_exception (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, payroll_run_id UUID NOT NULL, employment_id UUID,
 exception_code VARCHAR(50) NOT NULL, severity VARCHAR(10) NOT NULL, message VARCHAR(500) NOT NULL,
 resolved BOOLEAN NOT NULL DEFAULT FALSE, resolution_comment VARCHAR(1000), resolved_by VARCHAR(255), resolved_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT uq_payroll_exception UNIQUE(tenant_id,payroll_run_id,employment_id,exception_code),
 CONSTRAINT fk_exception_run FOREIGN KEY(payroll_run_id,tenant_id) REFERENCES payroll_run(id,tenant_id),
 CONSTRAINT fk_exception_employee FOREIGN KEY(employment_id,tenant_id) REFERENCES employment(id,tenant_id),
 CONSTRAINT ck_exception_severity CHECK(severity IN ('WARNING','ERROR'))
);
CREATE INDEX ix_payroll_exception_run ON payroll_exception(tenant_id,payroll_run_id,resolved,severity);
