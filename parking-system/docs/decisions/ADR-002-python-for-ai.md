# ADR-002 — Microservicio Python separado para IA

## Estado
Aceptado — 2026-01-20.

## Contexto

El sistema necesita reconocimiento de placa (LPR), forecast de ocupación
y detección de fraude.  El backend principal está en Kotlin/Ktor.

## Decisión

Construimos `ai-service` como un microservicio Python 3.11 + FastAPI
independiente, con su propia imagen Docker y endpoints REST.

## Justificación

- Ultralytics (YOLO), PaddleOCR y los modelos preentrenados de visión por
  computador son **Python-first**.  Wrappers JVM existen pero quedan
  permanentemente desactualizados.
- Aislamiento operativo: si el ai-service cae, el backend sigue
  funcionando en modo degradado (ingreso manual de placa).
- Escalado independiente: el ai-service es CPU/GPU-intensivo, no necesita
  los mismos recursos que el backend OLTP.

## Mitigaciones

- Comunicación HTTP/JSON con timeout agresivo (5s) y circuit breaker en el
  backend Ktor para evitar cascada de fallos.
- Contrato OpenAPI documentado en `docs/api/openapi.yaml`.
- Health checks (`/health`, `/ready`) consumidos por Kubernetes.
