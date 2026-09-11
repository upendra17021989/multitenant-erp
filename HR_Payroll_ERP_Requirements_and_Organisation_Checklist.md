# HR & Payroll ERP â€” Requirements and Organisation Checklist

**Document purpose:** Collect and approve the business information required to design and build the first HR and Payroll module of the ERP.

**Recommended first release:** Organisation setup â†’ Employee master â†’ Attendance â†’ Leave â†’ Salary structure â†’ Monthly payroll â†’ Payslip â†’ Reports.

> This document should be completed jointly by HR, Payroll/Accounts, Management, IT, and the implementation team. Items marked **Required before development** should be finalized before related screens or calculations are built.

---

## 1. Project Overview

| Information required | Organisation response |
|---|---|
| Organisation/legal name | |
| Registered address | |
| Industry/business type | |
| Project sponsor/decision-maker | |
| HR process owner | |
| Payroll process owner | |
| IT/technical contact | |
| Accounts/finance contact | |
| Approximate employee count | |
| Expected number of system users | |
| Target go-live date | |
| Existing HR/payroll software | |
| Main reason for replacing/building the system | |
| Expected deployment: cloud/on-premises | |
| Single company or multiple legal entities | |

### Project goals

- [ ] Centralized employee records
- [ ] Attendance and shift management
- [x] Employee leave requests and approvals
- [ ] Automated monthly payroll
- [ ] Statutory deductions and reports
- [ ] Employee self-service portal
- [ ] PDF payslips
- [ ] Payroll/accounting export
- [ ] Management dashboards
- [ ] Other: ______________________________

### Success criteria

Define measurable outcomes, for example: payroll completed within two working days, zero manual salary calculations, or all employees able to download payslips.

| Success measure | Current value | Target value |
|---|---:|---:|
| Time required to process payroll | | |
| Number of manual Excel files | | |
| Average payroll corrections per month | | |
| Payslip delivery time | | |

---

## 2. Scope and Release Plan

### Phase 1 â€” Recommended MVP

- [ ] Login and password management
- [ ] Role-based access and permissions
- [ ] Company, branch, department and designation setup
- [ ] Employee master and document management
- [ ] Holiday, shift and attendance management
- [x] Leave types, balances, requests and approvals
- [ ] Salary components and salary structures
- [ ] Monthly payroll calculation and approval
- [ ] Payslip PDF generation and employee access
- [ ] Payroll, attendance and leave reports
- [ ] Audit history

### Phase 2 â€” Optional

- [ ] Biometric attendance integration
- [ ] Mobile attendance/geofencing
- [ ] Recruitment and onboarding workflow
- [ ] Expense and reimbursement claims
- [ ] Loans and salary advances
- [ ] Increment and promotion workflow
- [ ] Performance appraisal
- [ ] Training management
- [ ] Resignation, exit and full-and-final settlement
- [ ] Accounting integration
- [ ] Bank payment file/API integration
- [ ] Mobile application

### Explicitly out of scope for the first release

Record features that are not approved for the MVP:

______________________________________________________________________________

---

## 3. Organisation Structure

**Required before development**

Provide the current organisational hierarchy:

- Legal entities/companies
- Registered offices and work locations
- Branches
- Departments and sub-departments
- Designations/grades/bands
- Cost centres
- Reporting hierarchy
- Employee categories: permanent, probation, contractual, consultant, intern, etc.

| Master | Required fields/rules | Sample data provided? |
|---|---|---|
| Company | Legal name, code, PAN, TAN, GSTIN, PF/ESI registrations, address | [ ] |
| Branch/location | Code, name, address, state, PT/LWF applicability | [ ] |
| Department | Code, name, parent department, head | [ ] |
| Designation | Code, title, grade/band | [ ] |
| Cost centre | Code, name, accounting reference | [ ] |
| Holiday list | Location, year and holiday dates | [ ] |

Questions:

1. Can one employee work for more than one branch or cost centre? __________
2. Can employees transfer between branches/departments? __________
3. Should historical transfers and promotions be preserved? __________
4. Is the system for one organisation only or intended as a multi-company product? __________

---

## 3A. Multi-Tenancy and Three-Company Requirements

**Required before architecture and database development**

The client currently operates **three separate companies**. The ERP shall therefore support these companies as independent tenants/legal entities within one application, while allowing specifically authorized group-level users to work across selected companies.

### Core multi-tenant clause

> The system shall support multiple organisations as independent tenants. Each tenant's employees, attendance, leave, payroll, documents, statutory registrations, configurations and reports must remain logically isolated. A user must not be able to view or modify another tenant's data unless explicit cross-company access has been granted. Tenant-specific roles, salary rules, leave policies, statutory settings, branding and operational configurations shall be maintained independently.

### Current companies

| Tenant/company | Legal name | Company code | Separate statutory entity? | Approx. employees | Payroll owner |
|---|---|---|---:|---:|---|
| Company 1 | | | [ ] | | |
| Company 2 | | | [ ] | | |
| Company 3 | | | [ ] | | |

The solution must allow additional companies to be added later without changing the core application code or database design.

### Data isolation requirements

- Every tenant-owned record must carry an immutable `tenant_id` or equivalent company identifier.
- Employee, attendance, leave, salary, payroll, payslip, document and audit queries must always be restricted to the authorized tenant scope.
- Tenant filtering must be enforced in backend authorization and data access; it must not rely only on a company selector in the frontend.
- Uploaded employee documents and generated payslips must be stored in tenant-separated paths or storage boundaries.
- Cache entries, background jobs, imports, exports, notifications and generated reports must preserve tenant context.
- Database constraints and unique identifiers must be tenant-aware where appropriateâ€”for example, the same employee code may exist in different companies.
- Logs and audit records must record the acting user, selected tenant, affected tenant and action.
- Backups and restoration procedures must prevent one tenant's restored data from overwriting or exposing another tenant's data.
- Automated security tests must verify that users cannot access another tenant by changing a URL, request parameter, API payload or record ID.

### Company-specific configuration

Each company must be able to maintain its own:

- Legal name, registered address, logo and payslip branding
- PAN, TAN, GSTIN, PF, ESI, PT, LWF and other registrations, as applicable
- Branches, departments, designations, grades and cost centres
- Financial/payroll year and payroll processing calendar
- Employee-number and document-number formats
- Shifts, holidays, attendance, overtime and weekly-off rules
- Leave types, accruals, carry-forward and approval rules
- Salary components, structures, formulas and rounding rules
- Statutory rates and effective dates
- Payroll approval hierarchy and lock/reopen authority
- Payslip template, email sender and notification wording
- Bank account, payment file format and accounting mappings
- Data retention and document policies

### Users and cross-company access

| User type | Expected access |
|---|---|
| Tenant Administrator | Administration for one assigned company only |
| HR/Payroll User | Assigned company or explicitly selected companies only |
| Manager | Assigned employees within an authorized company only |
| Employee | Own records for the employing company only |
| Group Administrator | Configuration across approved companies |
| Group Management/Auditor | Approved consolidated or read-only multi-company reports |

The organisation must provide a **user-to-company access matrix**. Access must support:

- One user assigned to one company
- One user assigned to selected companies
- Different roles for the same user in different companies
- A deliberate company-switch action showing the active company clearly
- Optional group-level access without automatically granting salary-detail access
- Immediate revocation of company access when responsibilities change

### Employees working across companies

The organisation must decide and approve the following:

1. Can the same person be employed by more than one company at the same time? __________
2. If yes, should the person have separate employee records, employment contracts and salary structures? __________
3. Which company owns the employee's attendance and leave balance? __________
4. Can salary cost be allocated across companies or cost centres? __________
5. How should inter-company transfers be handled? __________
6. Should service history continue after a transfer, or should the old employment be closed and a new one created? __________
7. Can managers from one company approve records belonging to another company? __________

Recommended rule: maintain a shared person identity only when necessary, but create a separate employment record for each legal employer so payroll, statutory registration, documents and audit history remain company-specific.

### Company-wise and consolidated processing

- Payroll must be processed, approved, locked and reversed separately for each company.
- Failure or delay in one company's payroll must not block another company's payroll.
- Payslips, salary registers, bank files and statutory reports must be generated company-wise.
- Consolidated group reports may combine approved results from all three companies, but must retain company-level breakdowns.
- Consolidated access must be separately permissioned and must not bypass tenant-level salary confidentiality.
- Inter-company transfers and cost allocations must maintain an auditable history.
- Each report and export must display the company name/code and applied company filter.

### Tenant onboarding and lifecycle

The system should provide a controlled process to:

1. Create a new tenant/company
2. Configure statutory and payroll settings
3. Assign tenant administrators
4. Import or create employees
5. Validate opening balances and payroll rules
6. Activate payroll processing
7. Suspend a tenant without deleting its historical records
8. Export/archive tenant data according to approved retention rules

### Multi-tenant acceptance criteria

- [ ] Three companies can be configured and operated independently
- [x] Each company can have different leave rules; attendance and salary-rule configuration remains in progress
- [ ] Same employee/document codes can exist in different companies without conflict
- [ ] Company-scoped users cannot retrieve another company's data through UI or API
- [ ] Cross-company users see only companies explicitly assigned to them
- [ ] A user's role can differ by company
- [ ] Every screen clearly displays the active company when relevant
- [ ] Payroll runs and locks independently for each company
- [ ] Payslips, files, notifications and reports use the correct company branding and configuration
- [ ] Consolidated reports reconcile to the sum of approved company-wise reports
- [ ] Imports, exports, jobs, caches, documents and audit logs retain the correct tenant context
- [ ] Adding a fourth company requires configuration rather than code changes

Required inputs from the client:

- [ ] Legal and statutory details for all three companies
- [ ] Company-wise organisation structures and employee counts
- [ ] Company-wise HR, attendance, leave and payroll policies
- [ ] Company-wise salary structures and statutory applicability
- [ ] List of shared HR/Payroll/Management users and their company access
- [ ] Rules for employees working across or transferring between companies
- [ ] Required company-wise and consolidated reports
- [ ] Separate branding, payslip, bank and accounting requirements

---

## 4. Users, Roles and Approval Authority

Suggested roles:

- System Administrator
- Company Administrator
- HR Manager / HR Executive
- Payroll Manager / Payroll Executive
- Finance Approver
- Department Manager / Reporting Manager
- Employee
- Auditor / Read-only User

| Action | Employee | Manager | HR | Payroll | Finance | Admin |
|---|---:|---:|---:|---:|---:|---:|
| View own profile/payslip | | | | | | |
| Edit employee records | | | | | | |
| Approve leave | | | | | | |
| Correct attendance | | | | | | |
| Create/change salary | | | | | | |
| Process payroll | | | | | | |
| Approve payroll | | | | | | |
| Release payslips | | | | | | |
| View organisation-wide salary | | | | | | |
| Export bank/statutory reports | | | | | | |

Organisation must clarify:

- Who can see salary information?
- Who can change salary structures?
- Does salary revision require maker-checker approval?
- Who approves attendance corrections and leave?
- Who approves and locks monthly payroll?
- Can HR and Payroll roles be held by different people?
- Which actions require a reason/comment?

---

## 5. Employee Master Data

### Required employee fields

- Employee number and employment status
- Full name, date of birth, gender and contact details
- Current and permanent address
- Joining date, confirmation date and probation period
- Company, branch, department, designation, grade and cost centre
- Employment type and reporting manager
- Work email and personal email
- Bank account name, number, bank, branch and IFSC
- PAN, Aadhaar (only if legally/operationally required), UAN, PF and ESI details
- Nominee/emergency contact
- Salary/payment mode
- Exit date and reason, when applicable

### Employee documents

- [ ] Photograph
- [ ] Appointment/offer letter
- [ ] Identity proof
- [ ] Address proof
- [ ] PAN
- [ ] Bank proof/cancelled cheque
- [ ] Education certificates
- [ ] Previous employment documents
- [ ] Signed policies/forms
- [ ] Other: ______________________________

Decide which fields are mandatory, optional, confidential, editable by employees, or editable only by HR.

### Employee numbering

| Decision | Organisation response |
|---|---|
| Existing employee-code format | |
| Auto-generated or manually entered | |
| Separate series per branch/company | |
| Should old employee codes be preserved | |

---

## 6. Attendance, Shifts and Overtime

**Required before attendance and payroll calculation development**

### Attendance source

- [ ] Manual entry
- [ ] Excel/CSV import
- [ ] Biometric device
- [ ] Mobile/web check-in
- [ ] Third-party attendance API
- [ ] Combination of the above

### Information required

- Shift names, start/end times and break duration
- Grace period and late-arrival/early-exit rules
- Minimum hours for full day and half day
- Weekly-off rules
- Overnight/rotational shift handling
- Work-from-home and on-duty rules
- Missing punch and attendance-correction workflow
- Overtime eligibility, approval and rate formula
- Compensatory-off rules
- Attendance-lock date and who can reopen a locked month
- Treatment of holidays/week-offs when adjacent leave is taken

| Rule | Organisation response |
|---|---|
| Payroll attendance period | |
| Full-day required hours | |
| Half-day required hours | |
| Late marks allowed | |
| Late-mark salary/leave impact | |
| Overtime formula | |
| Rounding rules | |
| Attendance approval authority | |

Required samples:

- [ ] Shift list
- [ ] Holiday calendar
- [ ] At least two months of attendance data
- [ ] Biometric export/API documentation, if applicable
- [ ] Existing overtime sheet and calculation examples

---

## 7. Leave Management

For every leave type, collect:

| Rule | Example/response |
|---|---|
| Leave name and code | Casual Leave / CL |
| Paid or unpaid | |
| Annual entitlement | |
| Monthly/annual accrual | |
| Eligibility and waiting period | |
| Minimum/maximum request | |
| Half-day allowed | |
| Carry-forward limit | |
| Encashment rule | |
| Negative balance allowed | |
| Applicable employee categories | |
| Supporting document required | |
| Approval levels | |
| Sandwich/club-leave rule | |
| Balance on resignation | |

Additional questions:

- Is leave year calendar year, financial year, or joining-date based?
- Are leave balances credited monthly, quarterly, or annually?
- Can managers approve backdated leave?
- Does unpaid leave automatically reduce salary?
- How are maternity, paternity, bereavement, compensatory and special leave handled?

Required samples:

- [ ] Leave policy
- [ ] Current employee leave balances
- [ ] Leave request/approval format
- [ ] Examples of special cases

---

### Implementation status (2026-09-11)

Completed and verified:

- [x] Tenant-scoped leave type configuration
- [x] Paid/unpaid, entitlement, accrual-frequency, request limits, half-day, carry-forward-limit, negative-balance and supporting-document policy fields
- [x] Employee yearly opening, accrued, adjustment, used and available balances
- [x] Leave request submission with date, limit, overlap and available-balance validation
- [x] HR approval/rejection and pending-request cancellation APIs
- [x] Approved leave deducts the employee balance transactionally
- [x] Leave requests, balances and types are isolated by company
- [x] Leave administration frontend for types, balances, requests and decisions
- [x] Automated tests covering approval, balance use, invalid requests and tenant isolation

Still pending in the leave phase:

- [ ] Automatic monthly/quarterly/annual accrual jobs
- [ ] Year-end carry-forward and encashment processing
- [ ] Eligibility/waiting-period and employee-category rules
- [ ] Supporting-document upload and mandatory-document enforcement
- [ ] Configurable multi-level manager/HR approval chains
- [ ] Backdated approval, sandwich/club-leave and resignation rules
- [ ] Maternity, paternity, bereavement, compensatory-off and other special workflows
- [ ] Approved unpaid leave integration with payroll
- [ ] Employee self-service restriction to the authenticated employee record
- [ ] Leave reports, notifications and full audit/change history

---
## 8. Salary Structure and Payroll Rules

**Required before payroll development**

### Pay period and processing

| Decision | Organisation response |
|---|---|
| Salary month | Calendar month / other |
| Attendance cut-off | |
| Payroll processing date | |
| Salary payment date | |
| Working-day basis | Calendar days / fixed days / payable days |
| Rounding method | |
| Arrears handling | |
| Negative salary handling | |
| Payroll locking/reopening authority | |

### Earnings

- [ ] Basic salary
- [ ] HRA
- [ ] Special allowance
- [ ] Conveyance allowance
- [ ] Medical allowance
- [ ] Bonus
- [ ] Incentive/commission
- [ ] Overtime
- [ ] Arrears
- [ ] Reimbursement
- [ ] Other: ______________________________

### Deductions and employer contributions

- [ ] Employee PF
- [ ] Employer PF/EPS/EDLI-related components, as applicable
- [ ] Employee ESI
- [ ] Employer ESI
- [ ] Professional Tax
- [ ] TDS
- [ ] Labour Welfare Fund
- [ ] Loan/advance recovery
- [ ] Notice-pay recovery
- [ ] Other: ______________________________

For each salary component, provide:

| Information | Required detail |
|---|---|
| Component name/code | |
| Earning, deduction or employer contribution | |
| Fixed amount, percentage or formula | |
| Calculation base | |
| Taxable/non-taxable treatment | |
| PF/ESI applicability | |
| Prorated by payable days? | |
| Included in gross/CTC/net pay? | |
| Rounding rule | |
| Effective-from/effective-to dates | |

### Mandatory calculation examples

The organisation should provide approved manual calculations for at least:

1. Full-month salary
2. New joiner during the month
3. Employee leaving during the month
4. Employee with unpaid leave
5. Overtime/incentive
6. Arrears or revised salary
7. PF-applicable employee
8. ESI-applicable employee
9. TDS deduction
10. Loan/advance deduction

These examples will be used as payroll acceptance tests.

---

## 9. Indian Statutory and Compliance Requirements

The organisation's HR/Payroll/CA or compliance advisor must confirm the rules applicable to its legal entities and locations. The development team should not assume statutory applicability.

Collect:

- PF establishment and employee rules
- ESI registration and applicability
- Professional Tax rules for every state/location
- Labour Welfare Fund applicability
- TDS calculation method and declaration/proof workflow
- Bonus, gratuity, leave encashment and other applicable policies
- Required statutory exports, challan-supporting reports and returns
- Required retention period for payroll and employee records
- Current compliance formats and calculation examples

> Rates, thresholds and regulations can change. Keep statutory rates and effective dates configurable, and have calculations validated by the organisation's authorized payroll/compliance professional before production use.

---

## 10. Payroll Workflow and Controls

Confirm the desired monthly workflow:

1. Attendance and leave cut-off
2. Attendance review and lock
3. Variable earnings/deductions import
4. Draft payroll calculation
5. Exception review
6. Payroll approval
7. Final payroll lock
8. Payslip release
9. Bank-payment output
10. Accounting/statutory export

### Payroll statuses

Recommended: `DRAFT` â†’ `CALCULATED` â†’ `UNDER_REVIEW` â†’ `APPROVED` â†’ `LOCKED` â†’ `PAID`.

Questions:

- Can approved payroll be cancelled or only reversed/reprocessed?
- Can one employee be recalculated without reopening the full payroll?
- Who can view payroll before approval?
- When should payslips become visible?
- Is an employee acknowledgement required?
- What exception reports are required before approval?

---

## 11. Payslip and Document Requirements

Provide:

- Organisation logo
- Registered name and address
- Payslip sample/template
- Required earnings/deduction columns
- CTC, leave balance, attendance and bank-details visibility rules
- Authorized signatory requirement
- File naming and password-protection rules
- Preferred language(s)
- Email wording, sender address and delivery method

- [ ] Payslip available in employee portal
- [ ] Payslip emailed to employee
- [ ] Bulk ZIP/PDF download for Payroll team
- [ ] Password-protected PDF
- [ ] Digital signature required

---

## 12. Reports, Dashboards and Exports

### Minimum recommended reports

- Employee master and headcount
- New joiners, confirmations and exits
- Attendance summary and exceptions
- Leave balances and leave transactions
- Salary register
- Component-wise payroll report
- Department/branch/cost-centre salary report
- Deduction and employer-contribution report
- Bank transfer statement/file
- Payslip register
- Payroll variance report against previous month
- Statutory deduction reports
- Audit/change history

For each report, specify:

| Requirement | Organisation response |
|---|---|
| Report name/purpose | |
| Required columns and totals | |
| Filters | |
| Who can access it | |
| PDF/Excel/CSV requirement | |
| Scheduled email required | |
| Sample report provided | [ ] |

### Dashboard KPIs

- [ ] Total and active employees
- [ ] Attendance today
- [ ] Employees on leave
- [ ] Pending approvals
- [ ] Monthly payroll cost
- [ ] Overtime cost
- [ ] Upcoming confirmations/birthdays/work anniversaries
- [ ] Custom KPI: ______________________________

---

## 13. Integrations

| Integration | Needed now/later | Provider/system | Documentation/contact |
|---|---|---|---|
| Biometric attendance | | | |
| Email | | | |
| SMS/WhatsApp | | | |
| Bank payment file/API | | | |
| Accounting/ERP | | | |
| Active Directory/Google/Microsoft login | | | |
| Job/recruitment portal | | | |
| Other | | | |

For every integration, collect API documentation, sample files, credentials for a non-production environment, frequency, error-handling expectations and system owner.

---

## 14. Existing Data and Migration

### Data to be provided

- [ ] Active employee master
- [ ] Inactive/ex-employee master, if history is required
- [ ] Salary structures and revision history
- [ ] Current leave balances
- [ ] Attendance history
- [ ] Payroll history
- [ ] Loans/advances and outstanding balances
- [ ] Employee documents
- [ ] Department/designation/branch masters
- [ ] Statutory identifiers

Decisions:

| Question | Organisation response |
|---|---|
| Migration cut-off date | |
| Number of historical years to migrate | |
| Who will clean and approve source data | |
| How duplicate/missing records will be handled | |
| Who signs off migrated totals | |

Use masked or synthetic data during early development wherever possible. Transfer real employee and payroll data only through an approved secure process.

---

## 15. Security, Privacy and Audit

Confirm requirements for:

- Role and branch/company-level data access
- Separation of HR and payroll privileges
- Encryption in transit and at rest
- Password policy and optional multi-factor authentication
- Session timeout
- Audit log for employee, attendance, leave and salary changes
- Masking bank account, PAN and other sensitive fields
- Backup frequency and restore testing
- Data retention and employee-exit handling
- Export/download restrictions
- Production support access
- Security incident reporting

Important audit fields should include created/updated/approved by and date, old value, new value, change reason, and source/import reference.

Financial and payroll records should not be hard-deleted after approval. Use cancellation, correction or versioned effective dates with a complete audit trail.

---

## 16. Technical and Operational Requirements

| Requirement | Organisation response |
|---|---|
| Expected concurrent users | |
| Supported devices | Desktop / tablet / mobile |
| Supported browsers | |
| Expected availability | |
| Maximum acceptable response time | |
| Cloud/on-premises preference | |
| Required region/data location | |
| Backup frequency and retention | |
| Disaster-recovery expectation | |
| Environments required | Dev / test / UAT / production |
| Existing domain/email provider | |
| Internal IT support available | |

Recommended initial architecture: **React + TypeScript + MUI frontend, Spring Boot modular-monolith backend, PostgreSQL database, object storage for employee documents, and background jobs for payroll/report generation.**

Suggested backend modules: `auth`, `organization`, `employee`, `attendance`, `leave`, `payroll`, `approval`, `document`, `reporting`, `notification`, and `audit`.

---

## 17. Non-Functional Acceptance Requirements

- [ ] Authorized users can access only permitted employee/salary data
- [ ] All important changes are auditable
- [ ] Payroll calculations are reproducible after locking
- [ ] Reports reconcile with approved payroll totals
- [ ] Exports work for the expected employee volume
- [ ] System has documented backup and restore procedures
- [ ] Common screens work on agreed browsers/devices
- [ ] Sensitive information is masked where required
- [ ] Performance and availability targets are met

---

## 18. User Acceptance Testing and Sign-Off

The organisation should nominate users from HR, Payroll, Finance, Management, Managers and Employees.

### Minimum UAT scenarios

- Employee creation, transfer, confirmation and exit
- Attendance import, missing punch and correction
- Leave request, multi-level approval and unpaid leave
- Salary assignment and revision with effective date
- All approved payroll calculation examples from Section 8
- Payroll approval, locking and authorized correction
- Payslip generation and access control
- Reports, exports and month-to-month reconciliation
- Permission and unauthorized-access testing
- Data migration reconciliation

| Sign-off area | Owner | Target date | Approved? |
|---|---|---|---|
| Organisation structure/master data | | | [ ] |
| Attendance and leave rules | | | [ ] |
| Salary and statutory rules | | | [ ] |
| Roles and approvals | | | [ ] |
| Reports and payslip | | | [ ] |
| Migrated data | | | [ ] |
| UAT and go-live | | | [ ] |

---

## 19. Required Inputs â€” Quick Handover Checklist

Ask the organisation to provide these before estimation and development are finalized:

### Mandatory

- [ ] Organisation profile and legal-entity details
- [ ] Legal, statutory and payroll configuration for all three companies
- [ ] User-to-company role and access matrix
- [ ] Cross-company employment, transfer and consolidated-reporting rules
- [ ] Branch, department, designation, grade and cost-centre lists
- [ ] Employee master sample and required employee fields
- [ ] HR policy and payroll policy
- [ ] Attendance, shift, overtime and holiday rules
- [ ] Leave types, balances, accrual and approval rules
- [ ] Salary components, formulas and salary-structure samples
- [ ] Ten approved payroll calculation examples
- [ ] PF/ESI/PT/LWF/TDS applicability confirmed by authorized personnel
- [ ] Current payslip and payroll-register samples
- [ ] User roles, permissions and approval matrix
- [ ] Required reports and sample formats
- [ ] Existing data format and migration scope
- [ ] Named HR, Payroll, Finance and Management sign-off owners

### If applicable

- [ ] Biometric/API documentation and sample export
- [ ] Bank payment file specification
- [ ] Accounting integration format/API
- [ ] Email/SMS/WhatsApp configuration requirements
- [ ] Employee document templates
- [ ] Security, hosting and data-residency policy

---

## 20. Open Questions and Decision Log

| ID | Question/decision | Owner | Due date | Decision/status |
|---|---|---|---|---|
| 1 | | | | |
| 2 | | | | |
| 3 | | | | |
| 4 | | | | |
| 5 | | | | |

---

## 21. Change Control

After scope approval, every new request should record:

- Requested change and business reason
- Requested by and date
- MVP or later-phase classification
- Impact on screens, calculations, reports, migration, security and integrations
- Estimated effort/cost and schedule impact
- Approval/rejection and approver

This prevents payroll-critical requirements from being changed informally during development.

---

## 22. Final Approval

By approving this document, stakeholders confirm that the recorded rules and samples represent the organisation's intended HR and payroll processes. Statutory calculations must receive separate validation from the organisation's authorized payroll/compliance professional before go-live.

| Role | Name | Signature/approval | Date |
|---|---|---|---|
| Project Sponsor | | | |
| HR Owner | | | |
| Payroll/Finance Owner | | | |
| IT Owner | | | |
| Implementation Lead | | | |
