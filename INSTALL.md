# AssetDock — On-Premise Installation & Usage Manual

Complete guide for installing, configuring, and operating AssetDock on your own infrastructure.

---

## Table of Contents

1. [Requirements](#1-requirements)
2. [Installation](#2-installation)
3. [First Access — Setup Wizard](#3-first-access--setup-wizard)
4. [Using AssetDock](#4-using-assetdock)
5. [User Management & Roles](#5-user-management--roles)
6. [Importing Assets via CSV](#6-importing-assets-via-csv)
7. [Accessing from Other Computers](#7-accessing-from-other-computers)
8. [Updating to a New Version](#8-updating-to-a-new-version)
9. [Stopping and Starting](#9-stopping-and-starting)
10. [Backup & Restore](#10-backup--restore)
11. [HTTPS Setup (optional)](#11-https-setup-optional)
12. [Health Monitoring (optional)](#12-health-monitoring-optional)
13. [Configuration Reference](#13-configuration-reference)
14. [Troubleshooting](#14-troubleshooting)

---

## 1. Requirements

| Requirement | Details |
|---|---|
| **Operating System** | Windows 10/11, macOS, or Linux |
| **Docker** | [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running |
| **RAM** | Minimum 2 GB available for Docker |
| **Disk** | ~500 MB for images + database storage |
| **Network** | Port 3000 available (configurable) |

> That's it. No Java, no Node.js, no database installation needed.

---

## 2. Installation

### Step 1 — Download the deployment files

Download and place these files in the same directory on your machine:

```
assetdock/
├── docker-compose.client.yml
├── .env.client.example
└── update.sh
```

### Step 2 — Configure the environment

Copy `.env.client.example` to `.env`:

```bash
# Linux / macOS
cp .env.client.example .env

# Windows (PowerShell)
Copy-Item .env.client.example .env
```

Open `.env` in any text editor and set the two required values:

**`JWT_SECRET`** — A random string used to sign authentication tokens.

Generate one with:

```bash
# Linux / macOS
openssl rand -base64 48

# Windows (PowerShell)
[Convert]::ToBase64String((1..48 | ForEach-Object { [byte](Get-Random -Max 256) }))
```

**`DB_PASSWORD`** — A strong password for the internal database.

> The database runs inside Docker and is never exposed to the network. This password is only used for internal communication between containers.

Example `.env` file after configuration:

```env
JWT_SECRET=aB3x9kL2mN7pQ4rS8tU1vW5yZ0bC6dE3fG9hJ2kL5mN8pQ
DB_PASSWORD=MyStr0ng!Passw0rd#2026
WEB_PORT=3000
FRONTEND_URL=http://localhost:3000
```

### Step 3 — Start the application

Open a terminal in the directory where the files are and run:

```bash
docker compose -f docker-compose.client.yml up -d
```

Docker will download the images automatically. The first run takes 1–2 minutes depending on your internet connection. Subsequent starts take a few seconds.

### Step 4 — Open AssetDock

Open your browser and go to:

```
http://localhost:3000
```

You will be redirected to the **Setup Wizard** on first access.

---

## 3. First Access — Setup Wizard

On the first access, AssetDock presents a setup wizard to configure your organization. This screen appears only once and cannot be accessed again after completion.

### What you need to provide:

| Field | Description | Example |
|---|---|---|
| **Organization name** | Your company or team name (2–200 characters) | `Acme Corporation` |
| **Administrator full name** | Full name of the first admin user | `Jane Smith` |
| **Administrator email** | Email used to log in | `admin@acme.com` |
| **Administrator password** | Must meet the security policy (see below) | `MyP@ss2026!` |

### Password requirements

The administrator password must contain **all** of the following:

- Minimum **8 characters**
- At least **1 uppercase letter** (A–Z)
- At least **1 lowercase letter** (a–z)
- At least **1 number** (0–9)
- At least **1 special character** (!@#$%^&* etc.)

A real-time strength indicator shows which requirements are met as you type.

### After setup

After completing the wizard, you are redirected to the login screen. Sign in with the email and password you just created.

> **Security note:** The setup endpoint is rate-limited to **3 attempts per 10 minutes per IP address** to prevent abuse.

---

## 4. Using AssetDock

After logging in, you will see the main application with a navigation bar at the top containing these sections:

### Overview (Dashboard)

The landing page after login. Displays:

- **KPI cards** — Total assets, active assignments, and user count at a glance
- **Asset status chart** — Visual breakdown of assets by status
- **Asset health bar** — Distribution of asset condition across your inventory
- **Recent activity feed** — Latest actions performed in the system

### Assets

The core of AssetDock. Here you manage your organization's assets (equipment, devices, furniture, etc.).

**Available actions:**

| Action | How |
|---|---|
| **View all assets** | Navigate to **Assets** in the top menu |
| **Create an asset** | Click **New Asset** and fill in the form |
| **View asset details** | Click on any asset in the list |
| **Edit an asset** | Open an asset and click **Edit** |
| **Search assets** | Use the search bar or press `Cmd+K` / `Ctrl+K` |

**Asset statuses:**

| Status | Meaning |
|---|---|
| `IN_STOCK` | Available, not assigned to anyone |
| `ASSIGNED` | Currently assigned to a user |
| `IN_MAINTENANCE` | Under repair or maintenance |
| `RETIRED` | No longer in active use |
| `LOST` | Reported as lost |

**Asset fields include:** Name, asset tag, serial number, category, manufacturer, location, model, purchase date, purchase price, condition, notes, and more.

### Assignments

Assets can be assigned to users. When you assign an asset:

- The asset status automatically changes to `ASSIGNED`
- A record is created tracking who received the asset and when
- When you return an asset, the status reverts to `IN_STOCK`

### Audit Logs

Every action in AssetDock is logged for compliance and traceability:

- User logins (success and failure)
- Asset creation, modification, and deletion
- User creation, modification, and status changes
- Assignments and returns
- CSV imports
- System setup events

Navigate to **Audit Logs** to view, filter, and search the full audit trail.

### Search (Cmd+K)

Press `Cmd+K` (macOS) or `Ctrl+K` (Windows/Linux) to open the global search palette. Search across assets, users, and other entities from anywhere in the application.

---

## 5. User Management & Roles

Navigate to **Users** to manage your organization's users.

### Creating users

Click **New User** and provide:

- Full name
- Email address
- Password (must meet the same requirements as the setup wizard)
- Role

### Roles and permissions

| Role | Description |
|---|---|
| **ORG_ADMIN** | Full access. Can manage users, assets, imports, and view audit logs. |
| **ASSET_MANAGER** | Can create, edit, and manage assets and assignments. Cannot manage users. |
| **AUDITOR** | Read-only access to assets plus full access to audit logs. |
| **VIEWER** | Read-only access to assets and dashboard. |

### User statuses

| Status | Description |
|---|---|
| **ACTIVE** | User can log in and use the system normally |
| **INACTIVE** | User cannot log in. Can be reactivated by an admin. |
| **LOCKED** | Automatically locked after 5 failed login attempts. An admin can unlock. |

---

## 6. Importing Assets via CSV

AssetDock supports bulk importing assets from CSV files.

### How to import

1. Navigate to **Imports** in the top menu
2. Click **New Import**
3. Upload a `.csv` file
4. Review the import preview
5. Confirm to process

### CSV format

The CSV file must contain headers matching the asset fields. The import process validates each row and reports errors for invalid entries without blocking valid ones.

> **Note:** The import endpoint is rate-limited to **5 imports per 5 minutes** to prevent abuse.

---

## 7. Accessing from Other Computers

Other people on your local network (same Wi-Fi or LAN) can access AssetDock from their browser.

### Step 1 — Find your machine's IP address

```bash
# Windows (PowerShell)
ipconfig
# Look for "IPv4 Address" under your network adapter

# Linux
ip addr
# Look for "inet" under your network adapter (e.g. eth0, wlan0)

# macOS
ifconfig
# Look for "inet" under en0
```

### Step 2 — Update your `.env` file

Change `FRONTEND_URL` to use your IP address:

```env
FRONTEND_URL=http://192.168.1.50:3000
```

Restart the application:

```bash
docker compose -f docker-compose.client.yml up -d
```

### Step 3 — Share the address

Tell your colleagues to open:

```
http://192.168.1.50:3000
```

> **Windows firewall:** You may need to allow port 3000 through Windows Defender Firewall:
> Firewall → Advanced Settings → Inbound Rules → New Rule → Port → TCP → 3000 → Allow

---

## 8. Updating to a New Version

### Option A — Using the update script (recommended)

```bash
chmod +x update.sh    # first time only
./update.sh
```

The script pulls the latest images and restarts the stack with zero data loss. Your database and configuration are preserved.

### Option B — Manual update

```bash
docker compose -f docker-compose.client.yml pull
docker compose -f docker-compose.client.yml up -d --remove-orphans
```

> Updates never affect your data. The database volume persists across image updates.

---

## 9. Stopping and Starting

```bash
# Stop the application (data is preserved)
docker compose -f docker-compose.client.yml down

# Start the application
docker compose -f docker-compose.client.yml up -d
```

> **Important:** `down` stops and removes containers but preserves all data in Docker volumes. Your assets, users, and configuration are never lost.

---

## 10. Backup & Restore

### Automatic backups

AssetDock automatically creates daily backups of your database:

| Setting | Value |
|---|---|
| **Schedule** | Every day at 02:00 UTC |
| **Retention** | Last 7 daily backups |
| **Storage** | Docker volume `assetdock-backups` |
| **Format** | Compressed PostgreSQL dump (`.sql.gz`) |

No configuration required — backups start as soon as the stack is running.

### Copying backups to your machine

To extract backups from Docker to a folder on your machine:

```bash
# Linux / macOS
docker run --rm \
  -v assetdock_assetdock-backups:/source \
  -v "$(pwd)/backups":/dest \
  alpine cp -r /source/. /dest/

# This creates a 'backups/' folder in your current directory.
```

```powershell
# Windows (PowerShell)
docker run --rm `
  -v assetdock_assetdock-backups:/source `
  -v "${PWD}/backups:/dest" `
  alpine cp -r /source/. /dest/
```

### Restoring from a backup

```bash
# 1. Stop the application (keep the database running)
docker compose -f docker-compose.client.yml stop api web

# 2. List available backups
docker exec assetdock-backup ls /backups/daily/

# 3. Restore from a specific backup
docker exec -i assetdock-db \
  bash -c 'PGPASSWORD=$POSTGRES_PASSWORD pg_restore -U assetdock -d assetdock --clean --if-exists' \
  < <(docker exec assetdock-backup cat /backups/daily/<backup-file.sql.gz> | gunzip)

# 4. Restart the application
docker compose -f docker-compose.client.yml up -d
```

> Replace `<backup-file.sql.gz>` with the actual filename from step 2.

---

## 11. HTTPS Setup (optional)

By default, AssetDock runs over HTTP, which is sufficient for local network use. If you need HTTPS — for example, to access it securely over the internet — follow these steps.

### Requirements

- A **domain name** with a DNS A record pointing to your server
- Ports **80** and **443** open and publicly accessible from the internet

### Step 1 — Configure your `.env` file

Add or update these variables:

```env
DOMAIN=assetdock.mycompany.com
FRONTEND_URL=https://assetdock.mycompany.com
JWT_SECRET=your_secret_here
DB_PASSWORD=your_db_password_here
```

### Step 2 — Download the additional file

You need the `Caddyfile` in the same directory as your compose files. This file is included in the deployment package.

### Step 3 — Start with the HTTPS compose file

Use `docker-compose.https.yml` **instead of** `docker-compose.client.yml`:

```bash
docker compose -f docker-compose.https.yml up -d
```

### Step 4 — Access via HTTPS

Open your browser and go to:

```
https://assetdock.mycompany.com
```

Caddy will automatically obtain and renew a TLS certificate from Let's Encrypt. No manual certificate management is required.

> **Note:** Your server must be reachable from the internet on ports 80 and 443 for Let's Encrypt to verify domain ownership. If your server is behind a firewall or NAT, this requires additional configuration (e.g. port forwarding or a Cloudflare Tunnel).

### Updating when using HTTPS

```bash
docker compose -f docker-compose.https.yml pull
docker compose -f docker-compose.https.yml up -d --remove-orphans
```

---

## 12. Health Monitoring (optional)

AssetDock can be combined with [Uptime Kuma](https://github.com/louislam/uptime-kuma), a lightweight monitoring dashboard that tracks the availability of your services.

### Step 1 — Start the monitoring overlay

Run alongside your main stack:

```bash
docker compose -f docker-compose.client.yml -f docker-compose.monitoring.yml up -d
```

### Step 2 — Access the monitoring dashboard

Open your browser and go to:

```
http://localhost:3001
```

Create an admin account on first access.

### Step 3 — Add monitors

Add the following monitors to track AssetDock:

| Monitor Name | Type | URL | Settings |
|---|---|---|---|
| AssetDock API | HTTP (Keyword) | `http://assetdock-api:8080/actuator/health` | Keyword: `UP` |
| AssetDock Web | HTTP | `http://assetdock-web:80` | — |

> Uptime Kuma runs inside the same Docker network and can reach containers by name.

### Changing the monitoring port

Add to your `.env` file:

```env
MONITORING_PORT=3002
```

---

## 13. Configuration Reference

All configuration is done via the `.env` file. Here is the complete list of variables:

### Required

| Variable | Description | Example |
|---|---|---|
| `JWT_SECRET` | Secret key for signing authentication tokens | `openssl rand -base64 48` |
| `DB_PASSWORD` | Internal database password | `MyStr0ng!Pass` |

### Optional

| Variable | Default | Description |
|---|---|---|
| `WEB_PORT` | `3000` | Port for the web interface |
| `FRONTEND_URL` | `http://localhost:3000` | Public URL of the web interface |
| `DOMAIN` | — | Domain name (HTTPS mode only) |
| `MONITORING_PORT` | `3001` | Port for Uptime Kuma dashboard |

---

## 14. Troubleshooting

### The page doesn't load

1. Make sure Docker Desktop is running
2. Check if the containers started:
   ```bash
   docker compose -f docker-compose.client.yml ps
   ```
3. Check the logs:
   ```bash
   docker compose -f docker-compose.client.yml logs
   ```
4. If only the API is failing, check its specific logs:
   ```bash
   docker compose -f docker-compose.client.yml logs api
   ```

### Port 3000 is already in use

Edit your `.env` file and change the port:

```env
WEB_PORT=3001
```

Then restart:

```bash
docker compose -f docker-compose.client.yml up -d
```

### I forgot the admin password

Log in with another admin account and use the **Users** section to reset the password. If no other admin exists, you will need to restore from a backup.

### The setup wizard doesn't appear

The setup wizard only appears once, on the first access when no organization exists. If you see the login screen, the system is already configured.

### Login keeps failing (no error message, just reloads)

This usually means cookies are not being set correctly. Check:

1. `FRONTEND_URL` in your `.env` matches the URL you're accessing
2. If accessing from another machine, `FRONTEND_URL` should use that machine's IP, not `localhost`

### Containers keep restarting

Check the logs for the failing container:

```bash
docker compose -f docker-compose.client.yml logs api --tail 50
docker compose -f docker-compose.client.yml logs web --tail 50
```

Common causes:
- `JWT_SECRET` not set or too short
- Database password mismatch between containers (changed `DB_PASSWORD` after first run)

### How to completely reset (delete all data)

> **Warning:** This will permanently delete all data including users, assets, and audit logs.

```bash
docker compose -f docker-compose.client.yml down -v
docker compose -f docker-compose.client.yml up -d
```

The `-v` flag removes all Docker volumes. The setup wizard will appear again on next access.

---

*AssetDock — Self-Hosted Asset Management*
