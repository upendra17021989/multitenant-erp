# Implementation Tracker

Last updated: 2026-09-04

This is the delivery source of truth. Update it whenever a milestone changes state. Architectural commitments are recorded in `ARCHITECTURE_DECISIONS.md`.

## Milestones

| ID | Milestone | Status |
|---|---|---|
| M00 | Requirements and foundational decisions | DONE |
| M01 | Repository and application foundation | DONE |
| M02 | Authentication and tenant-aware authorization | IN PROGRESS |
| M03 | Tenant onboarding and organisation masters | DONE |
| M04 | Employee master and documents | IN PROGRESS |
| M05 | Attendance, shifts and holidays | IN PROGRESS |
| M06 | Leave management | BACKLOG |
| M07 | Salary structure and rules engine | BACKLOG |
| M08 | Statutory calculation framework | BACKLOG |
| M09 | Monthly payroll workflow | BACKLOG |
| M10 | Payslips and secure delivery | BACKLOG |
| M11 | Reports, exports and dashboards | BACKLOG |
| M12 | Migration framework | BACKLOG |
| M13 | Security hardening and UAT | BACKLOG |
| M14 | Pilot and production rollout | BACKLOG |

## M01 checklist

- [x] Establish repository layout and working conventions
- [x] Record confirmed architecture decisions
- [x] Add backend build and application entry point
- [x] Add local PostgreSQL infrastructure
- [x] Add ignored local environment configuration and tracked template
- [x] Configure Supabase PostgreSQL environment variables with SSL
- [x] Add initial tenant/company database migration
- [x] Add tenant context and request boundary
- [x] Add frontend skeleton and active-company presentation
- [x] Run backend tests successfully
- [x] Install frontend dependencies and run production build
- [x] Confirm Flyway baseline and subsequent migration against Supabase PostgreSQL

## M02 checklist

- [x] Add Spring Security OAuth2 resource-server support
- [x] Validate Supabase access-token signature, issuer and expiry through JWKS
- [x] Link application users to Supabase Auth identities
- [x] Resolve active tenant roles from PostgreSQL
- [x] Reject unassigned tenant IDs at the backend request boundary
- [x] Add authenticated current-user and authorized-company endpoints
- [x] Add reusable tenant-role authorization helper
- [x] Add frontend Supabase email/password session handling
- [x] Add frontend authorized-company selector and tenant-aware API client
- [x] Add cross-tenant authorization tests
- [ ] Provision initial Supabase Auth users and map their company roles
- [ ] Complete authenticated end-to-end test with a real user session

## Current notes

- Tenant-scoped requests initially require `X-Tenant-Id`.
- M02 will authorize that tenant against the authenticated user; request input alone will never grant access.
- Business policies remain configurable until their production activation gates.
- Supabase PostgreSQL is the hosted database; local Docker PostgreSQL remains an optional development fallback.
- The authenticated M02 end-to-end test awaits frontend Supabase configuration and a provisioned test user.

## M03 checklist

- [x] Add tenant-scoped organisation-master schema
- [x] Add company legal/statutory profile API
- [x] Add branch/location create, list and update APIs
- [x] Enforce tenant-aware master-data uniqueness in PostgreSQL
- [x] Restrict organisation-master mutations by tenant role
- [x] Add department, designation, grade and cost-centre APIs
- [x] Add holiday-list APIs
- [x] Add organisation-master administration screens
- [x] Add tenant-isolation integration tests for organisation masters

## M04 checklist

- [x] Add separate tenant-scoped person and employment schema
- [x] Add employee create, list, detail and update APIs
- [x] Validate employment dates and tenant-local employee numbers
- [x] Enforce same-tenant organisation and reporting-manager assignments in PostgreSQL
- [x] Restrict employee data access and mutations by tenant role
- [x] Add employee tenant-isolation integration tests
- [x] Apply and verify employee migrations against Supabase PostgreSQL
- [x] Add employee administration screens
- [x] Add employee document metadata and tenant-separated storage abstraction
- [x] Add employee document upload, listing and download APIs
- [x] Add employee document upload, listing and download controls to the employee UI
- [x] Add a durable Supabase Storage adapter and server-only configuration
- [ ] Provision the private document bucket and storage credentials in the deployed environment

## M05 checklist

- [x] Add tenant-scoped effective-dated shift schema and APIs
- [x] Add employee shift-assignment schema and APIs
- [x] Add daily attendance-record schema and APIs
- [x] Add tenant-scoped attendance month locking and reopening
- [x] Add initial shift tenant-isolation integration test
- [x] Add attendance evaluation for full day, half day, absence and missing punches
- [x] Add grace-period, overtime and overnight-shift evaluation
- [x] Prevent records from being moved out of locked attendance months
- [ ] Add full attendance-record and month-lock tenant-isolation tests
- [ ] Add tenant-specific timezone configuration
- [ ] Add CSV attendance import and validation
- [ ] Add missing-punch correction and approval workflow
- [ ] Add overtime approval workflow
- [ ] Add attendance administration screens
- [ ] Add attendance summaries and exception reports

## Verification history

| Date | Check | Result |
|---|---|---|
| 2026-09-02 | `mvn test` | PASS - 2 tests, 0 failures |
| 2026-09-02 | `npm.cmd run build` | PASS - TypeScript and Vite production build |
| 2026-09-02 | Supabase direct JDBC `SELECT 1` | BLOCKED - direct endpoint not resolvable from current IPv4 environment |
| 2026-09-02 | Supabase session-pooler JDBC `SELECT 1` | PASS - database credentials and SSL connection verified |
| 2026-09-03 | `mvn clean verify` | PASS - 7 tests, 0 failures, 1 environment-dependent test skipped |
| 2026-09-03 | `npm.cmd run build` | PASS - authenticated TypeScript/Vite production build |
| 2026-09-03 | Supabase Flyway migration V2 | PASS - Auth identity mapping schema recorded at version 2 |
| 2026-09-03 | Secured startup on port 18080 | PASS - health 200; unauthenticated `/api/me` 401 |
| 2026-09-03 | `mvn clean verify` after organisation API implementation | PASS - 7 tests, 0 failures, 1 environment-dependent test skipped |
| 2026-09-03 | Supabase Flyway migration V3 | PASS - organisation-master schema recorded at version 3 |
| 2026-09-03 | `npm.cmd run build` after frontend organisation administration UI | PASS - TypeScript and Vite production build |
| 2026-09-04 | `mvn clean verify` after organisation tenant-isolation integration tests | PASS - 10 tests, 0 failures, 1 environment-dependent test skipped |
| 2026-09-04 | `mvn clean verify` after employee-master API implementation | PASS - 13 tests, 0 failures, 1 environment-dependent test skipped |
| 2026-09-04 | Supabase Flyway migration V4 | BLOCKED - session pool rejected the connection because its 15-client limit was reached |
| 2026-09-04 | `npm.cmd run build` after employee administration UI | PASS - TypeScript and Vite production build; Node upgrade warning remains |
| 2026-09-04 | Supabase Flyway migration V4 retry with reduced application pool | BLOCKED - Supabase session pool remains at its 15-client limit |
| 2026-09-04 | `mvn clean verify` after employee document APIs | PASS - 14 tests, 0 failures, 1 environment-dependent test skipped |
| 2026-09-04 | `mvn clean verify` after Supabase Storage adapter | PASS - 15 tests, 0 failures, 1 environment-dependent test skipped |
| 2026-09-04 | Supabase Flyway reconciliation after manual V4/V5 table creation | PASS - schema verified and Flyway history advanced to version 5 |
| 2026-09-04 | Packaged backend startup against Supabase | PASS - Flyway validation succeeded; `/api/employees` returned expected unauthenticated 401 |
| 2026-09-04 | `npm.cmd run build` after employee document UI | PASS - TypeScript and Vite production build; Node upgrade warning remains |
| 2026-09-04 | `mvn test` after attendance evaluation | PASS - 19 tests, 0 failures, 1 environment-dependent test skipped |

## Definition of done

Code, schema, tests, builds, documentation, and this tracker must all be updated.
