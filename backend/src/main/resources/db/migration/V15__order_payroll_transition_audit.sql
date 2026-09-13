ALTER TABLE payroll_run_transition
    ADD COLUMN transition_sequence BIGSERIAL NOT NULL UNIQUE;
