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

1. Copy `.env.example` to `backend/.env` and enter the JDBC and Supabase Auth values.
2. Run the API: `mvn -f backend/pom.xml spring-boot:run`. Spring loads `backend/.env` automatically for local development.
3. For optional local PostgreSQL instead, run `docker compose --env-file .env -f infrastructure/docker-compose.yml up -d` and override the backend database variables.
4. Install UI dependencies: `npm.cmd --prefix frontend install`
5. Create `frontend/.env.local` with `VITE_API_BASE_URL`, `VITE_SUPABASE_URL`, and `VITE_SUPABASE_PUBLISHABLE_KEY`.
6. Run the UI: `npm.cmd --prefix frontend run dev`

The API health endpoint is `GET /api/health`.

Use a Supabase direct connection for Flyway migrations when IPv6 is available. On IPv4-only networks, use the Session pooler on port 5432. Do not use transaction pooling for Flyway or this persistent Spring Boot service.

## Deployment

### Frontend on Vercel

1. Import this repository into Vercel. The root `vercel.json` installs and builds only `frontend`.
2. Set `VITE_API_BASE_URL` to `https://YOUR_CLOUD_RUN_SERVICE_URL/api`, plus `VITE_SUPABASE_URL` and `VITE_SUPABASE_PUBLISHABLE_KEY`, for Production and Preview as appropriate.
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
- `SUPABASE_JWT_ISSUER`: `https://YOUR_PROJECT_REF.supabase.co/auth/v1`
- `SUPABASE_JWKS_URL`: `https://YOUR_PROJECT_REF.supabase.co/auth/v1/.well-known/jwks.json`
- `CORS_ALLOWED_ORIGINS`: comma-separated Vercel origins, for example `https://example.vercel.app,https://example.com`

Cloud Run supplies `PORT` automatically. The application reads it through `server.port`. The local `backend/.env` file is optional and is excluded from the container image.
