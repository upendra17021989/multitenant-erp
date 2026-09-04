CREATE TABLE work_shift (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    code VARCHAR(40) NOT NULL,
    name VARCHAR(160) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    break_minutes INTEGER NOT NULL DEFAULT 0,
    grace_in_minutes INTEGER NOT NULL DEFAULT 0,
    grace_out_minutes INTEGER NOT NULL DEFAULT 0,
    full_day_minutes INTEGER NOT NULL,
    half_day_minutes INTEGER NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_work_shift_tenant_code_from UNIQUE (tenant_id, code, effective_from),
    CONSTRAINT uq_work_shift_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT ck_work_shift_minutes CHECK (break_minutes >= 0 AND grace_in_minutes >= 0 AND grace_out_minutes >= 0 AND full_day_minutes > 0 AND half_day_minutes > 0 AND half_day_minutes <= full_day_minutes),
    CONSTRAINT ck_work_shift_dates CHECK (effective_to IS NULL OR effective_to >= effective_from),
    CONSTRAINT ck_work_shift_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
CREATE INDEX ix_work_shift_tenant_effective ON work_shift (tenant_id, effective_from, effective_to);

CREATE TABLE employee_shift_assignment (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    employment_id UUID NOT NULL,
    shift_id UUID NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_employee_shift_tenant_employee_from UNIQUE (tenant_id, employment_id, effective_from),
    CONSTRAINT ck_employee_shift_dates CHECK (effective_to IS NULL OR effective_to >= effective_from),
    CONSTRAINT fk_employee_shift_employment_same_tenant FOREIGN KEY (employment_id, tenant_id) REFERENCES employment(id, tenant_id),
    CONSTRAINT fk_employee_shift_shift_same_tenant FOREIGN KEY (shift_id, tenant_id) REFERENCES work_shift(id, tenant_id)
);
CREATE INDEX ix_employee_shift_lookup ON employee_shift_assignment (tenant_id, employment_id, effective_from, effective_to);

CREATE TABLE attendance_record (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    employment_id UUID NOT NULL,
    attendance_date DATE NOT NULL,
    shift_id UUID,
    status VARCHAR(30) NOT NULL,
    check_in TIMESTAMPTZ,
    check_out TIMESTAMPTZ,
    worked_minutes INTEGER,
    overtime_minutes INTEGER NOT NULL DEFAULT 0,
    source VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    notes VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_attendance_tenant_employee_date UNIQUE (tenant_id, employment_id, attendance_date),
    CONSTRAINT ck_attendance_status CHECK (status IN ('PRESENT','ABSENT','HALF_DAY','LEAVE','HOLIDAY','WEEKLY_OFF','WORK_FROM_HOME','ON_DUTY','MISSING_PUNCH')),
    CONSTRAINT ck_attendance_source CHECK (source IN ('MANUAL','IMPORT','BIOMETRIC','WEB','MOBILE','API')),
    CONSTRAINT ck_attendance_minutes CHECK ((worked_minutes IS NULL OR worked_minutes >= 0) AND overtime_minutes >= 0),
    CONSTRAINT ck_attendance_punches CHECK (check_out IS NULL OR (check_in IS NOT NULL AND check_out >= check_in)),
    CONSTRAINT fk_attendance_employment_same_tenant FOREIGN KEY (employment_id, tenant_id) REFERENCES employment(id, tenant_id),
    CONSTRAINT fk_attendance_shift_same_tenant FOREIGN KEY (shift_id, tenant_id) REFERENCES work_shift(id, tenant_id)
);
CREATE INDEX ix_attendance_tenant_date ON attendance_record (tenant_id, attendance_date);

CREATE TABLE attendance_month_lock (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    attendance_month DATE NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    locked_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reopen_reason VARCHAR(500),
    CONSTRAINT uq_attendance_month_lock UNIQUE (tenant_id, attendance_month),
    CONSTRAINT ck_attendance_month_first_day CHECK (EXTRACT(DAY FROM attendance_month) = 1)
);
