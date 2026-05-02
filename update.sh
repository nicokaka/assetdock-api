#!/usr/bin/env bash
# =============================================================================
# AssetDock — Update Script
# =============================================================================
# Pulls the latest Docker images and restarts the stack with zero data loss.
# Run this script from the directory containing docker-compose.client.yml.
#
# Usage:
#   chmod +x update.sh
#   ./update.sh
# =============================================================================

set -euo pipefail

COMPOSE_FILE="docker-compose.client.yml"

if [ ! -f "$COMPOSE_FILE" ]; then
  echo "[ERROR] $COMPOSE_FILE not found in the current directory."
  echo "        Run this script from the folder containing your AssetDock deployment files."
  exit 1
fi

echo ""
echo "=== AssetDock Update ==="
echo ""

echo "[1/3] Pulling latest images..."
docker compose -f "$COMPOSE_FILE" pull

echo ""
echo "[2/3] Restarting services with new images..."
docker compose -f "$COMPOSE_FILE" up -d --remove-orphans

echo ""
echo "[3/3] Verifying services are healthy..."
sleep 5
docker compose -f "$COMPOSE_FILE" ps

echo ""
echo "=== Update complete. AssetDock is running. ==="
echo "    Open http://localhost:3000 in your browser."
echo ""
