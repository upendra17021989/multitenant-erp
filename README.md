# Multi-tenant HR & Payroll ERP

Greenfield HR and payroll platform for multiple independent legal entities.

## Technology

- Backend: Java 21, Spring Boot, Maven, PostgreSQL, Flyway
- Frontend: React, TypeScript, Vite, MUI
- Local infrastructure: Docker Compose

## Repository layout

- `backend/` - modular-monolith API
- `frontend/` - browser application
- `infrastructure/` - local and deployment infrastructure
- `docs/` - architecture decisions and delivery tracking

## Project tracking

Start with `docs/IMPLEMENTATION_TRACKER.md`. It is the durable source of truth for completed work, current work, decisions, and next steps.

## Local development

1. Copy `.env.example` to `.env` and enter the JDBC values from Supabase Dashboard > Connect.
2. Export `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` from `.env`, then run the API: `mvn -f backend/pom.xml spring-boot:run`.
3. For optional local PostgreSQL instead, run `docker compose --env-file .env -f infrastructure/docker-compose.yml up -d` and override the backend database variables.
4. Install UI dependencies: `npm.cmd --prefix frontend install`
5. Run the UI: `npm.cmd --prefix frontend run dev`

The API health endpoint is `GET /api/health`.

Use a Supabase direct connection for Flyway migrations when IPv6 is available. On IPv4-only networks, use the Session pooler on port 5432. Do not use transaction pooling for Flyway or this persistent Spring Boot service.
