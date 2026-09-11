CREATE TABLE payroll_setting (
 tenant_id UUID PRIMARY KEY REFERENCES tenant(id), pay_frequency VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
 attendance_cutoff_day INTEGER NOT NULL DEFAULT 25, payment_day INTEGER NOT NULL DEFAULT 7,
 working_day_basis VARCHAR(20) NOT NULL DEFAULT 'CALENDAR_DAYS', rounding_scale INTEGER NOT NULL DEFAULT 2,
 negative_salary_policy VARCHAR(20) NOT NULL DEFAULT 'HOLD', updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT ck_payroll_days CHECK(attendance_cutoff_day BETWEEN 1 AND 31 AND payment_day BETWEEN 1 AND 31),
 CONSTRAINT ck_working_basis CHECK(working_day_basis IN ('CALENDAR_DAYS','FIXED_30','PAYABLE_DAYS')),
 CONSTRAINT ck_negative_policy CHECK(negative_salary_policy IN ('HOLD','ZERO','ALLOW'))
);
CREATE TABLE salary_component (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenant(id), code VARCHAR(40) NOT NULL, name VARCHAR(160) NOT NULL,
 component_type VARCHAR(30) NOT NULL, calculation_type VARCHAR(20) NOT NULL, calculation_base_code VARCHAR(40),
 default_value NUMERIC(14,4) NOT NULL DEFAULT 0, taxable BOOLEAN NOT NULL DEFAULT TRUE,
 pf_applicable BOOLEAN NOT NULL DEFAULT FALSE, esi_applicable BOOLEAN NOT NULL DEFAULT FALSE,
 prorated BOOLEAN NOT NULL DEFAULT TRUE, included_in_gross BOOLEAN NOT NULL DEFAULT TRUE,
 included_in_ctc BOOLEAN NOT NULL DEFAULT TRUE, included_in_net BOOLEAN NOT NULL DEFAULT TRUE,
 rounding_scale INTEGER NOT NULL DEFAULT 2, effective_from DATE NOT NULL, effective_to DATE, status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT uq_salary_component_tenant_code_from UNIQUE(tenant_id,code,effective_from),
 CONSTRAINT uq_salary_component_id_tenant UNIQUE(id,tenant_id),
 CONSTRAINT ck_component_type CHECK(component_type IN ('EARNING','DEDUCTION','EMPLOYER_CONTRIBUTION')),
 CONSTRAINT ck_calculation_type CHECK(calculation_type IN ('FIXED','PERCENTAGE','FORMULA')),
 CONSTRAINT ck_component_dates CHECK(effective_to IS NULL OR effective_to>=effective_from),
 CONSTRAINT ck_component_status CHECK(status IN ('ACTIVE','INACTIVE'))
);
CREATE TABLE salary_structure (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenant(id), code VARCHAR(40) NOT NULL, name VARCHAR(160) NOT NULL,
 description VARCHAR(500), effective_from DATE NOT NULL, effective_to DATE, status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT uq_salary_structure_tenant_code_from UNIQUE(tenant_id,code,effective_from),
 CONSTRAINT uq_salary_structure_id_tenant UNIQUE(id,tenant_id),
 CONSTRAINT ck_structure_dates CHECK(effective_to IS NULL OR effective_to>=effective_from),
 CONSTRAINT ck_structure_status CHECK(status IN ('DRAFT','ACTIVE','INACTIVE'))
);
CREATE TABLE salary_structure_component (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenant(id), salary_structure_id UUID NOT NULL,
 salary_component_id UUID NOT NULL, sequence_number INTEGER NOT NULL, component_value NUMERIC(14,4), formula_expression VARCHAR(1000),
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT uq_structure_component UNIQUE(tenant_id,salary_structure_id,salary_component_id),
 CONSTRAINT uq_structure_sequence UNIQUE(tenant_id,salary_structure_id,sequence_number),
 CONSTRAINT fk_structure_line_structure FOREIGN KEY(salary_structure_id,tenant_id) REFERENCES salary_structure(id,tenant_id),
 CONSTRAINT fk_structure_line_component FOREIGN KEY(salary_component_id,tenant_id) REFERENCES salary_component(id,tenant_id)
);
CREATE TABLE employee_salary_assignment (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenant(id), employment_id UUID NOT NULL, salary_structure_id UUID NOT NULL,
 effective_from DATE NOT NULL, effective_to DATE, annual_ctc NUMERIC(14,2) NOT NULL, revision_reason VARCHAR(500),
 created_by VARCHAR(255) NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT uq_employee_salary_from UNIQUE(tenant_id,employment_id,effective_from),
 CONSTRAINT ck_assignment_dates CHECK(effective_to IS NULL OR effective_to>=effective_from),
 CONSTRAINT ck_assignment_ctc CHECK(annual_ctc>=0),
 CONSTRAINT fk_salary_employee FOREIGN KEY(employment_id,tenant_id) REFERENCES employment(id,tenant_id),
 CONSTRAINT fk_salary_structure FOREIGN KEY(salary_structure_id,tenant_id) REFERENCES salary_structure(id,tenant_id)
);
CREATE INDEX ix_employee_salary_current ON employee_salary_assignment(tenant_id,employment_id,effective_from,effective_to);
