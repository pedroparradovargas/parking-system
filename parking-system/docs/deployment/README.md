# Deployment

## Entornos

- **dev**: docker compose en la laptop.  Ver `infrastructure/scripts/dev-setup.sh`.
- **staging**: cluster K8s pequeño (1 nodo).  Manifiestos en
  `infrastructure/kubernetes/`.
- **prod**: cluster K8s multi-AZ con RDS Postgres + ElastiCache Redis.
  Provisionamiento con Terraform (ver `infrastructure/terraform/`).

## Pipeline

1. **PR abierto** → GitHub Actions corre `ci-kotlin.yml`, `ci-python.yml`,
   `ci-web-admin.yml` según paths cambiados.
2. **Merge a main** → mismo CI + build de imágenes Docker → push a
   `ghcr.io/parking/{backend,ai-service,web-admin}:<sha>`.
3. **Promote a prod** → workflow manual `deploy-prod.yml` (no incluido
   en este commit; se añade tras configurar el cluster) actualiza el
   tag en los manifiestos K8s.

## Variables de entorno requeridas (backend)

| Variable | Descripción | Default |
|---|---|---|
| `DATABASE_URL` | JDBC URL Postgres | local |
| `DB_USER` / `DB_PASSWORD` | credenciales | `parking/parking` |
| `JWT_SECRET` | clave HMAC ≥32 bytes | demo (¡cambiar!) |
| `AI_SERVICE_URL` | URL del microservicio IA | localhost:8000 |
| `REDIS_HOST` | host Redis para pub/sub WS | localhost |
| `ENABLE_HSTS` | activa HSTS header | true |
| `CORS_ALLOWED_HOSTS` | JSON array | localhost:3000 |

## Backups

- `pg_dump` diario cifrado con AES-256 a S3/GCS.  Retención 30 días.
- Restore tested cada trimestre como parte de DR drill.

## Observabilidad

- Métricas Prometheus en `:9090` (configuración en
  `infrastructure/docker/prometheus.yml`).
- Logs estructurados con Logback JSON, agregados a CloudWatch / Loki.
- WebSocket `/ws/occupancy` para dashboards en vivo.
