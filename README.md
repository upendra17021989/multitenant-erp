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
- `DB_POOL_MAX_SIZE`: maximum JDBC connections per backend instance; default `3`
- `DB_POOL_MIN_IDLE`: idle JDBC connections retained per instance; default `0`
- `SUPABASE_JWT_ISSUER`: `https://YOUR_PROJECT_REF.supabase.co/auth/v1`
- `SUPABASE_JWKS_URL`: `https://YOUR_PROJECT_REF.supabase.co/auth/v1/.well-known/jwks.json`
- `CORS_ALLOWED_ORIGINS`: comma-separated Vercel origins, for example `https://example.vercel.app,https://example.com`
- `DOCUMENT_STORAGE_PROVIDER`: `supabase` in deployed environments (`filesystem` is the local default)
- `SUPABASE_URL`: Supabase project URL
- `SUPABASE_SERVICE_ROLE_KEY`: server-only secret used by the backend for private document storage
- `SUPABASE_DOCUMENT_BUCKET`: private bucket name, default `employee-documents`

Create the document bucket as private in Supabase Storage. The service-role key belongs only in Cloud Run/Secret Manager and must never use a `VITE_` prefix or be exposed to the browser.

Cloud Run supplies `PORT` automatically. The application reads it through `server.port`. The local `backend/.env` file is optional and is excluded from the container image.

### Optional payslip email

Apply Flyway V24 with the updated backend, and configure these server-only values to enable email:

```properties
PAYSLIP_EMAIL_ENABLED=true
PAYSLIP_EMAIL_FROM=payroll@example.com
SMTP_HOST=smtp.example.com
SMTP_PORT=587
SMTP_USERNAME=your-smtp-user
SMTP_PASSWORD=your-smtp-secret
SMTP_AUTH=true
SMTP_STARTTLS=true
```

Email is disabled by default. Store SMTP credentials in Secret Manager. STARTTLS is enabled and required by default; connection/read/write timeouts are bounded. Enabled email requires a configured host and valid sender address at startup. The sender must be authorized by your mail provider.

In Salary & Payroll > Payslips, load a month and open **Email & history** for an employee. Payroll managers and company/group/system administrators can explicitly send a released payslip from locked or paid payroll. Payroll executives can inspect history. The recipient is the employment's work email; no personal-email fallback, CC, or browser-supplied recipient is used. The original stored PDF is checked against its recorded size and SHA-256 before attachment. Company name and payroll period appear in the message.

Each attempt records the tenant, payslip, recipient snapshot, requesting user, timestamps, status, and a safe failure code. `ACCEPTED` means SMTP accepted the message, not confirmed inbox delivery. `FAILED` records invalid addresses, unavailable/corrupt documents, or a send error. An SMTP timeout can still mean the provider accepted a message; check provider logs before sending again. Each explicit resend creates a new audit record. Reusing the same request UUID returns the original attempt without sending again.

`SENDING` is committed before contacting SMTP. If the process stops or cannot record the final outcome, that record remains visible and blocks a new send for that payslip. An operator must reconcile the attempt against provider logs before any controlled database correction; never automatically retry an unknown outcome. Delivery is synchronous, one payslip per action, with no automatic send on release and no automatic retries. Disabling email keeps download, acknowledgement, and delivery history available.

API: `GET .../payslips/email-settings`, `GET .../payslips/{id}/email-attempts`, and `POST .../payslips/{id}/email` with `{"requestId":"<UUID>"}`, under `/api/payroll/runs/{year}/{month}`. These require the usual authenticated tenant context. No live emails are sent by the automated tests.
