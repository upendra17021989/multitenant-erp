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
