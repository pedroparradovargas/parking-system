# Arquitectura — parking-system

## Vista de alto nivel

```
┌────────────────────────┐        ┌────────────────────────┐
│  Caja Desktop (JVM)    │        │  App móvil cliente     │
│  Compose + SQLDelight  │◄──────►│  Compose + SQLDelight  │
│  Periféricos POS       │        │  Cámara LPR, NFC, BT   │
└────────┬───────────────┘        └─────────┬──────────────┘
         │                                  │
         │              ┌───────────────────┴─────┐
         │              │  Web PWA (Wasm)         │
         │              │  Compose + SQLDelight   │
         │              └─────────────┬───────────┘
         │                            │
         │           HTTPS + JWT      │           Bearer JWT
         └─────────────►┌─────────────▼─────┐◄──────────────────┐
                        │  Backend Ktor 3    │                   │
                        │  - REST + WS       │                   │
                        │  - Exposed + Flyway│                   │
                        └──┬───────────┬─────┘                   │
                           │           │                         │
                ┌──────────▼──┐    ┌───▼─────────────┐    ┌──────▼──────┐
                │ PostgreSQL  │    │  ai-service     │    │  web-admin  │
                │ + TimescaleDB│    │  FastAPI + YOLO │    │  Next.js 14 │
                └─────────────┘    └─────────────────┘    └─────────────┘
```

## Principios

1. **Offline-first** en TODOS los clientes (incluida web vía Wasm + IndexedDB).
2. **Multi-tenant** por `parking_id` en cada tabla.
3. **Source of truth**: PostgreSQL del backend.  Los clientes son cachés con outbox.
4. **Idempotencia** en escrituras (sincronización por `localId` UUID).
5. **Motor de tarifas único** en `shared/.../TariffCalculator.kt`.
6. **Auditoría inmutable** con hash chain SHA-256.

## Sincronización (resumen)

1. Cliente cierra sesión local → genera `Receipt` con `localId = UUID`.
2. `SyncManager` empuja `receipts_queue WHERE sync_status='PENDING'` al servidor.
3. Servidor lo inserta una sola vez (idempotente por `(parking_id, local_id)`).
4. Cliente marca el recibo como `SYNCED` con el `server_id` retornado.
5. WebSocket `/ws/occupancy` propaga censo en vivo a dashboards.

## Capas

- **domain**: modelos y reglas (sin Ktor, sin SQLDelight).
- **data**: implementaciones `expect/actual` por plataforma (Android/iOS/JVM/Wasm).
- **presentation**: Compose en `composeApp`.

## ¿Por qué Kotlin Multiplatform?

Ver `../decisions/ADR-001-kmp-choice.md`.
