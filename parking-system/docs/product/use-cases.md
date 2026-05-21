# Casos de Uso — parking-system

**Formato:** UC-AAA-nnn (AAA = área).
**Convención:** flujo principal numerado, alternativas con sufijo (e.g. 3a, 3b), excepciones con prefijo `Ex`.
**Mapeo a RF:** cada UC referencia los RF de `PRD.md` que cubre.

---

## Actores

| Símbolo | Actor |
|---------|-------|
| **OP** | Operador de caja |
| **AD** | Administrador del parqueadero |
| **CE** | Cliente eventual |
| **CA** | Cliente alquilado |
| **SU** | Supervisor ejecutivo |
| **SYS** | Sistema (procesos automáticos) |

---

## Caja / sesiones

### UC-CAJ-001 — Registrar entrada de vehículo eventual

**Actor primario:** OP.
**Objetivo:** dejar al vehículo dentro del parqueadero con una sesión activa abierta.
**Precondiciones:** OP autenticado; parking abierto; al menos una zona con cupo o sin zona específica.
**RF cubiertos:** RF-CAJ-001, RF-PER-002 (lector), RF-IA-001 (LPR opcional).

**Flujo principal:**
1. OP escanea la placa con cámara LPR o lector de barras (o digita manualmente).
2. Sistema valida formato de placa (5–10 caracteres).
3. OP selecciona tipo de vehículo (CAR, MOTORCYCLE, TRUCK, BUS, BICYCLE).
4. OP selecciona zona (opcional) — sistema muestra zonas con cupo libre.
5. OP confirma "Registrar entrada".
6. Sistema crea `ParkingSession` con `entryAt = now`, `status = ACTIVE`, `localId = UUID`.
7. Sistema incrementa ocupación de la zona elegida.
8. Sistema imprime "tiquete de entrada" en impresora térmica.
9. Sistema notifica vía WebSocket nueva ocupación.

**Alternativos:**
- **5a (mensualidad detectada):** sistema busca placa en `customers`; si tiene plan vigente, marca la sesión con `customerId` (cobro será $0).

**Excepciones:**
- **Ex-1 (placa inválida):** sistema muestra error; vuelve a paso 1.
- **Ex-2 (zona llena):** sistema sugiere otra zona o "sin zona".
- **Ex-3 (sin conexión):** sistema opera offline; sesión queda en cola para sincronizar (RF-CAJ-007).

---

### UC-CAJ-002 — Cerrar sesión y cobrar

**Actor primario:** OP.
**Objetivo:** entregar al vehículo cobrando lo que corresponde.
**Precondiciones:** sesión activa existente.
**RF cubiertos:** RF-CAJ-002, RF-CAJ-003, RF-CAJ-004, RF-CAJ-005, RF-CAJ-006.

**Flujo principal:**
1. OP escanea placa o digita.
2. Sistema busca `ParkingSession` activa por placa (`findActiveByPlate`).
3. Sistema muestra detalle (entrada, tiempo transcurrido, zona).
4. OP solicita "Calcular cobro".
5. Sistema invoca `TariffCalculator.calculate(tariff, entryAt, exitAt = now)` y muestra `ChargeBreakdown` (minutos, horas facturadas, subtotal, recargo nocturno, IVA, total).
6. OP confirma "Cobrar y cerrar".
7. OP selecciona medio de pago (efectivo, tarjeta, transferencia).
8. Sistema marca sesión `status = CLOSED`, registra `exitAt`, `totalCents`, `ivaCents`.
9. Sistema crea `Receipt` con `localId`, secuencia local, `subtotalCents`, `ivaCents`, `totalCents`.
10. Sistema imprime recibo en impresora térmica.
11. Si pago efectivo: sistema abre cajón monedero (RF-PER-003).
12. Sistema decrementa ocupación de zona.
13. `SyncManager` empuja recibo al servidor en background (idempotente por `localId`).

**Alternativos:**
- **5a (cliente alquilado vigente):** `ChargeBreakdown.appliedMonthly = true`, `totalCents = 0`. Recibo se imprime como "Mensualidad" sin cobro.
- **5b (dentro de gracia):** `ChargeBreakdown.withinGrace = true`, `totalCents = 0`.

**Excepciones:**
- **Ex-1 (placa sin sesión activa):** sistema avisa; ofrece registrar entrada (UC-CAJ-001).
- **Ex-2 (impresora caída):** sistema imprime recibo digital (QR) y marca incidente; recibo queda válido.
- **Ex-3 (sin conexión):** recibo se guarda en outbox; al sincronizar, servidor confirma o reporta conflicto.

---

### UC-CAJ-003 — Sincronizar recibos pendientes

**Actor primario:** SYS (SyncManager).
**Objetivo:** subir recibos creados offline al servidor.
**Precondiciones:** conexión restablecida; al menos un recibo `PENDING` en outbox.
**RF cubiertos:** RF-CAJ-007.

**Flujo:**
1. `ConnectivityMonitor` detecta `online = true`.
2. `SyncManager.runOnce()` se dispara.
3. Selecciona recibos con `sync_status = PENDING` (FIFO).
4. POST `/api/v1/sync/push` con `{deviceId, parkingId, receipts: [...]}`.
5. Servidor procesa idempotentemente por `(parking_id, local_id)`:
   - **Accepted:** recibo nuevo, devuelve `server_id`.
   - **Already existing:** devuelve el `server_id` previo.
   - **Conflict:** otra caja cerró antes la misma sesión; reporta conflicto.
6. Cliente marca recibos `SYNCED` con el `server_id` o `CONFLICT` con `last_error`.
7. GET `/api/v1/sync/pull?since=lastSyncMillis` para traer cambios remotos (tarifas, zonas).
8. Actualiza tablas locales con UPSERT transaccional.

**Excepciones:**
- **Ex-1 (push falla por red):** mantiene `PENDING`, reintenta al siguiente tick.
- **Ex-2 (servidor 5xx):** registra error en log, reintenta con backoff.

---

### UC-CAJ-004 — Cierre de turno / caja

**Actor primario:** OP.
**Objetivo:** consolidar el turno con totales para entregar al siguiente operador.
**RF cubiertos:** RF-REP-003.
**Estado actual:** **NO IMPLEMENTADO** (gap detectado).

**Flujo esperado:**
1. OP solicita "Cierre de caja".
2. Sistema lista sesiones cerradas en el turno por OP.
3. Sistema agrega totales: cantidad, efectivo, tarjeta, otros medios.
4. OP confirma "Cerrar turno" — se imprime resumen.
5. Sistema crea registro de cierre con timestamp + hash de auditoría.

---

## Cliente eventual

### UC-CLI-001 — Reservar puesto vía PWA

**Actor primario:** CE.
**Objetivo:** asegurar cupo antes de llegar al parqueadero.
**Estado actual:** UI portada (`ReservationScreen` + `PaymentQrScreen`), sin persistencia ni pago real.
**RF cubiertos:** parcialmente RF-CAJ-001 (entrada con reserva).

**Flujo:**
1. CE abre la PWA, ingresa placa y tipo.
2. Sistema genera `reservationId` local.
3. Sistema muestra QR con datos de la reserva.
4. CE llega al parqueadero; OP escanea el QR.
5. Sistema crea sesión activa pre-cargada.

---

## Cliente alquilado

### UC-CLA-001 — Acceso con mensualidad vigente

**Actor primario:** CA.
**Objetivo:** entrar/salir sin pasar por cobro.
**Precondiciones:** plan vigente registrado (UC-ADM-CLI-002).
**RF cubiertos:** RF-CAJ-008, RF-CLI-002.

**Flujo:**
1. CA llega al parqueadero; sistema escanea placa.
2. Sistema busca cliente por placa.
3. Si plan vigente: registra sesión con `customerId`; cobro será $0 al salir.
4. Si plan vencido: tratar como cliente eventual (UC-CAJ-001).
5. Al salir, `TariffCalculator` marca `appliedMonthly = true`; `Receipt.totalCents = 0` pero se registran horas para reportes.

---

## Administrador — Tarifas

### UC-ADM-TAR-001 — Crear nueva tarifa

**Actor primario:** AD.
**RF cubiertos:** RF-TAR-001, RF-TAR-004, RF-TAR-005, RF-TAR-006.
**Estado actual:** **NO IMPLEMENTADO** en backend (CRUD inexistente). UI `TariffManagementScreen` es read-only sobre datos hardcoded.

**Flujo esperado:**
1. AD entra a "Gestión Tarifas".
2. Selecciona tipo de vehículo (CAR / MOTORCYCLE / etc.).
3. Ingresa: primera hora ($), subsiguientes ($), recargo nocturno (% + franja), gracia (min), IVA (%), validFrom, validTo (opcional).
4. Sistema valida (precios > 0, franja válida, vigencia no se solapa con tarifa actual).
5. AD pulsa "Guardar".
6. Sistema crea `Tariff` con `validFrom = ahora` (o futuro programado), `validTo = NULL` (vigente indefinido) o fecha futura.
7. Sistema cierra `validTo` de la tarifa anterior (si la había) al `validFrom - 1ms` para evitar solapes.
8. Audit log: `tariff.created` con payload completo.
9. SyncManager propaga la nueva tarifa a las cajas en su próximo pull.

---

### UC-ADM-TAR-002 — Editar tarifa vigente (versionada)

**Política:** las tarifas NO se sobrescriben in-place. Editar crea una nueva versión y cierra la anterior.

**Flujo:**
1. AD selecciona tarifa vigente.
2. Sistema muestra formulario pre-cargado.
3. AD modifica campos.
4. AD pulsa "Guardar nueva versión".
5. Sistema cierra `validTo` de la anterior y crea una nueva con `validFrom = ahora`.

---

### UC-ADM-TAR-003 — Configurar planes de mensualidad

**RF cubiertos:** RF-TAR-007.

**Flujo:**
1. AD entra a "Planes de Mensualidad".
2. Crea planes: mensual ($), trimestral ($), semestral ($), anual ($).
3. Sistema calcula descuento implícito vs. cobro por hora (ayuda visual).
4. Audit log.

---

### UC-ADM-TAR-004 — Configurar tarifas especiales

**RF cubiertos:** RF-TAR-008.

**Tipos:**
- Fin de semana (multiplicador o tarifa alterna por día de la semana).
- Festivos (calendario configurable).
- Eventos puntuales (rango fechas + multiplicador).

---

## Administrador — Zonas

### UC-ADM-ZON-001 — Crear zona

**RF cubiertos:** RF-ZON-001.
**Estado actual:** **NO IMPLEMENTADO** en backend (CRUD inexistente). UI `ZonesScreen` muestra ocupación, sin CRUD.

**Flujo:**
1. AD entra a "Zonas".
2. Ingresa código (A1, B2, ...), capacidad, tipos permitidos (multi-select).
3. Sistema valida código único por parking.
4. Crea zona con `currentOccupancy = 0`.
5. Audit log.

---

### UC-ADM-ZON-002 — Marcar cupos en mantenimiento

**RF cubiertos:** RF-ZON-004.

**Flujo:**
1. AD selecciona zona.
2. Ingresa cantidad de cupos a poner fuera de servicio.
3. Sistema reduce capacidad efectiva (`capacity - underMaintenance`); ocupación no contabiliza esos cupos.
4. Audit log.

---

## Administrador — Usuarios

### UC-ADM-USR-001 — Crear usuario operador

**RF cubiertos:** RF-USR-001, RF-USR-002.
**Estado actual:** **NO IMPLEMENTADO** (no hay endpoint CRUD usuarios).

**Flujo:**
1. AD entra a "Usuarios".
2. Ingresa: username, email, nombre, password inicial, roles (multi-select).
3. Sistema hashea password con bcrypt cost 12.
4. Inserta en `users` + asignaciones en `user_roles`.
5. Audit log: `user.created`.
6. Sistema envía credenciales por email (opcional, v2).

---

### UC-ADM-USR-002 — Activar 2FA TOTP

**RF cubiertos:** RF-USR-003.

**Flujo:**
1. AD selecciona usuario.
2. Sistema genera `totpSecret` y lo muestra como QR.
3. Usuario escanea con app authenticator.
4. AD ingresa código de verificación.
5. Sistema marca `requires_2fa = true`.

---

## Administrador — Clientes (mensualidades)

### UC-ADM-CLI-001 — Registrar cliente alquilado

**RF cubiertos:** RF-CLI-001.

**Flujo:**
1. AD entra a "Clientes".
2. Ingresa: documento, nombre, email, teléfono, vehículos (placas asociadas).
3. Sistema valida documento único por parking.
4. Audit log.

---

### UC-ADM-CLI-002 — Asignar plan de mensualidad

**RF cubiertos:** RF-CLI-002.

**Flujo:**
1. AD selecciona cliente.
2. Elige plan (mensual/trimestral/semestral/anual).
3. Sistema calcula `validFrom = ahora`, `validTo = ahora + duración`.
4. Registra `monthly_payments` (importe pagado, fecha, método).
5. Marca `customer.has_active_monthly = true`.
6. Audit log: `monthly.created`.

---

### UC-ADM-CLI-003 — Renovar plan

**RF cubiertos:** RF-CLI-003.

**Flujo:**
1. AD localiza cliente con plan vencido o por vencer.
2. Elige plan + medio de pago.
3. Sistema concatena vigencia (si vencido: nueva desde hoy; si vigente: nueva al expirar la actual).
4. Registra `monthly_payments`.
5. Audit log: `monthly.renewed`.

---

### UC-ADM-CLI-004 — Listar clientes por vencer

**RF cubiertos:** RF-CLI-005.

**Flujo:** AD ve dashboard "Mensualidades por vencer en 7 días" con filtros + acciones (renovar, contactar).

---

## Administrador — Reportes

### UC-ADM-REP-001 — Reporte de ingresos por rango

**RF cubiertos:** RF-REP-001, RF-REP-005, RF-REP-006.
**Estado actual:** **PARCIAL** — endpoint backend `/reports/revenue` existe; UI desktop muestra gráfico simple sin filtros de fecha; web-admin muestra 7 días fijos.

**Flujo:**
1. AD entra a "Reportes → Ingresos".
2. Selecciona rango (preset: hoy, ayer, semana, mes; o personalizado).
3. Aplica filtros (zona, tipo vehículo, operador).
4. Sistema muestra: total, IVA, por tipo, por día, top placas, conteo.
5. AD exporta a CSV/PDF.

---

### UC-ADM-REP-002 — Reporte de ocupación histórica

**RF cubiertos:** RF-REP-002.

**Flujo:** AD selecciona rango y ve serie temporal de ocupación por zona (TimescaleDB hyperfunctions `time_bucket`).

---

## Administrador — Configuración

### UC-ADM-CFG-001 — Editar datos fiscales

**RF cubiertos:** RF-CFG-001, RF-CFG-004.

**Flujo:**
1. AD entra a "Configuración → Datos Fiscales".
2. Edita NIT, razón social, dirección, resolución DIAN, serie de facturación.
3. Sistema valida formato NIT.
4. Guarda y audita.

---

### UC-ADM-CFG-002 — Editar horario operación

**RF cubiertos:** RF-CFG-002.

**Flujo:**
1. AD elige modo: 24 h o por franjas (lun-vie X-Y, sáb A-B, dom C-D).
2. Sistema valida no-solapamientos.
3. Audita.

---

## Auditoría

### UC-AUD-001 — Consultar log de auditoría

**RF cubiertos:** RF-AUD-003.
**Estado actual:** tabla `audit_log` existe en DB pero NO hay repositorio ni endpoint para consulta.

**Flujo esperado:**
1. AD entra a "Auditoría".
2. Filtra por actor, entidad, acción, rango temporal.
3. Sistema lista entradas (paginadas) con payload JSON expandible.
4. AD verifica integridad de cadena (botón "Verificar hash chain") — sistema recorre desde el primer registro y valida que cada `current_hash = SHA256(prev_hash || payload)`.

---

### UC-AUD-002 — Verificación nocturna de cadena

**Actor primario:** SYS.
**RF cubiertos:** RF-AUD-004.

**Flujo:**
1. Job nocturno recorre `audit_log` cronológicamente.
2. Recalcula `current_hash` esperado para cada entrada.
3. Si discrepa: alerta vía email + Slack al admin.

---

## Supervisión ejecutiva

### UC-SUP-001 — Dashboard ejecutivo multi-sucursal

**Actor primario:** SU.
**RF cubiertos:** RF-REP-001, RF-REP-002.

**Flujo:**
1. SU entra a web-admin con sus credenciales.
2. Sistema valida roles (SUPERADMIN o ADMIN multi-parking).
3. Sistema agrega KPIs de todos sus parqueaderos: ingresos hoy, ocupación promedio, alertas.
4. SU puede entrar al detalle de un parqueadero específico.

---

## Mapa UC ↔ RF ↔ Estado actual

| UC | RF principales | Estado |
|----|---------------|--------|
| UC-CAJ-001 Entrada eventual | RF-CAJ-001 | ✅ UI lista (MockData); falta cableado repo real |
| UC-CAJ-002 Cobro y cierre | RF-CAJ-002..006 | ✅ UI + TariffCalculator; falta impresión + cajón real |
| UC-CAJ-003 Sync | RF-CAJ-007 | ✅ SyncManager funcional, no integrado a UI |
| UC-CAJ-004 Cierre turno | RF-REP-003 | ❌ No existe |
| UC-CLI-001 Reserva PWA | parcial | 🟡 UI mockup, sin persistencia |
| UC-CLA-001 Mensualidad | RF-CAJ-008, RF-CLI-002 | 🟡 lógica en TariffCalculator (`appliedMonthly`), sin clientes reales |
| UC-ADM-TAR-001..004 Tarifas | RF-TAR-001..008 | ❌ UI read-only, backend sin CRUD |
| UC-ADM-ZON-001..002 Zonas | RF-ZON-001..004 | ❌ Sin CRUD ni endpoint |
| UC-ADM-USR-001..002 Usuarios | RF-USR-001..005 | ❌ Sin CRUD |
| UC-ADM-CLI-001..004 Clientes | RF-CLI-001..005 | 🟡 UI registro mock; sin backend |
| UC-ADM-REP-001..002 Reportes | RF-REP-001..006 | 🟡 Endpoints básicos sin filtros, sin export |
| UC-ADM-CFG-001..002 Config | RF-CFG-001..005 | 🟡 SettingsScreen local; sin backend |
| UC-AUD-001..002 Auditoría | RF-AUD-001..004 | ❌ Tabla existe; sin código ni endpoint |
| UC-SUP-001 Dashboard ejecutivo | RF-REP-001..002 | 🟡 web-admin con 4 KPIs hardcoded |
