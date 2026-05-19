#!/usr/bin/env bash
# =============================================================================
# dev-setup.sh — entorno local listo para desarrollar.
# Levanta postgres+redis+ai+backend+web-admin con docker compose.
# =============================================================================
set -euo pipefail
cd "$(dirname "$0")/../docker"

echo "→ Construyendo y levantando stack…"
docker compose up -d --build

echo "→ Esperando salud de postgres…"
until docker compose exec -T postgres pg_isready -U parking -d parking; do sleep 2; done

echo "✓ Stack levantado:"
echo "  Backend       http://localhost:8080/healthz"
echo "  AI service    http://localhost:8000/health"
echo "  Web admin     http://localhost:3000"
echo "  Prometheus    http://localhost:9090"
echo "  Grafana       http://localhost:3001 (admin/admin)"
