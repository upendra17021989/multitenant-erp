CREATE TABLE payroll_run (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenant(id), payroll_year INTEGER NOT NULL,
 payroll_month INTEGER NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', calculated_at TIMESTAMPTZ NOT NULL,
 calculated_by VARCHAR(255) NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT uq_payroll_run_period UNIQUE(tenant_id,payroll_year,payroll_month),
 CONSTRAINT uq_payroll_run_id_tenant UNIQUE(id,tenant_id),
 CONSTRAINT ck_payroll_period CHECK(payroll_year BETWEEN 2000 AND 2200 AND payroll_month BETWEEN 1 AND 12),
 CONSTRAINT ck_payroll_status CHECK(status IN ('DRAFT','CALCULATED','UNDER_REVIEW','APPROVED','LOCKED','PAID'))
);
CREATE TABLE payroll_employee_result (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, payroll_run_id UUID NOT NULL, employment_id UUID NOT NULL,
 salary_assignment_id UUID NOT NULL, gross_pay NUMERIC(14,2) NOT NULL, deductions NUMERIC(14,2) NOT NULL,
 employer_contributions NUMERIC(14,2) NOT NULL, net_pay NUMERIC(14,2) NOT NULL, ctc NUMERIC(14,2) NOT NULL,
 CONSTRAINT uq_payroll_employee UNIQUE(tenant_id,payroll_run_id,employment_id),
 CONSTRAINT uq_payroll_result_id_tenant UNIQUE(id,tenant_id),
 CONSTRAINT fk_payroll_result_run FOREIGN KEY(payroll_run_id,tenant_id) REFERENCES payroll_run(id,tenant_id),
 CONSTRAINT fk_payroll_result_employee FOREIGN KEY(employment_id,tenant_id) REFERENCES employment(id,tenant_id),
 CONSTRAINT fk_payroll_result_assignment FOREIGN KEY(salary_assignment_id) REFERENCES employee_salary_assignment(id)
);
CREATE TABLE payroll_component_result (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, payroll_employee_result_id UUID NOT NULL,
 salary_component_id UUID NOT NULL, component_code VARCHAR(40) NOT NULL, component_name VARCHAR(160) NOT NULL,
 component_type VARCHAR(30) NOT NULL, amount NUMERIC(14,4) NOT NULL, sequence_number INTEGER NOT NULL,
 CONSTRAINT uq_payroll_result_component UNIQUE(tenant_id,payroll_employee_result_id,component_code),
 CONSTRAINT fk_component_result_employee FOREIGN KEY(payroll_employee_result_id,tenant_id) REFERENCES payroll_employee_result(id,tenant_id),
 CONSTRAINT fk_component_result_component FOREIGN KEY(salary_component_id,tenant_id) REFERENCES salary_component(id,tenant_id)
);
CREATE INDEX ix_payroll_run_tenant_period ON payroll_run(tenant_id,payroll_year,payroll_month);
CREATE INDEX ix_payroll_result_run ON payroll_employee_result(tenant_id,payroll_run_id);
