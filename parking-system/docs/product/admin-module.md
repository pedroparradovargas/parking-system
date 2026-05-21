# Módulo Administrador — Especificación + Gap Analysis + Plan

**Objetivo:** definir todo lo necesario para que un administrador de parqueadero opere un parqueadero moderno y óptimo desde la app — configurar tarifas, periodicidad, precios, zonas, usuarios, clientes mensuales, ver reportes, auditar acciones y gestionar periféricos.

---

## 1. Alcance funcional del módulo Admin

Un admin necesita gestionar **ocho áreas**:

1. **Tarifas** — qué cobrar, cuándo, a quién.
2. **Zonas** — espacio físico y reglas de uso.
3. **Usuarios / roles** — quién opera el sistema y con qué permisos.
4. **Clientes (mensualidades)** — base de datos de clientes y planes.
5. **Reportes** — ingresos, ocupación, productividad, ranking.
6. **Configuración del parqueadero** — datos fiscales, horarios, IVA, integraciones.
7. **Auditoría** — quién hizo qué, cuándo, con integridad criptográfica.
8. **Periféricos** — estado de impresora, lector, cajón, datafono.

---

## 2. Especificación detallada por área

### 2.1 Tarifas

**Modelo de datos requerido** (ya existe en `tariffs` table):

```
Tariff {
  id UUID
  parking_id UUID
  vehicle_type CAR | MOTORCYCLE | BICYCLE | TRUCK | BUS
  first_hour_cents BIGINT
  subsequent_hour_cents BIGINT
  night_surcharge_percent INT (0..100)
  night_from TIME
  night_to TIME
  grace_minutes INT (0..60)
  iva_percent INT (0..100, default 19)
  valid_from TIMESTAMP
  valid_to TIMESTAMP NULL
}
```

**Política de versionado:** las tarifas **NO se sobrescriben**. Editar produce una nueva versión y cierra `valid_to` de la anterior. Esto preserva la historia para auditoría y para recalcular cobros pasados.

**Operaciones que debe ofrecer la UI:**

- **Listar** tarifas vigentes, programadas (futuras) e históricas, por tipo de vehículo.
- **Crear** nueva tarifa (formulario con preview de cobro de ejemplo).
- **Editar** → crea nueva versión.
- **Programar** entrada en vigencia futura (`valid_from > now`).
- **Cerrar** tarifa anticipadamente.
- **Duplicar** tarifa a otro tipo de vehículo.

**Planes de mensualidad** (extensión, requiere nueva tabla `tariff_plans`):

```
TariffPlan {
  id UUID
  parking_id UUID
  name TEXT  -- "Mensual", "Trimestral", "Semestral", "Anual"
  duration_days INT
  price_cents BIGINT
  vehicle_type ENUM  -- aplica a qué tipo
  enabled BOOL
  created_at TIMESTAMP
}
```

UI muestra plan + descuento implícito vs. cobrar por hora (calculado en cliente).

**Tarifas especiales** (extensión):

```
SpecialTariff {
  id UUID
  parking_id UUID
  name TEXT  -- "Fin de semana", "Festivo", "Evento Concierto X"
  rule_type ENUM  -- WEEKEND | HOLIDAY | DATE_RANGE | DAY_OF_WEEK
  multiplier DECIMAL(3,2)  -- 1.20 = +20%, 0.80 = -20%
  date_from DATE NULL
  date_to DATE NULL
  enabled BOOL
}
```

Calendario de festivos editable; por defecto carga lista festivos Colombia.

---

### 2.2 Zonas

**Modelo** (existe en `zones`, extender):

```
Zone {
  id UUID
  parking_id UUID
  code TEXT  -- "A1", "VIP", "Motos"
  capacity INT
  current_occupancy INT  -- snapshot en vivo
  allowed_vehicle_types TEXT  -- CSV: "CAR,SUV"
  under_maintenance INT DEFAULT 0  -- NEW: cupos fuera de servicio
  enabled BOOL DEFAULT TRUE      -- NEW: zona desactivada
  display_order INT              -- NEW: orden en UI
  notes TEXT                     -- NEW: observaciones internas
}
```

**Operaciones UI:**

- Listar zonas con barra de ocupación + estado.
- Crear / editar / desactivar.
- Marcar cupos en mantenimiento (capacidad efectiva = `capacity - under_maintenance`).
- Reglas por zona (tipo permitido).

---

### 2.3 Usuarios y roles

**Modelos existentes** (`users`, `roles`, `user_roles`, `permissions`, `role_permissions`).

**Roles propuestos:**

- **SUPERADMIN:** multi-parking; gestiona admins.
- **ADMIN:** gestiona un parking (todo el módulo admin).
- **CASHIER:** opera la caja (entrada/salida, cobro).
- **VIEWER:** solo lectura (reportes, dashboards).

**Permisos atómicos** (refinables, ejemplos):
`tariff.read`, `tariff.write`, `zone.read`, `zone.write`, `user.read`, `user.write`, `customer.read`, `customer.write`, `report.read`, `audit.read`, `session.open`, `session.close`, `config.write`, `device.manage`.

**Operaciones UI:**

- Listar usuarios (filtro por rol, estado).
- Crear usuario (form: username, email, nombre, password inicial, roles).
- Editar (excepto password).
- Resetear password (token de un solo uso).
- Activar/desactivar.
- Activar 2FA (genera QR TOTP).

---

### 2.4 Clientes alquilados (mensualidades)

**Modelos existentes** (`customers`, `monthly_payments`).

**Extensión:** tabla pivot `customer_vehicles` (placa(s) por cliente).

```
CustomerVehicle {
  id UUID
  customer_id UUID
  plate TEXT
  vehicle_type ENUM
  primary BOOL  -- placa principal
  created_at TIMESTAMP
}
```

**Operaciones UI:**

- Listar clientes con plan vigente / próximo a vencer / vencido.
- Crear cliente + asociar vehículos.
- Asignar plan (de los configurados en 2.1).
- Renovar plan (validez se concatena al actual o desde hoy si vencido).
- Suspender / cancelar.
- Ver historial de pagos.

---

### 2.5 Reportes

**Endpoints existentes:** `/api/v1/parkings/{id}/reports/revenue?from=&to=` y `/reports/occupancy`.

**Falta para v1:**

- Filtros: zona, tipo de vehículo, operador, rango horario.
- Granularidad: día / hora / semana / mes.
- Export CSV (botón en UI; backend genera o cliente serializa).
- Export PDF (backend con plantilla incluyendo datos fiscales).
- Reporte "Cierre de caja" por turno y operador.
- Reporte "Mensualidades" (vigentes, vencidas, ingresos por renovación).
- Top placas (más frecuentes / más tiempo).

---

### 2.6 Configuración del parqueadero

**Modelo extendido** (la tabla `parkings` ya existe, agregar columnas o tabla `parking_config`):

```
ParkingConfig {
  parking_id UUID PK
  legal_name TEXT
  tax_id TEXT
  legal_address TEXT
  city TEXT
  dian_resolution TEXT
  dian_resolution_from TIMESTAMP
  dian_resolution_to TIMESTAMP
  invoice_series TEXT
  invoice_next_sequence BIGINT
  timezone TEXT DEFAULT 'America/Bogota'
  operating_mode ENUM  -- '24x7' | 'CUSTOM'
  operating_schedule JSONB  -- {mon: [{from:'08:00', to:'22:00'}], ...}
  ai_service_url TEXT
  notification_email TEXT
  updated_at TIMESTAMP
  updated_by UUID
}
```

**Operaciones UI:**

- Editar datos fiscales (validación NIT).
- Editar horarios (UI con grilla por día).
- Configurar zona horaria.
- Configurar conexiones (URL backend, ai-service).
- Configurar plantilla de recibo (encabezado, pie, logos).

---

### 2.7 Auditoría

**Tabla `audit_log` existe** pero no está poblada.

**Acciones que DEBEN auditarse:**

| Acción | Entidad | Quién |
|--------|---------|-------|
| `login.success` / `login.failure` | user | sistema |
| `refresh.success` | user | sistema |
| `tariff.created` / `tariff.updated` / `tariff.closed` | tariff | admin |
| `tariff_plan.created` / `tariff_plan.updated` | tariff_plan | admin |
| `zone.created` / `zone.updated` / `zone.disabled` | zone | admin |
| `user.created` / `user.updated` / `user.disabled` / `password.reset` | user | admin |
| `customer.created` / `customer.updated` | customer | admin |
| `monthly.created` / `monthly.renewed` / `monthly.cancelled` | monthly | admin |
| `session.opened` / `session.closed` | session | operador |
| `cash.closed` (cierre de turno) | cash_session | operador |
| `config.updated` | parking_config | admin |

**Mecánica hash chain:**

```python
current_hash = SHA256(prev_hash || ts || action || entity || entity_id || actor_user_id || canonical_json(payload))
```

`prev_hash` del primer registro = "0" * 64. Cada inserción consulta el último `current_hash` para encadenar.

**Operaciones UI:**

- Listado con filtros (actor, entidad, acción, fechas).
- Detalle con payload JSON expandido.
- Verificación de integridad: botón que recorre todo el log y valida hashes.
- Alertas si discrepancia (job nocturno).

---

### 2.8 Periféricos

**Implementaciones existentes (desktop):**

- `ThermalPrinter.kt` — ESC/POS via `javax.print`. Listo.
- `BarcodeScanner.kt` — SharedFlow<String> (lector HID). Listo.
- `CashDrawer.kt` — comando ESC/POS al imprimir. Listo.

**Falta UI:**

- Pantalla "Periféricos" en módulo admin.
- Test de impresora (botón "Imprimir prueba").
- Test de cajón.
- Configurar nombre/marca de impresora (selector si hay varias).
- Estado en vivo (online/offline).
- Log de impresiones (opcional).

**Datafono (v2):** interfaz abstracta + adapters para distintos proveedores.

---

## 3. Gap analysis vs. estado actual

### 3.1 Resumen por área

| Área | UI cliente (composeApp) | Backend endpoints | DB schema | Gap principal |
|------|------------------------|-------------------|-----------|---------------|
| Tarifas | 🟡 read-only hardcoded | ❌ sin CRUD | ✅ tablas listas | Backend CRUD + UI con formulario + endpoint + sync |
| Tarifas — planes | ❌ tabla "Suscripciones" hardcoded | ❌ tabla no existe | ❌ falta `tariff_plans` | Crear tabla + CRUD + UI |
| Tarifas — especiales | ❌ tabla hardcoded | ❌ tabla no existe | ❌ falta `special_tariffs` + `holidays` | Crear todo |
| Zonas | 🟡 visualización ocupación | ❌ sin CRUD | ✅ tabla existe | Backend CRUD + UI editar + sync |
| Usuarios | ❌ no existe pantalla | ❌ sin CRUD | ✅ tablas listas | Backend CRUD + UI + endpoint reset password |
| Permisos granulares | ❌ | ❌ asignaciones fijas | ✅ tablas `permissions`, `role_permissions` | Endpoint para gestionar + UI |
| Clientes | 🟡 form de alta mock | ❌ sin repo ni rutas | ✅ tablas existen | Backend CRUD + UI listado + plan + renovación |
| Reportes | 🟡 dashboard view-only | 🟡 revenue + occupancy básicos | OK | Filtros, export CSV/PDF, cierre caja |
| Configuración | 🟡 SettingsScreen local (URL backend, tema) | ❌ sin endpoint | 🟡 columnas básicas en `parkings`, falta tabla `parking_config` o extender | Backend GET/PUT + UI |
| Auditoría | ❌ no existe pantalla | ❌ sin escritura ni lectura | ✅ tabla existe | Implementar `AuditRepository.append()` + endpoint consulta + UI + job verificación |
| Periféricos | ❌ no existe pantalla | ❌ no aplica (local) | ❌ no aplica | UI test/calibración + log opcional |

### 3.2 Backend — endpoints faltantes

```
Tarifas
  POST   /api/v1/parkings/{id}/tariffs                 (crear nueva versión)
  PUT    /api/v1/parkings/{id}/tariffs/{tariffId}      (editar = crear nueva versión)
  DELETE /api/v1/parkings/{id}/tariffs/{tariffId}      (cerrar vigencia ahora)
  GET    /api/v1/parkings/{id}/tariffs?historic=true   (extender existente con filtro)

Planes
  GET/POST/PUT/DELETE /api/v1/parkings/{id}/tariff-plans

Tarifas especiales
  GET/POST/PUT/DELETE /api/v1/parkings/{id}/special-tariffs

Zonas
  POST/PUT/DELETE /api/v1/parkings/{id}/zones

Usuarios
  GET    /api/v1/parkings/{id}/users
  POST   /api/v1/parkings/{id}/users
  PUT    /api/v1/parkings/{id}/users/{userId}
  POST   /api/v1/parkings/{id}/users/{userId}/disable
  POST   /api/v1/parkings/{id}/users/{userId}/reset-password
  POST   /api/v1/parkings/{id}/users/{userId}/2fa/enable
  POST   /api/v1/parkings/{id}/users/{userId}/2fa/disable

Clientes
  GET    /api/v1/parkings/{id}/customers
  POST   /api/v1/parkings/{id}/customers
  PUT    /api/v1/parkings/{id}/customers/{customerId}
  POST   /api/v1/parkings/{id}/customers/{customerId}/monthly  (asignar plan)
  POST   /api/v1/parkings/{id}/customers/{customerId}/monthly/{monthlyId}/renew
  POST   /api/v1/parkings/{id}/customers/{customerId}/monthly/{monthlyId}/cancel

Reportes adicionales
  GET    /api/v1/parkings/{id}/reports/cash-closing?from=&to=&operator=
  GET    /api/v1/parkings/{id}/reports/monthly-customers
  GET    /api/v1/parkings/{id}/reports/top-plates?limit=10&period=
  POST   /api/v1/parkings/{id}/reports/export        (body: type + filters, devuelve PDF/CSV)

Configuración
  GET/PUT /api/v1/parkings/{id}/config

Auditoría
  GET    /api/v1/parkings/{id}/audit?entity=&action=&from=&to=&actor=
  POST   /api/v1/parkings/{id}/audit/verify          (verifica hash chain)
```

### 3.3 Migraciones DB necesarias (Flyway V2..)

```sql
-- V2__admin_extension.sql

-- Planes de mensualidad
CREATE TABLE tariff_plans (...);

-- Tarifas especiales + festivos
CREATE TABLE special_tariffs (...);
CREATE TABLE holidays (...);

-- Cliente ↔ vehículos
CREATE TABLE customer_vehicles (...);

-- Cierre de caja
CREATE TABLE cash_sessions (
  id UUID PK,
  parking_id UUID FK,
  operator_user_id UUID FK,
  opened_at TIMESTAMP,
  closed_at TIMESTAMP NULL,
  total_cash_cents BIGINT,
  total_card_cents BIGINT,
  total_other_cents BIGINT,
  sessions_count INT,
  hash_chain VARCHAR(64)
);

-- Extender zones
ALTER TABLE zones ADD COLUMN under_maintenance INT DEFAULT 0;
ALTER TABLE zones ADD COLUMN enabled BOOL DEFAULT TRUE;
ALTER TABLE zones ADD COLUMN display_order INT;
ALTER TABLE zones ADD COLUMN notes TEXT;

-- Configuración (extender parkings o tabla aparte)
CREATE TABLE parking_config (...);  -- según 2.6
```

---

## 4. Arquitectura del módulo Admin

### 4.1 En `composeApp`

Nueva sección "Admin" accesible solo a usuarios con rol ADMIN/SUPERADMIN. Sub-secciones:

```
ui/screens/admin/
├── AdminHomeScreen.kt              ← landing con accesos directos
├── tariffs/
│   ├── TariffListScreen.kt
│   ├── TariffEditScreen.kt         ← formulario nueva versión
│   ├── PlanListScreen.kt
│   └── SpecialTariffListScreen.kt
├── zones/
│   ├── ZoneListScreen.kt
│   └── ZoneEditScreen.kt
├── users/
│   ├── UserListScreen.kt
│   └── UserEditScreen.kt
├── customers/
│   ├── CustomerListScreen.kt
│   ├── CustomerEditScreen.kt
│   └── MonthlyAssignScreen.kt
├── reports/
│   ├── RevenueReportScreen.kt
│   ├── OccupancyReportScreen.kt
│   └── CashClosingReportScreen.kt
├── config/
│   └── ParkingConfigScreen.kt
├── audit/
│   └── AuditLogScreen.kt
└── devices/
    └── DeviceStatusScreen.kt
```

**Refactor de `TariffManagementScreen` actual:** convertirlo en `TariffListScreen` + sub-pantallas. Cumple regla 300 líneas.

### 4.2 En `shared`

Nuevos contratos:

```kotlin
domain/repository/
  AdminRepositories.kt
    interface AdminTariffRepository { listAll/create/update/close; planes; especiales }
    interface AdminZoneRepository
    interface AdminUserRepository
    interface AdminCustomerRepository
    interface AdminReportRepository  // extiende ReportRepository con filtros + export
    interface AdminConfigRepository
    interface AdminAuditRepository
    interface DeviceStatusRepository

data/api/
  AdminApiClient.kt   // o extender ParkingApiClient
```

DTOs adicionales: `TariffPlanDto`, `SpecialTariffDto`, `HolidayDto`, `CustomerDto`, `CustomerVehicleDto`, `MonthlyDto`, `CashSessionDto`, `AuditEntryDto`, `ParkingConfigDto`, `DeviceStatusDto`.

### 4.3 En `backend`

Nuevos archivos:

```
routes/
  AdminTariffRoutes.kt
  AdminZoneRoutes.kt
  AdminUserRoutes.kt
  AdminCustomerRoutes.kt
  AdminConfigRoutes.kt
  AuditRoutes.kt
  (extender) OtherRoutes.kt para reportes adicionales

domain/
  AdminTariffRepository.kt
  AdminZoneRepository.kt
  AdminUserRepository.kt
  AdminCustomerRepository.kt
  AdminConfigRepository.kt
  AuditRepository.kt           ← inserta entradas con hash chain
  CashClosingRepository.kt
```

**Decorator/aspect:** todas las rutas de admin deben pasar por un middleware que:
- Valida rol (ADMIN o SUPERADMIN; superadmin para multi-parking).
- Valida `parking_id` claim vs. path param (rechazo cross-tenant).
- Inserta entrada en `audit_log` con hash chain antes/después de la mutación.

### 4.4 En `web-admin`

Replicar lectura de tarifas, zonas, usuarios, clientes, reportes y auditoría como páginas Next. El portal Web queda como **alternativa solo-lectura** (decisión: edición primaria en composeApp Desktop por preferencia operativa del admin; edición secundaria en web-admin en fase 3).

---

## 5. Plan de implementación por fases

### Fase A — Fundamentos (1–2 sprints)

**Objetivos:** habilitar persistencia real, audit_log, refactor.

- A1. Cablear `AppState` a `LocalRepository` real (Koin DI; eliminar dependencia exclusiva de `MockData`).
- A2. Crear `AuditRepository` backend + integrar en rutas existentes (login, sesiones).
- A3. Partir `CashierScreen`, `DashboardScreen`, `TariffManagementScreen` en archivos < 300 líneas.
- A4. Generar/validar `openapi.yaml` actual.
- A5. Tests básicos: `AuthRepository`, `SessionRepository`, `SyncManager`.

### Fase B — Admin Tarifas (2 sprints)

- B1. Migración Flyway: `tariff_plans`, `special_tariffs`, `holidays`.
- B2. Backend: `AdminTariffRepository` + `AdminTariffRoutes` (CRUD + planes + especiales).
- B3. Shared: DTOs + `AdminTariffRepository` interface + client.
- B4. UI: `TariffListScreen` + `TariffEditScreen` con formulario completo + validaciones.
- B5. Audit log enganchado en cada mutación.
- B6. SyncManager: pulls de tarifas/planes/especiales en `/sync/pull`.
- B7. Tests UI flow.

### Fase C — Admin Zonas (1 sprint)

- C1. Migración: extender `zones` (under_maintenance, enabled, display_order, notes).
- C2. Backend `AdminZoneRoutes`.
- C3. UI `ZoneListScreen` + `ZoneEditScreen`.
- C4. Audit + sync.

### Fase D — Admin Usuarios + 2FA (1 sprint)

- D1. Backend `AdminUserRoutes` (CRUD + reset + 2FA enable/disable).
- D2. UI `UserListScreen` + `UserEditScreen` + QR TOTP.
- D3. Audit.

### Fase E — Admin Clientes + mensualidades (2 sprints)

- E1. Migración: `customer_vehicles`.
- E2. Backend `AdminCustomerRoutes` (CRUD + asignar plan + renovar + cancelar).
- E3. UI `CustomerListScreen` + `CustomerEditScreen` + `MonthlyAssignScreen`.
- E4. Integrar en `TariffCalculator`: cuando placa pertenece a cliente con plan vigente, `appliedMonthly = true` (ya soportado conceptualmente; falta cableado real DB → cálculo).
- E5. Audit.

### Fase F — Reportes avanzados (1–2 sprints)

- F1. Migración: `cash_sessions`.
- F2. Backend: endpoints con filtros + export CSV/PDF.
- F3. UI: pantallas de reporte con filtros + export.
- F4. Cierre de caja flow (operador) + reporte (admin).

### Fase G — Configuración + Auditoría UI (1 sprint)

- G1. Migración: `parking_config`.
- G2. Backend `AdminConfigRoutes`.
- G3. UI `ParkingConfigScreen`.
- G4. UI `AuditLogScreen` + endpoint `/audit/verify`.
- G5. Job nocturno de verificación.

### Fase H — Periféricos UI (1 sprint)

- H1. UI `DeviceStatusScreen` con test impresora / cajón / lector.
- H2. Integrar `BarcodeScanner.scans` con `PlateInput` en pantallas de cobro (HU-PER-002).
- H3. Cablear `ThermalPrinter.printReceipt` en flujo de cierre de sesión (HU-PER-001).
- H4. Log de impresiones (opcional).

**Total estimado:** 9–11 sprints (∼5 meses calendario para un dev senior fullstack KMP).

---

## 6. Riesgos del módulo Admin

| Riesgo | Mitigación |
|--------|-----------|
| Tarifa mal configurada cobra incorrecto a clientes en curso | Preview obligatorio antes de guardar (mostrar cálculo de ejemplo con 1 h, 4 h, nocturno). Vigencia futura por default, no inmediata. |
| Borrar usuario corrompe historial de sesiones | Soft-delete (`enabled=false`); preserva FK. |
| Cliente con plan vigente cierra cobro doble si la caja se desincroniza | Cliente decide vía DB local (rápido) y servidor confirma en sync. Idempotencia por `local_id` evita doble cobro. |
| Audit log inflado | Particionado mensual + retención configurable (ej. 7 años para fiscal); query con índices sobre `parking_id`, `ts`, `actor_user_id`. |
| Performance de listas grandes (clientes, audit) | Paginación servidor-side + cursor-based para audit. |
| 2FA bloqueando operadores que pierden el dispositivo | Reset por SUPERADMIN. |

---

## 7. Checklist de "definition of done" para el módulo Admin

- [ ] Toda mutación admin pasa por audit log con hash chain válido.
- [ ] Toda mutación admin valida rol + parking_id del JWT vs. path param.
- [ ] Toda nueva versión de tarifa se persiste sin sobrescribir la anterior.
- [ ] Tests automatizados de cada endpoint admin (happy + multi-tenant cross + audit).
- [ ] Tests UI smoke para los flujos críticos.
- [ ] OpenAPI actualizado.
- [ ] Documentación de uso en `docs/product/admin-module.md` (este documento) actualizada con capturas.
- [ ] Migraciones Flyway versionadas (V2, V3, ...) con plan de rollback.
- [ ] Job nocturno de verificación de cadena audit en producción.

---

## 8. Decisiones abiertas (a confirmar con producto)

1. **Edición primaria:** ¿el admin edita desde la **caja desktop** (composeApp) o desde el **web-admin Next.js**? Recomendación: composeApp como primary (offline-friendly), web-admin como alternativa cloud para multi-sucursal.
2. **Permisos granulares:** ¿Implementar permisos por acción (ej. solo "tariff.read") o quedarse con roles fijos (ADMIN/CASHIER)? Recomendación v1: roles fijos; v2: granulares para clientes enterprise.
3. **Tarifas multi-rango horario** (no sólo nocturno/diurno, sino p.ej. franjas pico/valle): ¿requerido en v1? Recomendación: v2.
4. **Datafono:** ¿integración real con qué proveedor (Mercado Pago, Wompi, PayU)? Decidir antes de v2.
5. **Facturación electrónica DIAN:** ¿integración propia o vía proveedor (Alegra, Siigo)? Decidir antes de v1 cerrada.
