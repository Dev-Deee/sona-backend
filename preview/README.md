# Sona preview — a hosted test link for testers

Runs the Docker images CI publishes (`ghcr.io/dev-deee/sona-backend` and `ghcr.io/dev-deee/sona-frontend`)
so testers can open one HTTPS link on their phone or laptop and give feedback. Nothing to install for them.

- **`:develop`** — everything merged into `develop` (what testers should normally see)
- **`:main`** — released builds
- **`:sha-<commit>`** — one exact build

How it fits together: the **app** container (nginx) serves Sona and forwards `/api` to the **API** container,
so the browser only ever talks to one address. PostgreSQL stores accounts, presets and share links.
HTTPS is required — without it the app can't be installed as a PWA or work offline.

## One-time: make the images public

The images inherit the private repos' visibility. GitHub only lets you change this on the website:

1. Open each package's settings:
   - <https://github.com/users/Dev-Deee/packages/container/sona-backend/settings>
   - <https://github.com/users/Dev-Deee/packages/container/sona-frontend/settings>
2. **Danger Zone → Change visibility → Public**.

The source code stays private; secrets are never in the images (they are set when the containers start).

---

## Option A — Railway (no server to manage)

Railway runs the images, gives you an HTTPS URL and handles restarts. Roughly $5/month after the trial.

1. Create a project at <https://railway.com>, then **+ New → Database → PostgreSQL**.
2. **+ New → Docker Image** → `ghcr.io/dev-deee/sona-backend:develop`. Rename the service to `sona-backend`.
   Under **Variables**, add:

   | Variable | Value |
   |---|---|
   | `SPRING_PROFILES_ACTIVE` | `prod` |
   | `DATABASE_URL` | `jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}` |
   | `DATABASE_USERNAME` | `${{Postgres.PGUSER}}` |
   | `DATABASE_PASSWORD` | `${{Postgres.PGPASSWORD}}` |
   | `JWT_SECRET` | output of `openssl rand -base64 48` |
   | `CORS_ORIGINS` | the app's public URL from step 3, e.g. `https://sona-frontend-production.up.railway.app` |

   Don't generate a public domain for the API — the app reaches it over Railway's private network.
3. **+ New → Docker Image** → `ghcr.io/dev-deee/sona-frontend:develop`. Rename it to `sona-frontend`.
   - Variable `API_UPSTREAM` = `http://${{sona-backend.RAILWAY_PRIVATE_DOMAIN}}:8080`
   - **Settings → Networking → Generate Domain**, target port **80**. That URL is your test link —
     copy it into the API's `CORS_ORIGINS` (step 2).
4. Open the link. To pick up a newer build later: each service → **⋮ → Redeploy** (it re-pulls `:develop`).

## Option B — a small VPS (about $4–6/month)

Any Linux server with Docker (Hetzner, DigitalOcean, …). Caddy gets a free HTTPS certificate automatically.

Copy the three files in this folder to the server (from your machine):

```bash
scp preview/compose.yml preview/Caddyfile preview/.env.example user@your-server:~/sona/
ssh user@your-server 'cd ~/sona && cp .env.example .env'
```

Edit `.env`:

- `SONA_DOMAIN` — your domain, or with no domain yet use the server IP with dashes + `.sslip.io`
  (IP `203.0.113.10` → `203-0-113-10.sslip.io`). Open ports 80 and 443 in the firewall.
- `JWT_SECRET` — `openssl rand -base64 48`
- `POSTGRES_PASSWORD` — `openssl rand -base64 24`

```bash
docker compose up -d
```

Testers open `https://<SONA_DOMAIN>`. Update to the latest build:

```bash
docker compose pull && docker compose up -d
```

Try it on your own machine first with `SONA_DOMAIN=localhost` (the browser will warn about Caddy's local certificate).

---

## Collecting feedback

Simplest options while testing: a short Google/Microsoft Form linked from your invite message, or GitHub
Issues (testers need repo access). Ask testers to include the page and calculator they were on — the URL
(e.g. `#/coverage/delay-planner`) identifies it exactly.
