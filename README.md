# sona-backend

API for **[Sona](https://trysona.io)** — the audio engineering calculator PWA. *Built for engineers behind the show.*

The calculators themselves run entirely in the browser (`sona-frontend`). This service adds accounts,
saved venues and shows, presets, teams, and share links.

| | |
|---|---|
| Runtime | Java 25, Spring Boot 4.0.1 |
| Database | PostgreSQL 16 + Flyway |
| Auth | JWT access tokens (JJWT 0.12.6, 15 min) + rotating opaque refresh tokens (7 days, stored hashed) |
| Docs | Swagger UI at `/swagger-ui/index.html` (shortcut `/api/docs`), OpenAPI JSON at `/v3/api-docs` |
| Tests | JUnit 5, Mockito, Testcontainers — 80% line-coverage gate (JaCoCo) |

## Local development

Prerequisites: JDK 25, Docker.

```bash
cp .env.example .env            # then set JWT_SECRET (openssl rand -base64 48)
docker compose up --build       # PostgreSQL on :5432, API on :8080
# 5432 already in use? set POSTGRES_PORT=5433 in .env
```

Or run only the database in Docker and the app from your IDE / Maven:

```bash
docker compose up -d postgres
./mvnw spring-boot:run          # 'dev' profile; has a dev-only JWT secret fallback
```

Or skip Docker Compose entirely and let Testcontainers start a throwaway database:

```bash
./mvnw spring-boot:test-run
```

Then open http://localhost:8080/swagger-ui/index.html (or http://localhost:8080/api/docs).

The frontend dev server (`npm run dev` in `sona-frontend`) proxies `/api` to `localhost:8080`.

## Tests

```bash
./mvnw test      # unit + integration tests (needs Docker for Testcontainers)
./mvnw verify    # same, plus the JaCoCo 80% line-coverage check
```

Coverage report: `target/site/jacoco/index.html`.

## Configuration

| Variable | Profile | Purpose |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | all | `dev` (default) or `prod` |
| `JWT_SECRET` | all | HS256 key, **≥ 32 bytes**. The app refuses to start without one in `prod`. |
| `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` | prod | JDBC URL (`jdbc:postgresql://…`) and credentials |
| `SPRING_DATASOURCE_URL`, `…_USERNAME`, `…_PASSWORD` | dev | Defaults to the compose database |
| `CORS_ORIGINS` | all | Comma-separated. Default: `http://localhost:5173,https://trysona.io` (prod: `https://trysona.io`) |
| `API_DOCS_ENABLED` | prod | Set `false` to hide Swagger UI |

Auth endpoints are rate limited to 10 requests/second per client IP (`sona.rate-limit.auth-requests-per-second`).

## API overview

All responses use an envelope:

```json
{ "success": true, "data": { }, "message": "OK", "timestamp": "2026-10-06T12:00:00Z" }
{ "success": false, "error": "VALIDATION_ERROR", "message": "email must not be blank",
  "details": [{ "field": "email", "message": "must not be blank" }], "timestamp": "…" }
```

Resources are identified by UUID only; internal numeric IDs are never exposed.

| Area | Endpoints |
|---|---|
| Auth | `POST /api/auth/register`, `/login`, `/refresh`, `/logout` |
| Me | `GET/PATCH/DELETE /api/users/me`, `PATCH /api/users/me/password` |
| Venues | `GET/POST /api/venues` (`?name=&page=&size=`), `GET/PATCH/DELETE /api/venues/{uuid}` |
| Shows | `GET/POST /api/venues/{uuid}/shows`, `GET/PATCH/DELETE /api/shows/{uuid}` |
| Presets | `GET/POST /api/presets` (`?tab=`), `GET/PATCH/DELETE /api/presets/{uuid}` |
| Share | `POST /api/share` (auth), `GET /api/share/{token}` (public) |
| Teams | `GET/POST /api/teams`, `GET/DELETE /api/teams/{uuid}`, `POST /api/teams/{uuid}/members`, `PATCH/DELETE /api/teams/{uuid}/members/{userUuid}` |

### Access rules

- Venues: visible to the owner and members of the venue's team. Team `OWNER`/`EDITOR` can edit; only the venue owner can delete. Deleting a venue that still has shows returns **409**.
- Shows: inherit their venue's rules; delete is limited to the show's creator or the venue owner.
- Presets: readable by the owner, team members, or anyone if `isPublic`; only the owner can change them.
- Teams: the creator is the single `OWNER` and manages members; any member may remove themselves.
- Share links: expire after 7 days (**410 Gone**); `singleUse: true` links work exactly once.
- `PATCH` endpoints treat omitted/`null` fields (or an empty body) as "leave unchanged".
- `DELETE /api/users/me` removes the account and everything it owns.

### Schema notes

`V1`–`V7` are the base schema. Three further migrations support behaviour the API needs:
`V8` refresh-token storage (for revocation), `V9` single-use share links, and `V10` `ON DELETE`
rules so account and team deletion cascade cleanly.

## Deployment (Railway)

`main` deploys automatically via `.github/workflows/deploy.yml`.

1. Create a Railway project with a PostgreSQL plugin and a service named `sona-backend` from this repo (it builds the `Dockerfile`).
2. Set service variables: `SPRING_PROFILES_ACTIVE=prod`, `JWT_SECRET`, `DATABASE_URL` (as `jdbc:postgresql://${{PGHOST}}:${{PGPORT}}/${{PGDATABASE}}`), `DATABASE_USERNAME`, `DATABASE_PASSWORD`, optionally `CORS_ORIGINS`.
3. Add GitHub secret `RAILWAY_TOKEN` (and optionally `CODECOV_TOKEN`).
4. Point `api.trysona.io` at the service. Health check: `/actuator/health`.
