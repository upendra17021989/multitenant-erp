CREATE TABLE statutory_rule (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenant(id), code VARCHAR(40) NOT NULL,
 name VARCHAR(160) NOT NULL, result_type VARCHAR(30) NOT NULL, basis_type VARCHAR(20) NOT NULL,
 base_component_codes VARCHAR(1000), rate_percent NUMERIC(9,4) NOT NULL,
 eligibility_ceiling NUMERIC(14,2), contribution_ceiling NUMERIC(14,2), rounding_scale INTEGER NOT NULL DEFAULT 0,
 effective_from DATE NOT NULL, effective_to DATE, status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT uq_statutory_rule_tenant_code_from UNIQUE(tenant_id,code,effective_from),
 CONSTRAINT uq_statutory_rule_id_tenant UNIQUE(id,tenant_id),
 CONSTRAINT ck_statutory_result_type CHECK(result_type IN ('DEDUCTION','EMPLOYER_CONTRIBUTION')),
 CONSTRAINT ck_statutory_basis_type CHECK(basis_type IN ('GROSS','COMPONENTS')),
 CONSTRAINT ck_statutory_values CHECK(rate_percent>=0 AND (eligibility_ceiling IS NULL OR eligibility_ceiling>=0) AND (contribution_ceiling IS NULL OR contribution_ceiling>=0) AND rounding_scale BETWEEN 0 AND 4),
 CONSTRAINT ck_statutory_dates CHECK(effective_to IS NULL OR effective_to>=effective_from),
 CONSTRAINT ck_statutory_status CHECK(status IN ('DRAFT','ACTIVE','INACTIVE'))
);
CREATE TABLE payroll_variable_input (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenant(id), employment_id UUID NOT NULL,
 payroll_year INTEGER NOT NULL, payroll_month INTEGER NOT NULL, code VARCHAR(40) NOT NULL, name VARCHAR(160) NOT NULL,
 input_type VARCHAR(20) NOT NULL, amount NUMERIC(14,2) NOT NULL, notes VARCHAR(500), created_by VARCHAR(255) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT uq_variable_input UNIQUE(tenant_id,employment_id,payroll_year,payroll_month,code),
 CONSTRAINT fk_variable_employee FOREIGN KEY(employment_id,tenant_id) REFERENCES employment(id,tenant_id),
 CONSTRAINT ck_variable_period CHECK(payroll_year BETWEEN 2000 AND 2200 AND payroll_month BETWEEN 1 AND 12),
 CONSTRAINT ck_variable_type CHECK(input_type IN ('EARNING','DEDUCTION')),
 CONSTRAINT ck_variable_amount CHECK(amount>=0)
);
CREATE TABLE payroll_adjustment_result (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, payroll_employee_result_id UUID NOT NULL,
 source_type VARCHAR(20) NOT NULL, source_id UUID NOT NULL, code VARCHAR(40) NOT NULL, name VARCHAR(160) NOT NULL,
 result_type VARCHAR(30) NOT NULL, amount NUMERIC(14,2) NOT NULL,
 CONSTRAINT uq_payroll_adjustment UNIQUE(tenant_id,payroll_employee_result_id,source_type,source_id),
 CONSTRAINT fk_adjustment_result_employee FOREIGN KEY(payroll_employee_result_id,tenant_id) REFERENCES payroll_employee_result(id,tenant_id),
 CONSTRAINT ck_adjustment_source CHECK(source_type IN ('STATUTORY','VARIABLE')),
 CONSTRAINT ck_adjustment_result_type CHECK(result_type IN ('EARNING','DEDUCTION','EMPLOYER_CONTRIBUTION'))
);
CREATE INDEX ix_statutory_rule_effective ON statutory_rule(tenant_id,effective_from,effective_to);
CREATE INDEX ix_variable_input_period ON payroll_variable_input(tenant_id,payroll_year,payroll_month);
