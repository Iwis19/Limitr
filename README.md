# Limitr 🛡️

API protection and abuse-detection platform with a Spring Boot backend and Angular admin dashboard for monitoring, rule tuning, and enforcement workflows.

---

## Repository Note

Most of the development began locally since the summer, but more implementations in the future will be committed as soon as i finish each feature!

---

## Key Features

1. **API Key Protection + Rate Limiting**
   - `ApiProtectionFilter` guards `/api/**` routes with `X-API-KEY` authentication
   - Per-principal fixed-window rate limiting with response headers (`X-RateLimit-*`, `Retry-After`)
   - Supports dynamic limits based on enforcement state
2. **Abuse Detection Layer**
   - Scores principals from behavior signals (rate-limit abuse, failed auth, request spikes, resource enumeration)
   - Uses rolling time windows and cooldown logic to reduce noise
   - Feeds directly into enforcement decisions
3. **Enforcement + Incident Handling**
   - Automatic state progression (`OK` → `WARN` → `THROTTLED` → `TEMP_BANNED`)
   - Temporary bans with expiration + manual ban/unban actions
   - Incident logging + avoid repeated spam records
4. **Admin Dashboard Interface**
   - Angular SPA 
   - Live admin actions for filtering 
   - Rule editing UI for thresholds
5. **Auth + Persistence**
   - JWT-based admin authentication 
   - Spring Data JPA persistence 
   - PostgreSQL primary datastore with H2 profile support

---

## Tech Stack
- **Java 21**
- **Spring Boot** (Web, Security, Validation, JPA)
- **PostgreSQL** + **H2**
- **Angular 18** + **TypeScript**
- **JWT** (`jjwt`)
- Maven, RxJS

---

## Setup

### Prerequisites
- **Java 21**
- **Node.js 20+**
- **npm**
- **Docker Desktop** for PostgreSQL
- **Maven**

### 1. Start PostgreSQL
```powershell
docker compose up -d postgres
```

Default database values:
- database: `limitr`
- username: `postgres`
- password: `postgres`

### 2. Start the backend
```powershell
mvn spring-boot:run
```
Backend URL:
- `http://localhost:8080`

### 3. Start the frontend
```powershell
npm install
npm start
```
Frontend URL:
- `http://localhost:4200`

### 4. Use the seeded demo credentials
Admin login:
- username: `admin`
- password: `admin12345`

Demo API client:
- principal id: `demo-client`
- API key: `demo-free-key`

### Optional H2 mode
If you want to run the backend without PostgreSQL:
```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=h2"
```

### Frontend production build
```powershell
cd frontend
npm run build
```

---

## Project Structure
```text
New folder/
├── backend/
│   ├── src/main/java/com/limitr/
│   │   ├── config/               # Security + API protection filters + data seeding
│   │   ├── controller/           # Auth, admin, demo API, SPA forward controllers
│   │   ├── domain/               # JPA entities + enums
│   │   ├── dto/                  # Request/response payload models
│   │   ├── repository/           # Spring Data repositories
│   │   ├── service/              # Rate limiting, detection, enforcement, auth
│   │   └── LimitrApplication.java
│   ├── src/main/resources/
│   │   ├── application.yml       # Postgres + app config
│   │   ├── application-h2.yml    # H2 local profile config
│   │   └── static/               # Built frontend assets served by backend
│   └── pom.xml
├── frontend/
│   ├── src/app/
│   │   ├── pages/                # login, dashboard, logs, incidents, rules
│   │   ├── services/             # auth + admin API services
│   │   ├── guards/               # auth guard
│   │   ├── interceptors/         # JWT auth header interceptor
│   │   └── shell/                # authenticated app shell layout
│   ├── package.json
│   └── angular.json
├── scripts/
│   └── sync-frontend-to-backend.ps1
└── docker-compose.yml
```

## Lessons Learned
- Designing layered API defenses that combine limits, scoring, and enforcement
- Balancing protection with operator visibility through logs and incidents
- Full-stack workflow with aligned control and logics
- Managing JWT auth flow on Angular 
- Iterating on threshold tuning without breaking 

## License
MIT
