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

## Deployment

### Frontend on Vercel

1. Import this repository into Vercel and set the project Root Directory to `frontend`.
2. Set `VITE_API_BASE_URL` to `https://YOUR_CLOUD_RUN_SERVICE_URL/api` for Production and Preview as appropriate.
3. Deploy. Vercel uses `frontend/vercel.json` to build the Vite application into `dist` and provide SPA routing.

### Backend on Cloud Run

Build and deploy from the backend directory:

```sh
gcloud run deploy multitenant-erp-backend --source backend --region YOUR_REGION --allow-unauthenticated
```

Configure these Cloud Run runtime variables in **Edit and deploy new revision > Variables & Secrets**:

- `DB_URL`: Supabase Session pooler JDBC URL on port 5432
- `DB_USERNAME`: Supabase pooler username
- `DB_PASSWORD`: Supabase database password (prefer a Secret Manager reference)
- `CORS_ALLOWED_ORIGINS`: comma-separated Vercel origins, for example `https://example.vercel.app,https://example.com`

Cloud Run supplies `PORT` automatically. The application reads it through `server.port`. The local `backend/.env` file is optional and is excluded from the container image.
