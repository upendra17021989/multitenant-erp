# Implementation Tracker

Last updated: 2026-09-02

This is the delivery source of truth. Update it whenever a milestone changes state. Architectural commitments are recorded in `ARCHITECTURE_DECISIONS.md`.

## Milestones

| ID | Milestone | Status |
|---|---|---|
| M00 | Requirements and foundational decisions | DONE |
| M01 | Repository and application foundation | IN PROGRESS |
| M02 | Authentication and tenant-aware authorization | NEXT |
| M03 | Tenant onboarding and organisation masters | BACKLOG |
| M04 | Employee master and documents | BACKLOG |
| M05 | Attendance, shifts and holidays | BACKLOG |
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
- [x] Add initial tenant/company database migration
- [x] Add tenant context and request boundary
- [x] Add frontend skeleton and active-company presentation
- [x] Run backend tests successfully
- [x] Install frontend dependencies and run production build
- [ ] Confirm clean migration against PostgreSQL

## Current notes

- Tenant-scoped requests initially require `X-Tenant-Id`.
- M02 will authorize that tenant against the authenticated user; request input alone will never grant access.
- Business policies remain configurable until their production activation gates.

## Verification history

| Date | Check | Result |
|---|---|---|
| 2026-09-02 | `mvn test` | PASS - 2 tests, 0 failures |
| 2026-09-02 | `npm.cmd run build` | PASS - TypeScript and Vite production build |

## Definition of done

Code, schema, tests, builds, documentation, and this tracker must all be updated.
