# AssetDock — Installation Guide

## Requirements

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running
- That's it

---

## Installation

### 1. Download the deployment files

Download and place these two files in the same directory on your machine:

- `docker-compose.client.yml`
- `.env.client.example`

### 2. Configure the environment

Rename `.env.client.example` to `.env` and open it in any text editor.

You must set two values:

**`JWT_SECRET`** — A strong random string used to sign authentication tokens.

Generate one on:
- Linux / macOS:
  ```
  openssl rand -base64 48
  ```
- Windows (PowerShell):
  ```
  [Convert]::ToBase64String((1..48 | ForEach-Object { [byte](Get-Random -Max 256) }))
  ```

**`DB_PASSWORD`** — A strong password for the internal database.

> The database is only accessible internally inside Docker. It is never exposed to the network.

### 3. Start the application

Open a terminal (or PowerShell) in the directory where the files are and run:

```
docker compose -f docker-compose.client.yml up -d
```

Docker will download the images automatically (first run takes 1–2 minutes).

### 4. Open the application

Open your browser and go to:

```
http://localhost:3000
```

On the first access, a **setup wizard** will appear. Follow the steps to create your organization and administrator account.

---

## Accessing from other computers on the same network

Other people on your local network (same Wi-Fi or LAN) can access AssetDock directly from their browser.

**Step 1 — Find your machine's IP address:**

- Windows: open PowerShell and run `ipconfig`, look for `IPv4 Address`
- Linux / macOS: run `ip addr` or `ifconfig`, look for your network adapter

**Step 2 — Share the address:**

Tell your colleagues to open:
```
http://<your-IP-address>:3000
```

Example: `http://192.168.1.50:3000`

> **Note:** On Windows, you may need to allow port 3000 through Windows Defender Firewall. Go to:
> Firewall → Advanced Settings → Inbound Rules → New Rule → Port → TCP → 3000 → Allow

---

## Updating to a new version

Run the included update script:

```bash
./update.sh
```

This will pull the latest images and restart the stack with zero data loss. Your database and configuration are preserved.

If you don't have the script, you can run the commands manually:

```bash
docker compose -f docker-compose.client.yml pull
docker compose -f docker-compose.client.yml up -d --remove-orphans
```

---

## Stopping and starting

```
# Stop
docker compose -f docker-compose.client.yml down

# Start
docker compose -f docker-compose.client.yml up -d
```

---

## Backup

AssetDock automatically creates daily backups of your database.

- Backups run every day at **02:00 UTC**
- The last **7 daily backups** are retained automatically
- Backups are stored in the Docker volume `assetdock-backups`

No action is required — backups start as soon as the stack is running.

### Copying backups to your host machine

To copy your backups out of Docker to a folder on your machine:

```bash
# Linux / macOS
docker run --rm \
  -v assetdock_assetdock-backups:/source \
  -v "$(pwd)/backups":/dest \
  alpine cp -r /source/. /dest/

# This creates a 'backups/' folder in your current directory.
```

### Restoring from a backup

```bash
# 1. Stop the application (keeps the database container running)
docker compose -f docker-compose.client.yml stop api web

# 2. List available backups
docker exec assetdock-backup ls /backups/daily/

# 3. Restore (replace <backup-file.sql.gz> with the filename you want)
docker exec -i assetdock-db \
  bash -c 'PGPASSWORD=$POSTGRES_PASSWORD pg_restore -U assetdock -d assetdock --clean --if-exists' \
  < <(docker exec assetdock-backup cat /backups/daily/<backup-file.sql.gz> | gunzip)

# 4. Restart the application
docker compose -f docker-compose.client.yml up -d
```

---

## Troubleshooting

**The page doesn't load:**
- Make sure Docker Desktop is running
- Confirm the containers started: `docker compose -f docker-compose.client.yml ps`
- Check logs: `docker compose -f docker-compose.client.yml logs`

**Port 3000 is already in use:**
- Edit your `.env` file and change `WEB_PORT=3000` to another port, such as `3001`
- Restart: `docker compose -f docker-compose.client.yml up -d`

**I forgot the admin password:**
- Log in with another admin account and use the user management panel to reset it
