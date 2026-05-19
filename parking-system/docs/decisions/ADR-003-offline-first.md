# ADR-003 — Offline-first en todos los clientes

## Estado
Aceptado — 2026-01-25.

## Contexto

Los parqueaderos operan 16-24 horas al día.  Las interrupciones de
internet (típicamente 5-30 min/mes en Colombia) **no pueden detener** la
operación: vehículos siguen entrando y saliendo.

## Decisión

Cada app cliente (Android, iOS, Desktop, Web PWA) tiene su propia base
de datos SQLite local (SQLDelight) y opera con autonomía.  El servidor
es la fuente de verdad eventualmente consistente.

## Implementación

- **Lecturas**: el repositorio local responde primero.  Si hay red, se
  actualiza desde el servidor en background.
- **Escrituras**: persisten primero en SQLite (`receipts_queue` con
  `sync_status='PENDING'`).  Un `SyncManager` empuja al servidor en
  cuanto hay conectividad.
- **Indicador visual**: `OfflineBanner` cuando no hay red.

## Implicaciones

- Toda escritura debe ser **idempotente** desde el cliente: ver
  ADR-004 sobre estrategia de sincronización.
- El motor de tarifas debe correr local + en servidor con resultados
  bit-exactos.  Tests en `commonTest` garantizan esto.

## Trade-offs

- Pro: operación nunca se detiene.
- Contra: ventana de inconsistencia entre dispositivos cuando hay
  particiones largas de red.  Se acepta como costo del negocio.
