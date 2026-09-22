# Architecture Decisions

Accepted on 2026-09-02:

1. Each legal company is an independent tenant. New companies require configuration, not code changes.
2. Every tenant-owned record carries immutable `tenant_id`; tenant-aware uniqueness includes it where applicable.
3. Roles are assigned per tenant, so one user may hold different roles in different companies.
4. Backend authorization and data access enforce tenant isolation. Jobs, caches, files, imports, exports, and audits retain tenant context.
5. Person and employment multiline are separate. One person may have separate company-specific employments.
6. Approved payroll and financial history is immutable. Corrections use reversal, replacement, or effective-dated versions.
7. Attendance, leave, salary, approval, and payroll rules are configurable and effective-dated.
8. Indian statutory rates, thresholds, applicability, and formulas are configurable and effective-dated.
9. Supabase Auth issues user sessions. The backend validates Supabase JWT signature, issuer, and expiry, then resolves tenant roles from PostgreSQL; JWT or request input never grants tenant membership by itself.

## Initial architecture

Use a React/TypeScript/MUI frontend, Java 21 Spring Boot modular-monolith backend, PostgreSQL, object storage, and background jobs. Preserve domain module boundaries so modules can scale independently later.

## Payslip email delivery (2026-09-22)

Optional SMTP delivery is an explicit payroll-manager action, disabled by default. It uses the tenant's company name, employment work email, and original stored payslip PDF. A shared, server-configured verified sender is used. Release and final-payroll checks apply before delivery; recipient addresses cannot be supplied by the browser.

Delivery attempts commit before the external send and complete in a separate transaction, preserving failures. Tenant-scoped request UUIDs prevent repeated requests from sending twice. Concurrent sends for the same payslip are blocked while an attempt is in progress. SMTP acceptance does not prove delivery. Interrupted attempts retain an unknown outcome and require operator reconciliation, with no automatic retry. This initial scope sends one payslip synchronously; bulk queues and bounce tracking are outside M10's optional delivery implementation.

## Leave approval chains (2026-09-22)

Company policy selects an ordered chain of reporting manager, HR, and company administrator stages. Each leave request stores its own stage snapshot and reporting-manager employment. Changing policy does not rewrite pending requests. Role stages resolve active company members when checking access; manager stages resolve the captured employment's linked active user. The requester and leave owner are excluded from approver eligibility.

Leave creation, stage progression, final balance deduction, cancellation, and in-app notification creation share the existing tenant-locked transaction. A decision must reference the current stage UUID, preventing a repeated action from advancing a later stage. Only final approval changes the leave request to APPROVED and deducts balance. Earlier decisions remain visible after rejection or cancellation. Notifications are durable per-user, tenant-scoped records with an idempotent read timestamp; they do not send external email.
