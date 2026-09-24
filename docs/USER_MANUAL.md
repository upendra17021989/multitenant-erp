# Amar Group ERP — User Manual

Version 1.0 · 24 September 2026

For employees, reporting managers, HR, payroll teams, and company administrators.

This guide describes the screens implemented in the current application. Availability depends on your company access, assigned roles, and the version deployed by your administrator. Screen labels are shown in **bold**. The guide was checked against the application source; live deployment acceptance testing was not part of its preparation.

## Contents

1. [Sign in and choose your company](#1-sign-in-and-choose-your-company)
2. [Navigation and access](#2-navigation-and-access)
3. [Employee quick start](#3-employee-quick-start)
4. [Account administration](#4-account-administration)
5. [Organisation masters](#5-organisation-masters)
6. [Employee master and documents](#6-employee-master-and-documents)
7. [Attendance, shifts, and approvals](#7-attendance-shifts-and-approvals)
8. [Leave administration and approvals](#8-leave-administration-and-approvals)
9. [Salary setup and payslips](#9-salary-setup-and-payslips)
10. [Expenses, loans, and advances](#10-expenses-loans-and-advances)
11. [HR workflows and exit](#11-hr-workflows-and-exit)
12. [Dashboard, reports, and exports](#12-dashboard-reports-and-exports)
13. [Operating checklists](#13-operating-checklists)
14. [Troubleshooting](#14-troubleshooting)

## 1. Sign in and choose your company

### First-time activation

1. Ask your company administrator to invite you using your correct email address.
2. Open the invitation email and follow its activation link.
3. On **Set your password**, enter and confirm a password of at least eight characters.
4. Select **Activate account**.
5. Confirm that the company shown at the top of the application is the company you intend to work in.

An employee record and a login account are separate. Your administrator must link them before you can use employee self-service.

### Returning users

1. Open the site address supplied by your organisation.
2. Enter **Email** and **Password**, then select **Sign in**.
3. Use the company selector in the top bar to choose an authorised company.
4. Select a module from the left sidebar.

The company selector lists only companies assigned to your account. Changing it reloads the workspace for that company. Finish and save your current form before switching companies; unsaved entries may be lost.

Select **Sign out** in the top bar when finished. If you forget your password or an invitation link fails, contact your administrator; the current sign-in screen has no password-reset button.

## 2. Navigation and access

| Menu | Purpose |
|---|---|
| My Leave & Payslips | View your leave balances, request leave, download payslips, and acknowledge receipt. |
| Leave Approvals & Notifications | Decide assigned leave stages, view notifications, and configure approval chains where authorised. |
| Dashboard | View company workforce indicators; users without management access see self-service. |
| Employee Master | Maintain employees, documents, and self-service links. |
| Attendance & Muster | Maintain shifts, attendance, imports, corrections, and overtime reviews. |
| Leave Management | Maintain requests, balances, leave types, and accrual processing. |
| Salary & Payroll | Configure salaries and manage payslip generation and release. |
| Expenses, Loans & Advances | Submit financial requests and record approvals and external payments. |
| HR Workflows & Exit | Manage employee lifecycle checklists and approvals. |
| Organisation Masters | Maintain company information and organisation reference records. |
| Account Administration | Invite accounts and enable or disable access. |
| Reports & Exports | View reports and download CSV files. |

The **PF / ESIC / PT**, **CLRA & Licences**, and **Data Quality** sidebar entries are currently disabled placeholders.

### Understanding roles

| Role or responsibility | Access guidance |
|---|---|
| Employee | Uses linked employee self-service and financial requests. |
| Reporting manager | Sees leave requests assigned to their reporting-manager stage; their employee and account links must be configured. |
| HR Manager | Can access management screens and maintain HR records; individual operations remain role-controlled. |
| Payroll Manager | Can access management screens and perform authorised payroll and financial operations. |
| Company Administrator | Manages company setup and accounts, with administrative access to company workflows. |
| Group / System Administrator | Works within the companies authorised for the account; these roles cannot be granted from Account Administration. |
| HR Executive / Payroll Executive | Assignable roles, but the current sidebar restricts several management modules to manager or administrator roles. Ask your administrator if a required screen is disabled. |

A visible page does not grant permission for every action on it. Read-only messages, disabled controls, and server permission errors reflect the active company's permissions. The top bar shows one role label even when an account has several roles.

## 3. Employee quick start

### Request leave

1. Open **My Leave & Payslips** and review **Leave balances**.
2. Under **Request leave**, select a leave type and enter start and end dates.
3. Enter **Days** in half-day increments and provide a reason. Confirm the number of days against company policy; it is entered separately from the date range.
4. Attach a supporting document if required by the leave type. Maximum file size is 10 MB.
5. Select **Submit request**.
6. Check **My requests** for the status and **Approval history** for stage decisions.

A request remains **PENDING** until all approval stages finish. You can select **Cancel** while it is pending. Contact HR about changes to an already approved request.

### Download and acknowledge a payslip

1. Open **My Leave & Payslips** and locate **Released payslips**.
2. Select the payslip's download button for the required period.
3. Open the downloaded PDF and review it.
4. Select **Acknowledge receipt** to record receipt. The first acknowledgement timestamp is retained.

Acknowledgement confirms receipt, not agreement with the calculation. Downloading a file does not automatically acknowledge it. Use **Refresh payslips** if payroll has just released a new document. Raise calculation questions with payroll.

## 4. Account administration

**Location:** Setup → **Account Administration**. Company, group, or system administrator access is required.

### Invite a user

1. Confirm the active company.
2. Under **Invite employee account**, optionally select an employee. This fills their name and, where available, work email, and creates the self-service link.
3. Verify **Display name** and **Email**.
4. Select the required **Company roles**. At least one role is required.
5. Select **Send invitation** and wait for the success message.
6. Ask the recipient to activate the account using the invitation email.

Available roles are Employee, HR Executive, HR Manager, Payroll Executive, Payroll Manager, and Company Administrator. The **Company accounts** table shows names, emails, roles, employee links, and status. Use **Refresh** to reload it.

### Enable or disable an account

Find the account in **Company accounts** and select **Disable** or **Enable**. Disabling removes ERP authorisation immediately. You cannot disable your own account through this screen. Disabling a user with access to multiple companies is blocked; refer that case to the group/system administrator.

The current screen supports invitation and status changes. It does not provide an existing-account role editor or invitation-resend button.

## 5. Organisation masters

**Location:** Setup → **Organisation Masters**.

Create reference records before assigning them to employees. Company administrators and HR managers can maintain these records where authorised.

| Tab | Information to maintain |
|---|---|
| Company | Registered address, PAN, TAN, GSTIN, PF/ESI registrations, and configured logo path. |
| Branches | Code, name, address, state, PT/LWF applicability, and status. |
| Departments | Code, name, optional parent department, and status. |
| Grades | Code, name, and status. |
| Designations | Code, title, optional grade, and status. |
| Cost centres | Code, name, accounting reference, and status. |
| Holidays | Date, name, optional branch, and optional-holiday flag. |

Choose a tab, select **New**, complete the form, and select **Save**. To edit, select an existing row, update the form, and save. The Company tab edits the current company profile. Codes must be unique within the applicable company master.

## 6. Employee master and documents

**Location:** **Employee Master**.

### Add or update an employee

1. Select **New**, or select an existing employee row.
2. Complete Employment and Personal details, including employee number, name, joining date, employment type, and status.
3. Select organisation assignments, including branch, department, designation, and reporting manager as applicable.
4. Enter payment and statutory details. Verify the bank account, IFSC, and work email carefully.
5. Select **Save employee** and confirm the saved record.

Use the **Status** filter if an employee is missing from the list. Employee statuses include Active, Probation, Notice, Inactive, and Exited. A reporting manager must belong to the same company.

### Bulk Excel upload

1. Prepare an `.xlsx` workbook with the required columns **Employee Code**, **First Name**, and **Join Date**.
2. Under **Bulk employee upload**, select **Choose Excel**.
3. Enter the exact **Worksheet name** and select the **Default employment type**.
4. Select **Upload employees**.
5. Review accepted, rejected, and skipped counts and the row-level messages.
6. Correct rejected rows and upload the corrected workbook.

Existing employee codes in the active company are skipped as **Already exists**. The import does not update those employees; edit their records individually. An upload can accept some rows and reject others, so review the results before repeating it.

### Documents and self-service links

Select a saved employee. Under **Documents**, select a **Document type**, choose a file up to 10 MB, and select **Upload**. Use **Download** on a document row to retrieve it.

Under **Self-service access**, enter the email of an active application user who already has company access and select **Link user**. Use **Unlink** to remove an incorrect association. For a new account, Account Administration can invite and link the user in one step.

## 7. Attendance, shifts, and approvals

**Location:** **Attendance & Muster**.

### Configure shifts and assignments

1. Open **Shifts** and enter the shift code, name, start/end times, break minutes, grace periods, full-day/half-day thresholds, and effective dates.
2. Set the shift status and select **Save shift**.
3. Open **Assignments**, select the employee and shift, and enter effective dates.
4. Select **Assign shift**.

Administrators can set **Company timezone** in **Corrections & overtime**, for example `Asia/Kolkata`, and select **Save timezone**. Confirm the timezone before importing punches or reviewing overnight shifts.

### Record attendance

1. Open **Attendance** and select the From/To dates and optional employee filter.
2. Select **Refresh** to load records.
3. Complete **New attendance**, or select a row to edit it.
4. Choose the employee, date, shift, status, check-in/check-out times, and notes.
5. Select **Save attendance** and review the resulting status and worked/overtime minutes.

### Import attendance CSV

Open **CSV Import**, select **Choose CSV**, then **Upload**. Use these exact headers:

```csv
employeeNumber,attendanceDate,shiftCode,checkIn,checkOut,status,notes
EMP001,2026-09-21,DAY,2026-09-21T09:00:00+05:30,2026-09-21T18:00:00+05:30,PRESENT,Regular shift
```

Only `employeeNumber` and `attendanceDate` are required. Punches use ISO timestamps; include the timezone offset to make the intended time clear. Replace the sample employee number and shift code with records from the active company. Review accepted/rejected counts and correct reported rows.

### Request a punch correction or overtime approval

1. Open **Corrections & overtime** and select the **Attendance month**.
2. Select the existing **Attendance record**.
3. Select **Review type**: Overtime or Punch correction.
4. For overtime, enter minutes. For a correction, enter correct check-in and check-out timestamps with timezone offsets.
5. Enter a **Reason**, then select **Submit for approval**.
6. A different authorised HR manager or administrator enters a decision comment and selects **Approve** or **Reject**.

Payroll uses approved overtime only. Editing attendance invalidates that record's overtime approval, so review and obtain approval again after a change.

### Close the attendance month

After resolving attendance issues and approvals, an authorised user selects the month on **Attendance** and chooses **Lock**. Locked months prevent attendance changes. Use **Reopen** only when an authorised correction is needed, then review and lock the month again.

## 8. Leave administration and approvals

**Location:** **Leave Management** and **Leave Approvals & Notifications**.

### Leave types and balances

In **Leave types**, configure code, name, annual entitlement, accrual frequency, minimum/maximum days, carry-forward limit, and the paid, half-day, negative-balance, and supporting-document options. Select **Save**.

In **Balances**, select employee, leave type, and year. Enter opening, accrued, and adjusted amounts, then **Save**. Review the table's Used and Available values. Coordinate manual balances with accrual activation so the same entitlement is not credited twice.

In **Requests**, HR can submit a request for an employee and inspect existing requests. Decisions are made through **Leave Approvals & Notifications**.

### Automatic accrual

1. Open **Accrual processing**.
2. Set **Accrual start** to the first date not already included in opening balances.
3. Select **Save start date**.
4. Use **Process completed periods** to process eligible completed periods, then review processing events.

The scheduled job processes completed monthly, quarterly, or annual periods and prorates by calendar service days. Year-end carry-forward follows each leave type's limit. Reruns do not duplicate credits. The start date cannot change after the first credit. A blank start date leaves initial activation disabled.

### Configure the approval chain

1. Open **Leave Approvals & Notifications**.
2. Under **Company approval chain**, choose one to three distinct stages in order: Reporting manager, HR manager, or Company administrator.
3. Use **Add stage** or **Remove** as needed, then **Save chain**.

Reporting-manager stages require a configured manager with a linked account and active company access. HR stages allow eligible HR staff and administrators. Administrator stages allow company/group/system administrators. Changes apply to new submissions; existing requests retain their original chains.

### Decide a request and read notifications

1. Under **Awaiting my approval**, review the employee, leave dates, days, reason, and current stage.
2. Open **Approval history** if needed.
3. Select **Approve stage**, or enter a decision comment and select **Reject**.
4. Use **Refresh** to load current requests and notifications; select **Mark read** for notifications you have reviewed.

The requester and leave owner cannot approve their own request. Balance is consumed only after final approval. Approved unpaid leave becomes available to payroll. A request with no eligible approver must have its manager/account configuration corrected before submission.

## 9. Salary setup and payslips

**Location:** **Salary & Payroll**.

### Set up salary data

1. In **Components**, enter a code, name, component type, calculation method, values, effective dates, and applicable payroll flags. Select **Save component**.
2. In **Structures**, enter a code, name, description, effective dates, and status. Add components and their values, then **Save structure**.
3. In **Employee salaries**, select an employee and active salary structure. Enter **Annual CTC**, effective date, and revision reason, then select **Assign salary**.
4. In **Payroll settings**, configure working-day basis, attendance cutoff day, payment day, and negative salary policy. Select **Save settings**.

Use payroll-approved values and effective dates. Existing assignment history is shown for the selected employee. Salary components can be earnings, deductions, or employer contributions.

### Monthly payroll preparation

The current browser page exposes salary setup and payslips. It does not expose buttons for monthly payroll calculation, exception resolution, review, approval, or locking. Coordinate those steps with the authorised payroll/system operator before generating payslips; backend support alone does not make them available as browser actions.

### Generate and release payslips

1. Open **Payslips**, choose **Payroll month**, and select **Load**.
2. Check the displayed payroll status. **Generate** requires locked or paid payroll.
3. An authorised payroll manager or administrator selects **Generate**.
4. Review the listed employees and download sample PDFs to check the period, identity, and amounts.
5. Select **Release** when the documents are ready for employees.
6. Monitor **Receipt acknowledged** and **Acknowledged by** in the table.

Generation and release are separate actions. Employees see released payslips for their linked employment, subject to final payroll status.

### Optional email delivery

1. Verify the employee's **Work email** in Employee Master.
2. On a released payslip, open **Email & history**.
3. If email is enabled and your role permits it, select **Send email**.
4. Review the recorded result before selecting **Send again**.

**ACCEPTED** means the mail server accepted the message; it does not confirm inbox delivery. **FAILED** records a failure. A **SENDING** attempt can have an unknown outcome after interruption; contact the operator before resending. Email requires administrator configuration. Downloads remain available when email is disabled.

## 10. Expenses, loans, and advances

**Location:** **Expenses, Loans & Advances**.

### Submit a request

1. Choose **Type**: Expense, Loan, or Advance. Administrators/payroll managers also select the employee; employee self-service uses the linked employee.
2. Enter **Amount (INR)** and **Purpose**.
3. For an expense, attach the required receipt, up to 10 MB.
4. For a loan or advance, enter monthly installments (1–60) and the first recovery month.
5. Select **Submit for approval** and monitor the status.

### Approve and record payment

An authorised finance approver enters an approval/rejection comment and selects **Approve** or **Reject**. The requester cannot approve their own request.

After completing payment outside the ERP, enter **External payment reference** and select **Record completed external payment**. For loans/advances this also schedules recovery. Use **View recovery schedule** on a disbursed request to see monthly amounts and whether they are included in finalised payroll.

Loans and advances are interest-free in this workflow. Expenses are reimbursed externally. This screen records payment information; it does not transfer money.

## 11. HR workflows and exit

**Location:** **HR Workflows & Exit**.

HR managers and administrators can create recruitment, onboarding, training, appraisal, and promotion workflows. Payroll managers and administrators can create salary revision and exit workflows.

1. Choose **Workflow** and the employee. Recruitment can use **Not yet employed**.
2. Enter the title (or candidate and position), details/objectives/evidence, and effective/target date.
3. Complete the workflow-specific fields: appraisal rating, new designation, salary structure/annual CTC, or verified settlement earnings and deductions.
4. Select **Create workflow**.
5. Enter **Task evidence / transition reason**, then complete the checklist items.
6. Select **Submit** for independent review.
7. An authorised independent reviewer selects **Approve** or **Reject**, supplying the required reason.
8. For an approved workflow, select **Complete** when ready to apply its outcome.
9. Select **View history** to review actions, actors, reasons, and timestamps.

Promotion and exit completion update employment records. Salary revision creates an effective-dated salary assignment. Exit settlement amounts must be calculated and verified before submission; entering them is not an automatic full-and-final calculation or a bank payment.

## 12. Dashboard, reports, and exports

### Dashboard

Select **Dashboard date** to inspect attendance and approved leave for that date and payroll cost for its month. Workforce and pending counts reflect current records. Available cards depend on access.

### Reports

1. Open **Reports & Exports**.
2. Choose a **Report**, **From**, and **To** date.
3. Select **View** to inspect the result and company/date heading.
4. Select **Download CSV** to export the selected report and period.

| Report group | Available selections |
|---|---|
| HR | Employees, employee movements, attendance, attendance summary, attendance exceptions, leave balances, leave transactions. |
| Payroll (role-restricted) | Salary register, payroll components, payroll adjustments, department payroll, bank statement, payslip register. |

Payroll reports include approved, locked, and paid runs. Department grouping uses current employee assignments. **Bank statement** is a review export; confirm the bank's required upload format before using it for payments. An empty report may reflect the selected dates or payroll status rather than missing employee data.

## 13. Operating checklists

### Set up a company

- Confirm administrator access and active company.
- Maintain company profile, branches, departments, grades, designations, cost centres, and holidays.
- Set the company timezone and configure shifts.
- Create/import employees and verify reporting managers, work emails, and bank details.
- Invite users and link employee accounts.
- Configure leave types, opening balances, accrual start, and approval chains.
- Configure salary components, structures, assignments, and payroll settings.
- Have the system operator confirm document storage and optional email configuration.

### Prepare month-end payroll

- Resolve rejected attendance imports and missing punches.
- Complete correction/overtime reviews and leave approvals.
- Check employee joiners, exits, salary changes, and scheduled loan recoveries.
- Review attendance summary and exception reports, then lock attendance.
- Coordinate payroll calculation, review, approval, and lock with the authorised operator.
- Generate, review, and release payslips; send optional emails after checking recipients.
- Review payroll exports and acknowledgement history for the correct company and month.

## 14. Troubleshooting

| Symptom | Action |
|---|---|
| No company access after sign-in | Ask an administrator to confirm active company membership and account status. |
| A menu or action is disabled | Confirm the active company, role, required fields, and workflow status. Executive-only roles have limited sidebar access in the current interface. |
| Self-service cannot find your employment | Ask HR/admin to check the employee-to-user link for this company. |
| An employee is missing | Check the active company and employee Status filter. |
| Employee import shows Already exists | That employee code was skipped. Edit the existing record to change it. |
| An import partly succeeds | Review row errors, fix rejected rows, and confirm accepted records before repeating the upload. |
| Attendance cannot be changed | Check whether the month is locked; an authorised user must reopen it. |
| Overtime is absent from payroll inputs | Check overtime approval and whether later attendance edits invalidated it. |
| Leave cannot be submitted | Check dates/days, balance and type rules, required document, reporting manager, and eligible approver accounts. |
| An approval action is stale or no longer available | Refresh the inbox; another approver may already have decided the stage. |
| A document upload/download fails | Check file size and company access; ask the operator to verify storage if the problem continues. |
| No payslip is visible | Check company, employee link, payroll period/status, and whether payroll released the document. |
| Generate is disabled | Load the correct month and confirm payroll is locked or paid and your role permits generation. |
| Payslip email is disabled or stuck at Sending | Ask the operator to verify configuration or reconcile the existing email attempt. |
| A request fails or a page reports an API error | Record the module, company, time, action, and exact error message; contact support. Check whether the action succeeded before repeating it. |

When reporting a problem, include the employee number or payroll month if relevant. Your organisation should supply the site URL and named HR, payroll, and technical support contacts alongside this manual.
