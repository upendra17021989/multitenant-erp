CREATE TABLE person (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    first_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE,
    gender VARCHAR(30),
    personal_email VARCHAR(320),
    mobile_number VARCHAR(30),
    current_address TEXT,
    permanent_address TEXT,
    emergency_contact_name VARCHAR(160),
    emergency_contact_phone VARCHAR(30),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_person_id_tenant UNIQUE (id, tenant_id)
);

CREATE INDEX ix_person_tenant_name ON person (tenant_id, last_name, first_name);

ALTER TABLE designation ADD CONSTRAINT uq_designation_id_tenant UNIQUE (id, tenant_id);
ALTER TABLE cost_centre ADD CONSTRAINT uq_cost_centre_id_tenant UNIQUE (id, tenant_id);

CREATE TABLE employment (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    person_id UUID NOT NULL,
    employee_number VARCHAR(40) NOT NULL,
    employment_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    employment_type VARCHAR(30) NOT NULL,
    joining_date DATE NOT NULL,
    confirmation_date DATE,
    probation_end_date DATE,
    exit_date DATE,
    exit_reason VARCHAR(500),
    work_email VARCHAR(320),
    branch_id UUID,
    department_id UUID,
    designation_id UUID,
    grade_id UUID,
    cost_centre_id UUID,
    reporting_manager_employment_id UUID,
    payment_mode VARCHAR(20),
    bank_account_name VARCHAR(160),
    bank_account_number VARCHAR(50),
    bank_name VARCHAR(160),
    bank_branch VARCHAR(160),
    bank_ifsc VARCHAR(11),
    pan VARCHAR(10),
    aadhaar_last_four VARCHAR(4),
    uan VARCHAR(12),
    pf_number VARCHAR(40),
    esi_number VARCHAR(30),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_employment_tenant_number UNIQUE (tenant_id, employee_number),
    CONSTRAINT uq_employment_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT ck_employment_status CHECK (employment_status IN ('ACTIVE', 'PROBATION', 'NOTICE', 'INACTIVE', 'EXITED')),
    CONSTRAINT ck_employment_type CHECK (employment_type IN ('PERMANENT', 'PROBATION', 'CONTRACT', 'CONSULTANT', 'INTERN')),
    CONSTRAINT ck_payment_mode CHECK (payment_mode IS NULL OR payment_mode IN ('BANK_TRANSFER', 'CHEQUE', 'CASH')),
    CONSTRAINT ck_aadhaar_last_four CHECK (aadhaar_last_four IS NULL OR aadhaar_last_four ~ '^[0-9]{4}$'),
    CONSTRAINT ck_employment_dates CHECK (confirmation_date IS NULL OR confirmation_date >= joining_date),
    CONSTRAINT ck_exit_date CHECK (exit_date IS NULL OR exit_date >= joining_date),
    CONSTRAINT fk_employment_person_same_tenant FOREIGN KEY (person_id, tenant_id) REFERENCES person(id, tenant_id),
    CONSTRAINT fk_employment_branch_same_tenant FOREIGN KEY (branch_id, tenant_id) REFERENCES branch(id, tenant_id),
    CONSTRAINT fk_employment_department_same_tenant FOREIGN KEY (department_id, tenant_id) REFERENCES department(id, tenant_id),
    CONSTRAINT fk_employment_designation_same_tenant FOREIGN KEY (designation_id, tenant_id) REFERENCES designation(id, tenant_id),
    CONSTRAINT fk_employment_grade_same_tenant FOREIGN KEY (grade_id, tenant_id) REFERENCES grade(id, tenant_id),
    CONSTRAINT fk_employment_cost_centre_same_tenant FOREIGN KEY (cost_centre_id, tenant_id) REFERENCES cost_centre(id, tenant_id),
    CONSTRAINT fk_employment_manager_same_tenant FOREIGN KEY (reporting_manager_employment_id, tenant_id) REFERENCES employment(id, tenant_id)
);

CREATE INDEX ix_employment_tenant_status ON employment (tenant_id, employment_status);
CREATE INDEX ix_employment_tenant_person ON employment (tenant_id, person_id);
