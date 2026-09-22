ALTER TABLE payslip ADD COLUMN acknowledged_by VARCHAR(255);
ALTER TABLE payslip ADD COLUMN acknowledged_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE payslip ADD CONSTRAINT ck_payslip_acknowledgement
    CHECK ((acknowledged_by IS NULL AND acknowledged_at IS NULL)
        OR (acknowledged_by IS NOT NULL AND acknowledged_at IS NOT NULL AND status = 'RELEASED'));