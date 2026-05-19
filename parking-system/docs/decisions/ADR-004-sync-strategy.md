# ADR-004 — Estrategia de sincronización (outbox + idempotency)

## Estado
Aceptado — 2026-02-01.

## Contexto

Múltiples cajas/dispositivos pueden generar transacciones simultáneas
mientras alguno está offline.  Se necesita una estrategia robusta para
reconciliar sin perder ni duplicar registros.

## Decisión

Implementamos el **outbox pattern** en cada cliente y reconciliación
idempotente en el servidor:

1. **Generación**: el cliente crea `localId = UUID v4` para cada recibo /
   sesión.  El `localId` es la clave de idempotencia.
2. **Outbox local**: tabla `receipts` con `sync_status IN (PENDING, SYNCED, CONFLICT, FAILED)`.
3. **Push**: `SyncManager` envía batches a `/api/v1/sync/push` cuando
   `ConnectivityMonitor` reporta `online`.
4. **Inserción idempotente**: el backend hace `INSERT ... ON CONFLICT
   (parking_id, local_id) DO NOTHING` y devuelve el `serverId` (existente
   o recién creado).
5. **Confirmación**: el cliente marca el registro como `SYNCED` con el
   `serverId` recibido.
6. **Conflictos**: si el servidor detecta que la sesión ya fue cerrada
   por otra caja (carrera entre dispositivos offline), responde con
   `conflicts` y el cliente reconciliará: por defecto last-write-wins
   con timestamp del servidor.
7. **Pull**: el cliente pide cambios desde `since=lastSyncMillis`.
   Cambios típicos: tarifas, zonas, mensualidades.

## Por qué no Operational Transform / CRDT

Para este dominio (cobros financieros) la complejidad de OT/CRDT no
compensa: una vez emitido el recibo es **inmutable**.  Lo que se
sincroniza es append-only.  El outbox + idempotency keys es suficiente.

## Auditoría

Toda mutación servidor-side se anota en `audit_log` con hash chain
SHA-256 (`prev_hash` ↔ `current_hash`) para detectar manipulación
retroactiva.
