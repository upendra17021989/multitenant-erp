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

1. Start PostgreSQL: `docker compose -f infrastructure/docker-compose.yml up -d`
2. Run the API: `mvn -f backend/pom.xml spring-boot:run`
3. Install UI dependencies: `npm.cmd --prefix frontend install`
4. Run the UI: `npm.cmd --prefix frontend run dev`

The API health endpoint is `GET /api/health`.
