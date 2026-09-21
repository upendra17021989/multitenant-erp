ALTER TABLE tenant ADD COLUMN time_zone VARCHAR(80) NOT NULL DEFAULT 'Asia/Kolkata';
ALTER TABLE attendance_record ADD COLUMN approved_overtime_minutes INTEGER NOT NULL DEFAULT 0;
ALTER TABLE attendance_record ADD CONSTRAINT ck_approved_overtime CHECK (approved_overtime_minutes BETWEEN 0 AND overtime_minutes);
ALTER TABLE attendance_record ADD CONSTRAINT uq_attendance_id_tenant UNIQUE(id,tenant_id);

CREATE TABLE attendance_review (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    attendance_id UUID NOT NULL,
    review_type VARCHAR(20) NOT NULL CHECK(review_type IN ('CORRECTION','OVERTIME')),
    original_record TEXT NOT NULL,
    proposed_check_in TIMESTAMPTZ,
    proposed_check_out TIMESTAMPTZ,
    overtime_minutes INTEGER NOT NULL DEFAULT 0 CHECK(overtime_minutes>=0),
    reason VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','APPROVED','REJECTED')),
    requested_by VARCHAR(255) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_by VARCHAR(255),
    decided_at TIMESTAMPTZ,
    decision_comment VARCHAR(1000),
    FOREIGN KEY(attendance_id,tenant_id) REFERENCES attendance_record(id,tenant_id)
);
CREATE UNIQUE INDEX uq_pending_attendance_review ON attendance_review(tenant_id,attendance_id,review_type) WHERE status='PENDING';
CREATE INDEX ix_attendance_review_tenant ON attendance_review(tenant_id,status,requested_at);
