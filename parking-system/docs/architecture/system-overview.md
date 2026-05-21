# Arquitectura — Visión completa del sistema

Este documento extiende `README.md` con detalle por módulo, contratos cross-platform, y flujos clave end-to-end. Para decisiones de diseño puntuales, ver `decisions/ADR-*.md`.

---

## 1. Vista de contenedores (C4 nivel 2)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          PARKING-SYSTEM                                 │
│                                                                         │
│   ┌────────────────┐    ┌────────────────┐    ┌────────────────┐        │
│   │ Caja Desktop   │    │ App móvil      │    │ Web PWA        │        │
│   │ (JVM kiosk)    │    │ Android / iOS  │    │ Wasm + SW      │        │
│   │ ESC/POS + HID  │    │ Cámara + LPR   │    │ IndexedDB      │        │
│   └───────┬────────┘    └────────┬───────┘    └────────┬───────┘        │
│           │   HTTPS+JWT          │                     │                │
│           └───────────┬──────────┴─────────────────────┘                │
│                       │                                                 │
│   ┌───────────────────▼───────────────────────────────┐                 │
│   │              BACKEND KTOR 3 (JVM)                 │                 │
│   │  REST (JSON) + WebSocket /ws/occupancy            │                 │
│   │  Auth JWT HS256 · Rate limit · Headers seguridad  │                 │
│   └─────┬─────────────────────────────────────┬───────┘                 │
│         │                                     │                         │
│   ┌─────▼──────┐   ┌────────────┐   ┌─────────▼──────────┐              │
│   │ PostgreSQL │   │ ai-service │   │ Web-admin (Next.js)│              │
│   │ 16 + Time- │   │ FastAPI    │   │ Reportes ejecutivos│              │
│   │ scaleDB    │   │ YOLO+Paddle│   │                    │              │
│   └────────────┘   └────────────┘   └────────────────────┘              │
└─────────────────────────────────────────────────────────────────────────┘
```

**Convenciones de comunicación:**

- Cliente → Backend: **REST JSON** sobre HTTPS con JWT Bearer.
- Backend → Cliente: **REST + WebSocket** (`/ws/occupancy`) para push de eventos.
- Backend → ai-service: **HTTP intra-LAN** con circuit breaker (no usado aún; topología preparada).
- Backend → DB: **JDBC + HikariCP + Exposed**, Flyway para migraciones.
- Cliente local → DB local: **SQLDelight** (Android / iOS / JVM / Wasm con drivers específicos).

---

## 2. Módulos del repositorio

| Módulo | Tecnología | Rol |
|--------|-----------|-----|
| `shared/` | KMP (commonMain + 4 actuals) | Dominio + motor de tarifas + repos cliente + cliente HTTP + drivers DB |
| `composeApp/` | Compose Multiplatform | UI compartida para Android / iOS / Desktop / Web |
| `backend/` | Ktor 3 + Exposed + Flyway | API HTTP + WebSocket + persistencia PostgreSQL |
| `iosApp/` | Xcode (Swift host) | Wrapper iOS que monta el `MainViewController` de Compose |
| `ai-service/` | Python 3.11 + FastAPI + YOLO + PaddleOCR | LPR, forecast, anti-fraude, insights |
| `web-admin/` | Next.js 14 + TypeScript + Tailwind | Portal ejecutivo (lectura) |
| `infrastructure/` | Docker / Kubernetes / Terraform | Despliegue |
| `docs/` | Markdown + OpenAPI | Documentación |

---

## 3. Capas dentro de `shared`

Clean architecture estricta — **Regla 2** del proyecto.

```
shared/src/commonMain/kotlin/com/parking/shared/
├── domain/
│   ├── model/       ← data classes, value classes, enums (PlateNumber, Money, ParkingSession, Tariff, ChargeBreakdown, ...)
│   ├── tariff/      ← TariffCalculator (motor único multiplataforma)
│   └── repository/  ← Interfaces (SessionRepository, TariffRepository, ...)
├── data/
│   ├── api/         ← ParkingApiClient + DTOs (Auth, Session, Catalog, Sync, Report, Lpr)
│   ├── local/       ← LocalRepository (SQLDelight) + DatabaseDriverFactory (expect)
│   └── sync/        ← SyncManager + ConnectivityMonitor (expect)
└── ...
```

**Regla:** `domain/*` no importa nada de `data/*`. `data/*` no importa nada de Ktor/SQLDelight en `commonMain` directamente — usa contratos `expect`.

---

## 4. Contratos `expect/actual`

Tres abstracciones cross-platform; cada una tiene 4 implementaciones:

### 4.1 `DatabaseDriverFactory`

```kotlin
// commonMain
expect class DatabaseDriverFactory {
    fun create(schema: SqlDriver.Schema): SqlDriver
}
```

| Plataforma | Implementación |
|-----------|---------------|
| `androidMain` | `AndroidSqliteDriver` (requiere `Context`) |
| `iosMain` | `NativeSqliteDriver` (sqlite3.framework) |
| `jvmMain` | `JdbcSqliteDriver` con archivo en `~/.parking-system/local.db` o in-memory para tests |
| `wasmJsMain` | `WebWorkerDriver` (sql.js + IndexedDB, async schema) |

### 4.2 `HttpClientFactory`

```kotlin
// commonMain
expect fun createHttpClientEngine(): HttpClientEngine
fun configureApplicationHttp(client: HttpClient) { /* ContentNegotiation, Timeout, Logging, Auth, defaultRequest */ }
```

| Plataforma | Engine |
|-----------|--------|
| `androidMain` | OkHttp |
| `iosMain` | Darwin |
| `jvmMain` | Java HttpClient (JDK 11+) |
| `wasmJsMain` | JS (fetch) |

### 4.3 `ConnectivityMonitor`

```kotlin
// commonMain
expect class ConnectivityMonitor {
    fun observe(): Flow<Boolean>
    fun isOnline(): Boolean
}
```

| Plataforma | Mecanismo |
|-----------|-----------|
| `androidMain` | `ConnectivityManager.NetworkCallback` |
| `iosMain` | `NWPathMonitor` |
| `jvmMain` | `InetAddress.isReachable` con polling |
| `wasmJsMain` | `navigator.onLine` + eventos `online/offline` |

---

## 5. Modelo de dominio (resumen)

| Tipo | Propósito |
|------|-----------|
| `PlateNumber` (value class) | Placa normalizada (uppercase, validación 5–10 chars) |
| `Money` (value class) | Importe en centavos. Operaciones aritméticas con redondeo HALF_UP. Formato COP |
| `VehicleType` (enum) | CAR, MOTORCYCLE, BICYCLE, TRUCK, BUS |
| `SessionStatus` (enum) | ACTIVE, CLOSED, CANCELLED, RESERVED |
| `ReceiptSyncStatus` (enum) | PENDING, SYNCED, CONFLICT, FAILED |
| `Tariff` | Tarifa por tipo + vigencia + recargo nocturno + gracia + IVA |
| `Zone` | Zona con id, código, capacidad, ocupación, tipos permitidos |
| `Vehicle` | Catálogo opcional (placa + tipo + propietario) |
| `Customer` | Cliente con plan; flag `hasActiveMonthly` |
| `ParkingSession` | Sesión activa o cerrada (placa, tipo, zona, entrada, salida, status, customer, operator) |
| `Receipt` | Comprobante fiscal con `localId` (idempotencia), CUFE DIAN, totales |
| `ChargeBreakdown` | Desglose del cobro (minutos, base, recargo, IVA, total, `appliedMonthly`, `withinGrace`) |

---

## 6. Motor de tarifas (`TariffCalculator`)

Único punto de cálculo monetario. Cualquier UI o test debe usar exclusivamente este motor (Regla 7).

**Entradas:** `tariff: Tariff`, `entryAt: Instant`, `exitAt: Instant`, `timeZone: TimeZone = America/Bogota`.

**Salida:** `ChargeBreakdown(billedMinutes, baseCents, nightSurchargeCents, subtotalCents, ivaCents, totalCents, appliedMonthly, withinGrace)`.

**Reglas implementadas:**

1. Si `exitAt - entryAt ≤ graceMinutes` → `withinGrace = true`, total = 0.
2. Si `customer.hasActiveMonthly` → `appliedMonthly = true`, total = 0 (sigue registrando horas).
3. Redondeo de horas **al alza** (1h 5min = 2h).
4. Tarifa bifásica: 1ª hora a `firstHourCents`, subsiguientes a `subsequentHourCents`.
5. Recargo nocturno: barre minuto a minuto entre `nightFrom` y `nightTo` (soporta cruce de medianoche). Aplica `nightSurchargePercent` sobre horas nocturnas.
6. IVA = subtotal × `ivaPercent / 100` con redondeo HALF_UP.
7. Operaciones en `Long` (centavos). Nunca `Double` ni `Float` (Regla 5).

---

## 7. Patrón Offline-first (Outbox)

```
┌──────────────────────────────────────────────────────────────┐
│                    CLIENTE (Caja/Móvil/Web)                  │
│                                                              │
│  1. OP cierra sesión                                         │
│     ↓                                                        │
│  2. LocalRepository.insertReceipt(localId=UUID, status=PENDING)│
│     ↓ (independiente de conectividad)                        │
│  3. Imprime tiquete físico                                   │
│                                                              │
│  ── ConnectivityMonitor.observe() ──→ online=true ──┐        │
│                                                     ↓        │
│  4. SyncManager.runOnce()                                    │
│     ↓                                                        │
│  5. POST /api/v1/sync/push {deviceId, receipts: [pending]}   │
│     ↓ (idempotente por (parking_id, local_id))               │
│  6. Server responde {accepted: [...], conflicts: [...]}      │
│     ↓                                                        │
│  7. UPDATE receipts SET sync_status=SYNCED, server_id=...    │
│     o sync_status=CONFLICT con last_error                    │
└──────────────────────────────────────────────────────────────┘
```

**Idempotencia:** la PK lógica del lado servidor es `(parking_id, local_id)`. Reintentos no duplican.

---

## 8. Sincronización inversa (Pull de catálogos)

Las cajas reciben tarifas y zonas del servidor:

```
GET /api/v1/sync/pull?since=<lastSyncMillis>
  ↓
Response: { tariffs: [TariffDto, ...], zones: [ZoneDto, ...] }
  ↓
LocalRepository.upsertTariffs() + upsertZones() (transaccional)
```

Esto garantiza que cuando AD edita una tarifa en backend, todas las cajas la reciben en su siguiente tick.

---

## 9. WebSocket de ocupancia

Endpoint: `/ws/occupancy`. Para dashboards en tiempo real.

```
Conexión → server envía {"type":"hello"}
SessionRoutes.open/close emite a MutableSharedFlow<OccupancyEvent>
  ↓
Cada cliente WS conectado recibe:
{"type":"occupancy","parkingId":"...","zone":"A1","occupancy":15,"capacity":20}
```

**Escalabilidad horizontal:** flag `realtime.redisEnabled` cambia el broadcaster a Redis Pub/Sub. No implementado en código pero la arquitectura lo prevé.

---

## 10. Esquema PostgreSQL (resumen, ver Flyway V1 para detalle)

```
parkings (id, code UNIQUE, name, tax_id, timezone, enabled)
└── users (id, parking_id FK, username, email, password_hash bcrypt, requires_2fa)
    └── refresh_tokens (id, user_id, token_hash SHA256, expires_at, revoked_at)
    └── user_roles (user_id, role_id) → roles (SUPERADMIN, ADMIN, CASHIER, VIEWER)
└── tariffs (id, parking_id, vehicle_type, prices, recargo, gracia, IVA, valid_from, valid_to)
└── zones (id, parking_id, code, capacity, current_occupancy, allowed_vehicle_types)
└── vehicles (id, parking_id, plate, type, owner)
└── customers (id, parking_id, full_name, document, contacto)
    └── monthly_payments (id, customer_id, amount, valid_from, valid_to, paid_at)
└── parking_sessions (id, parking_id, local_id UUID, plate, type, zone, customer, operator, entry, exit, status, total, IVA)  ← particionada por mes
└── receipts (id, parking_id, session_id, local_id UUID, sequence, subtotal, IVA, total, CUFE)
└── audit_log (id, parking_id, ts, action, entity, entity_id, actor_user_id, payload JSONB, prev_hash, current_hash SHA256)  ← particionada por mes
└── sync_log (id, parking_id, device_id, received_at, payload_size, accepted/conflict/rejected counts)
```

**Multi-tenancy:** toda tabla de negocio tiene `parking_id` FK (Regla 4). Validación en backend desde claim JWT `parkingId`.

---

## 11. Seguridad

| Control | Implementación |
|---------|----------------|
| Password storage | bcrypt cost 12 |
| Sesión | JWT HS256, access 1 h + refresh 30 d con rotación |
| Refresh token storage | hash SHA-256 en DB |
| 2FA | TOTP (`dev.samstevens.totp`), activable por usuario |
| Rate limit | global 60/min, `/auth` 5/min |
| Headers | HSTS 1 año, X-Frame-Options DENY, X-Content-Type-Options nosniff, Referrer-Policy strict-origin-when-cross-origin, CSP `default-src 'self'` |
| CORS | whitelist explícita |
| TLS | 1.3 en producción (reverse proxy gestiona certificados) |
| Multi-tenant | `parking_id` validado contra JWT claim |
| Auditoría | hash chain SHA-256 en `audit_log` (estructura lista, código pendiente) |

---

## 12. Estrategia de testing

| Capa | Framework | Cobertura actual |
|------|-----------|------------------|
| `shared/commonTest` | Kotest | ✅ `TariffCalculatorTest` (motor cubierto, corre en 4 plataformas) |
| `shared/jvmTest` `androidHostTest` `iosTest` `jsTest` | Kotest | Re-ejecuta los tests common por plataforma para verificar paridad |
| `backend` | Kotest + Ktor test host | ❌ Sin tests (esqueletos) |
| `composeApp` | Compose UI testing | ❌ No hay tests |
| `ai-service` | pytest | 🟡 smoke tests (`/health`, `/lpr` stub) |
| `web-admin` | — | ❌ No hay tests |

**Deuda crítica:** falta cobertura en `SyncManager`, `LocalRepository`, backend repositorios, flujos de UI críticos.

---

## 13. Flujos end-to-end clave

### 13.1 Cobro completo (offline + sync)

```
1. OP escanea placa "ABC123" (lector HID)
2. CashierScreen.findActiveByPlate("ABC123") → LocalRepository (DB local) → ParkingSession
3. previewCharge() → TariffCalculator.calculate(...) → ChargeBreakdown
4. OP confirma → AppState.closeSession() → LocalRepository.closeSession() + insertReceipt(PENDING)
5. ThermalPrinter.printReceipt(text, openDrawer=true)
6. SyncManager (background) → POST /sync/push → 200 → markReceiptSynced(server_id)
```

### 13.2 Login

```
1. UI → POST /api/v1/auth/login {username, password, totp?}
2. AuthRepository.findByUsername → bcrypt.verify(password, hash)
3. (Opcional) Validar TOTP si requires_2fa
4. Genera access JWT (1h) + refresh token (30d, hash SHA256 en DB)
5. Devuelve LoginResponse {accessToken, refreshToken, expiresIn, user{roles, parkingId, ...}}
6. Cliente guarda tokens; HttpClientFactory inyecta Bearer en defaultRequest
```

### 13.3 Refresh

```
1. UI detecta 401 o token próximo a expirar → POST /api/v1/auth/refresh {refreshToken}
2. AuthRepository.validateRefreshToken (SHA256 lookup + not revoked + not expired)
3. rotateRefreshToken: marca el viejo revocado, emite uno nuevo
4. Devuelve nuevo access + refresh
```

### 13.4 Edición de tarifa por admin (flujo objetivo — NO IMPLEMENTADO)

```
1. AD en TariffManagementScreen → "Editar tarifa CAR"
2. UI → PUT /api/v1/parkings/{id}/tariffs/{id} {nuevosCampos}  ← endpoint inexistente
3. Backend valida + crea nueva versión (valid_from=now), cierra anterior
4. Inserta audit_log con hash chain
5. Cajas en su siguiente /sync/pull reciben la nueva tarifa
6. LocalRepository.upsertTariffs() la persiste; UI refresca
```

---

## 14. Despliegue (resumen)

Ver `docs/deployment/README.md` para detalle.

- **Caja desktop:** instalador JVM (jpackage) o distZip; servicio Windows.
- **Móvil:** Play Store / App Store.
- **PWA:** static hosting (CDN) detrás del backend o aparte.
- **Backend:** Docker en Kubernetes (manifests en `infrastructure/kubernetes/`).
- **PostgreSQL:** managed (RDS / Cloud SQL) en producción.
- **ai-service:** Docker; opcionalmente GPU para LPR pesado.

---

## 15. Decisiones registradas

- ADR-001 — Kotlin Multiplatform como base.
- ADR-002 — Python aparte para subsistema IA.
- ADR-003 — Offline-first en todos los clientes.
- ADR-004 — Estrategia de sincronización con outbox pattern.

Ver `docs/decisions/`.
