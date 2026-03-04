# Limitr

Limitr is a full-stack API protection demo that simulates a mini API gateway with:
- API key authentication for `/api/**`
- JWT admin authentication for `/admin/**`
- Fixed-window rate limiting with `429` responses and standard rate-limit headers
- Abuse scoring and automated enforcement (`OK`, `WARN`, `THROTTLED`, `TEMP_BANNED`)
- Admin observability and live rule configuration

## Stack
- Backend: Spring Boot + Spring Security + Spring Data JPA + PostgreSQL
- Frontend: Angular (standalone components, router, guard, interceptor, reactive forms)

## Project Structure
- `backend/` Spring Boot API + static hosting for Angular production build
- `frontend/` Angular admin dashboard
- `scripts/sync-frontend-to-backend.ps1` helper to copy Angular build artifacts into Spring Boot static directory

## Backend Features Implemented
- `POST /auth/register`
- `POST /auth/login` (returns JWT)
- API key header auth: `X-API-KEY`
- Rate limit per principal per minute
- `429` responses include:
  - `X-RateLimit-Limit`
  - `X-RateLimit-Remaining`
  - `Retry-After`
- Abuse score over rolling 5 minutes:
  - `+3` if rate limit exceeded
  - `+2` if failed auth attempts > 10
  - `+2` if sequential `/api/resource/{id}` enumeration detected
  - `+1` if traffic spike detected
- Escalation:
  - score >= `THROTTLE_THRESHOLD` -> throttled limit
  - score >= `BAN_THRESHOLD` -> temporary ban (default 15 min)
- Persisted entities:
  - `RequestLog`
  - `Incident`
  - `ApiClient`
  - `RuleConfig`
- Admin endpoints:
  - `GET /admin/stats`
  - `GET /admin/logs`
  - `GET /admin/incidents`
  - `PUT /admin/rules`
  - `POST /admin/actions/ban`
  - `POST /admin/actions/unban`
- Demo protected endpoints:
  - `GET /api/public/ping`
  - `GET /api/data`
  - `GET /api/resource/{id}`

## Frontend Features Implemented
- Login page
- Dashboard overview page
- Logs page with filters
- Incidents page
- Rules configuration page (includes manual ban/unban actions)
- Angular Router
- Route guard for admin pages
- HTTP interceptor attaching JWT for `/admin/**`
- Reactive forms
- Modern security-console styling with dark mode toggle

## Default Demo Credentials
Configured in `backend/src/main/resources/application.yml` and seeded on startup:
- Admin username: `admin`
- Admin password: `admin12345`
- API key: `demo-free-key`

Override with env vars:
- `DEMO_ADMIN_USERNAME`
- `DEMO_ADMIN_PASSWORD`
- `DEMO_API_KEY`

## Local Development

### 1) Start PostgreSQL
Option A (recommended): from repo root
```bash
docker compose up -d postgres
docker compose ps
docker compose exec -T postgres pg_isready -U postgres -d limitr
```

Option B: use your own PostgreSQL and create database `limitr`.

Override DB connection with:
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

### 2) Start backend
From `backend/`:
```bash
cd "c:\Users\User\Computer Science\New folder\backend"
mvn spring-boot:run
```

Optional fallback (no PostgreSQL): run with H2 profile
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

### 3) Start frontend
From `frontend/`:
```bash
cd "c:\Users\User\Computer Science\New folder\frontend"
npm install
npm start
```
Angular dev server proxies `/auth`, `/api`, `/admin` to `http://localhost:8080`.

## Production-style Serve (Spring Boot serves Angular)

### 1) Build frontend
From `frontend/`:
```bash
npm install
npm run build
```

### 2) Copy Angular dist to backend static
From repo root:
```powershell
./scripts/sync-frontend-to-backend.ps1
```

### 3) Run backend
From `backend/`:
```bash
mvn spring-boot:run
```

Now Angular routes are served by Spring Boot with SPA fallback.

## Quick Abuse Simulation

### Call a protected endpoint successfully
```bash
curl -H "X-API-KEY: demo-free-key" http://localhost:8080/api/public/ping
```

### Trigger rate limiting
```bash
for i in {1..90}; do curl -s -o /dev/null -w "%{http_code}\n" -H "X-API-KEY: demo-free-key" http://localhost:8080/api/data; done
```

### Trigger sequential enumeration signal
```bash
for i in {1..12}; do curl -s -H "X-API-KEY: demo-free-key" http://localhost:8080/api/resource/$i > /dev/null; done
```

### Trigger failed auth signal
```bash
for i in {1..12}; do curl -s -o /dev/null -w "%{http_code}\n" -H "X-API-KEY: bad-key" http://localhost:8080/api/data; done
```

Then login to dashboard and inspect:
- `/incidents`
- `/logs`
- `/dashboard`

## Gemini UI Prompt (Suggested)
Use this prompt in Gemini for iterative UI refinement of Angular templates/styles:

```
Design a modern enterprise security-console dashboard for an API abuse detection platform.
Requirements: dark mode option, card-based metrics, logs table, incidents timeline/table,
rules configuration form, minimalistic typography, subtle motion, professional SOC look,
fully responsive desktop/mobile layout.
```
