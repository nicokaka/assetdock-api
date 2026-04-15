# AssetDock — Self-Hosted Deployment Guide

Deploy the complete AssetDock stack on your own infrastructure with a single command.

## What You Get

```
Your Server
┌─────────────────────────────────────────────────────┐
│  ┌───────────┐    ┌───────────┐    ┌─────────────┐  │
│  │ Web :3000 │───▶│ API :8080 │───▶│ PostgreSQL  │  │
│  │  (nginx)  │    │ (Spring)  │    │   :5432     │  │
│  └───────────┘    └───────────┘    └─────────────┘  │
│                                                     │
│  All data stays HERE. On YOUR server.               │
└─────────────────────────────────────────────────────┘
```

- **Web**: React frontend served by nginx (port 3000)
- **API**: Spring Boot backend (internal, not exposed)
- **Database**: PostgreSQL 17 with persistent data volume

## Requirements

- Docker 20.10+
- Docker Compose v2+
- 1 GB RAM minimum (2 GB recommended)
- 1 GB disk space

## Quick Start

### 1. Clone both repositories

```bash
git clone https://github.com/nicokaka/assetdock-api.git
git clone https://github.com/nicokaka/assetdock-web.git
```

Both repos must be side by side:
```
your-directory/
├── assetdock-api/
└── assetdock-web/
```

### 2. Configure environment

```bash
cd assetdock-api
cp .env.selfhosted.example .env.selfhosted
```

Edit `.env.selfhosted` and set:
- `POSTGRES_PASSWORD` — a strong database password
- `JWT_SECRET` — generate with `openssl rand -base64 48`
- `SEED_ADMIN_EMAIL` — your admin login email
- `SEED_ADMIN_PASSWORD` — your admin login password
- `SEED_ORG_NAME` — your organization name

### 3. Start everything

```bash
docker compose -f docker-compose.selfhosted.yml --env-file .env.selfhosted up -d
```

### 4. Access the application

Open **http://localhost:3000** and sign in with the admin credentials you configured.

## Managing the Stack

```bash
# View logs
docker compose -f docker-compose.selfhosted.yml logs -f

# Stop the stack
docker compose -f docker-compose.selfhosted.yml down

# Stop and DELETE all data (destructive)
docker compose -f docker-compose.selfhosted.yml down -v

# Rebuild after updates
docker compose -f docker-compose.selfhosted.yml up -d --build
```

## Updating

```bash
cd assetdock-api && git pull
cd ../assetdock-web && git pull
cd ../assetdock-api
docker compose -f docker-compose.selfhosted.yml up -d --build
```

Flyway migrations run automatically on API startup — the database schema updates itself.

## Production Considerations

### HTTPS

For production, place a reverse proxy (Caddy, Traefik, or nginx) in front of the web container with TLS:

```bash
# In .env.selfhosted, update:
FRONTEND_URL=https://assets.yourcompany.com
WEB_SESSION_SECURE_COOKIES=true
WEB_SESSION_COOKIE_SAME_SITE=Lax
```

### Backups

The PostgreSQL data is stored in a Docker volume named `assetdock-api_postgres-data`. Back it up regularly:

```bash
docker exec assetdock-db pg_dump -U assetdock assetdock > backup_$(date +%F).sql
```

### Restore

```bash
cat backup_2026-04-15.sql | docker exec -i assetdock-db psql -U assetdock assetdock
```
