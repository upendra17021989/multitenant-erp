ALTER TABLE payroll_employee_result ADD COLUMN period_days NUMERIC(7,2) NOT NULL DEFAULT 0;
ALTER TABLE payroll_employee_result ADD COLUMN eligible_days NUMERIC(7,2) NOT NULL DEFAULT 0;
ALTER TABLE payroll_employee_result ADD COLUMN unpaid_leave_days NUMERIC(7,2) NOT NULL DEFAULT 0;
ALTER TABLE payroll_employee_result ADD COLUMN absent_days NUMERIC(7,2) NOT NULL DEFAULT 0;
ALTER TABLE payroll_employee_result ADD COLUMN payable_days NUMERIC(7,2) NOT NULL DEFAULT 0;
ALTER TABLE payroll_employee_result ADD COLUMN proration_factor NUMERIC(12,8) NOT NULL DEFAULT 1;
ALTER TABLE payroll_employee_result ADD COLUMN overtime_minutes INTEGER NOT NULL DEFAULT 0;
