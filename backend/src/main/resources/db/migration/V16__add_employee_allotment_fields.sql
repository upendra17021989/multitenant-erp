ALTER TABLE person
    ADD COLUMN title VARCHAR(30),
    ADD COLUMN marital_status VARCHAR(30),
    ADD COLUMN father_guardian_name VARCHAR(160);

ALTER TABLE employment
    ADD COLUMN ticket_number VARCHAR(40),
    ADD COLUMN retirement_date DATE,
    ADD COLUMN pf_joining_date DATE,
    ADD COLUMN pran VARCHAR(20),
    ADD COLUMN group_joining_date DATE,
    ADD COLUMN cc_email VARCHAR(320),
    ADD COLUMN division VARCHAR(160),
    ADD COLUMN unit VARCHAR(160),
    ADD COLUMN category VARCHAR(160),
    ADD COLUMN project VARCHAR(160);

