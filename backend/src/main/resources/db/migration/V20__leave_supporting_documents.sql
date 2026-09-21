ALTER TABLE employee_document ADD CONSTRAINT uq_employee_document_id_tenant_employee UNIQUE(id,tenant_id,employment_id);
ALTER TABLE leave_request ADD COLUMN supporting_document_id UUID;
ALTER TABLE leave_request ADD CONSTRAINT fk_leave_supporting_document FOREIGN KEY(supporting_document_id,tenant_id,employment_id) REFERENCES employee_document(id,tenant_id,employment_id);
