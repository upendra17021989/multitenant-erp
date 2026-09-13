ALTER TABLE payroll_run DROP CONSTRAINT ck_payroll_status;
ALTER TABLE payroll_run ADD CONSTRAINT ck_payroll_status
    CHECK(status IN ('DRAFT','CALCULATED','UNDER_REVIEW','APPROVED','LOCKED','PAID','REVERSED'));

ALTER TABLE payroll_run ADD COLUMN approved_at TIMESTAMPTZ;
ALTER TABLE payroll_run ADD COLUMN approved_by VARCHAR(255);
ALTER TABLE payroll_run ADD COLUMN locked_at TIMESTAMPTZ;
ALTER TABLE payroll_run ADD COLUMN locked_by VARCHAR(255);
ALTER TABLE payroll_run ADD COLUMN reversed_at TIMESTAMPTZ;
ALTER TABLE payroll_run ADD COLUMN reversed_by VARCHAR(255);
ALTER TABLE payroll_run ADD COLUMN reversal_reason VARCHAR(1000);

CREATE TABLE payroll_run_transition (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    payroll_run_id UUID NOT NULL,
    from_status VARCHAR(20) NOT NULL,
    to_status VARCHAR(20) NOT NULL,
    reason VARCHAR(1000),
    acted_by VARCHAR(255) NOT NULL,
    acted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payroll_transition_run FOREIGN KEY(payroll_run_id,tenant_id)
        REFERENCES payroll_run(id,tenant_id),
    CONSTRAINT ck_payroll_transition_status CHECK(
        from_status IN ('CALCULATED','UNDER_REVIEW','APPROVED','LOCKED','PAID') AND
        to_status IN ('UNDER_REVIEW','APPROVED','LOCKED','REVERSED')
    )
);
CREATE INDEX ix_payroll_transition_run
    ON payroll_run_transition(tenant_id,payroll_run_id,acted_at);
