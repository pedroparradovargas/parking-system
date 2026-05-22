# Auditoría e Inventario de Código

**Fecha original:** 2026-05-21. **Actualizado:** 2026-05-22 (Fases A + B + C completas).

> **Cambios desde la auditoría inicial** (resumen rápido — detalle en `project_parking_system_rules.md`):
> - Fase A — LocalRepository cableado a AppState; AuditRepository con hash chain; archivos partidos < 300; openapi validado; 4 tests AuditRepository.
> - Fase B — CRUD Tarifas + Planes + Especiales + Holidays end-to-end; 8 tests AdminTariff.
> - Fase C — CRUD Zonas end-to-end; 8 tests AdminZone.
> - Consolidación — UI Planes/Especiales completa; SQLDelight local para los 3 catálogos admin; openapi extendido; 20 tests backend pasan.
>
> **Total tests backend al 2026-05-22**: 20 (4 + 8 + 8). Antes era 1 (TariffCalculator).

**Alcance:** todos los módulos bajo `parking-system/` (shared, composeApp, backend, ai-service, web-admin).
**Metodología:** lectura directa de archivos + grep cruzados para detectar referencias.

> ⚠️ Existe un **segundo proyecto Gradle** en la raíz del repo (`E:/aplicaciones moviles/parqueaderos/` — name `parqueaderos`, módulos `:androidApp :desktopApp :shared :webApp`). Es un **template stock KMP** ("Click me!" button) y NO es el sistema real. No se audita en este documento.

---

## 1. Inventario por módulo

### 1.1 `shared/` (Kotlin Multiplatform)

| Carpeta | Archivos | Resumen |
|---------|----------|---------|
| `domain/model/Models.kt` | 1 | 11 tipos: PlateNumber, Money, VehicleType, SessionStatus, ReceiptSyncStatus, Tariff, Zone, Vehicle, Customer, ParkingSession, Receipt, ChargeBreakdown |
| `domain/tariff/TariffCalculator.kt` | 1 | Motor único de cálculo (162 líneas) |
| `domain/repository/Repositories.kt` | 1 | 5 interfaces: SessionRepository, TariffRepository, ZoneRepository, CustomerRepository, ReceiptRepository |
| `data/api/ParkingApiClient.kt` + DTOs | 7 | Cliente Ktor + AuthDtos, CatalogDtos, SessionDtos, SyncDtos, ReportDtos, LprDtos |
| `data/api/HttpClientFactory.kt` | 1 commonMain + 4 actuals | `expect/actual` por plataforma |
| `data/local/LocalRepository.kt` + DriverFactory | 2 + 4 actuals | LocalRepository con SQLDelight; DatabaseDriverFactory `expect/actual` |
| `data/sync/SyncManager.kt` + ConnectivityMonitor | 2 + 4 actuals | Sync con outbox + Monitor `expect/actual` |
| `commonMain/sqldelight/*.sq` | 7 | tariffs, zones, sessions, receipts, customers, audit, ParkingDatabase |
| `commonTest/.../TariffCalculatorTest.kt` | 1 | 6 escenarios cubiertos |

**Total Kotlin:** ~27 archivos commonMain + 12 actuals = 39 archivos productivos.

### 1.2 `composeApp/` (Compose Multiplatform UI)

| Carpeta | Archivos | Resumen |
|---------|----------|---------|
| `commonMain/.../App.kt` + `state/` | 3 | App.kt, AppState.kt, MockData.kt |
| `commonMain/.../navigation/Screen.kt` | 1 | Sealed interface + AppNav |
| `commonMain/.../ui/shell/` | 3 | AppShell, HeaderBar, TabNavBar |
| `commonMain/.../ui/theme/` | 2 | ParkingTheme, Tokens |
| `commonMain/.../ui/components/` | 10 | KpiCard, AccentKpi, StatusPill, SectionHeader, SectionTitle, OfflineBanner, EmptyState, PlateInput, VehicleTypeSelector, ShadcnPrimitives |
| `commonMain/.../ui/screens/` | 13 archivos en 11 paquetes | Ver detalle abajo |
| `commonMain/.../di/AppModule.kt` | 1 | Koin module común |
| `androidMain/.../` | 2 | MainActivity, ParkingApplication |
| `desktopMain/.../` | 5 | Main, DesktopModule, ThermalPrinter, BarcodeScanner, CashDrawer |
| `iosMain/.../MainViewController.kt` | 1 | Wrapper iOS |
| `wasmJsMain/.../` | 2 | main.kt, PwaManifest.kt |

**Pantallas registradas en `Screen.kt`:**

| Screen | Archivo | Líneas | Estado |
|--------|---------|-------|--------|
| MainMenu | `mainmenu/MainMenuScreen.kt` | 293 | ✅ UI completa, navegación |
| RentedEntry | `rented/RentedVehicleEntryScreen.kt` | — | 🟡 wizard mock |
| EventualEntry / Cashier | `cashier/CashierScreen.kt` | **405** | ✅ funcional sobre AppState |
| Payment | `payment/PaymentInterfaceScreen.kt` | — | 🟡 wizard mock |
| ClientRegistration | `registration/ClientRegistrationScreen.kt` | — | 🟡 form mock |
| TariffManagement | `tariff/TariffManagementScreen.kt` | 405 | 🟡 read-only, sin CRUD |
| Availability | `availability/ParkingAvailabilityScreen.kt` | — | ✅ visualización |
| Reports → Dashboard | `dashboard/DashboardScreen.kt` | **462** | ✅ visualización gráficos |
| Support | `support/CustomerSupportScreen.kt` | — | 🟡 form mock |
| Cashier (interno) | igual que EventualEntry | | |
| Reservation | `reservation/ReservationScreen.kt` | — | 🟡 mock |
| PaymentQr | `reservation/PaymentQrScreen.kt` | — | 🟡 mock |

### 1.3 `backend/` (Ktor 3)

| Carpeta | Archivos | Resumen |
|---------|----------|---------|
| `Application.kt` | 1 | Entrypoint Ktor |
| `config/` | 4 | Configurations, Database (Hikari + Exposed + Flyway), Security (JWT + bcrypt), Websockets |
| `routes/` | 4 | AuthRoutes, SessionRoutes, SyncRoutes, OtherRoutes |
| `domain/` (repos) | 4 | AuthRepository, SessionRepository, SyncRepository, ReportsRepository |
| `resources/db/migration/` | (V1__init.sql) | 15 tablas + particiones |

**Total backend:** 13 archivos Kotlin + migraciones SQL.

### 1.4 `ai-service/` (FastAPI)

| Carpeta | Archivos | Resumen |
|---------|----------|---------|
| `app/main.py` | 1 | Factory + lifespan warmup |
| `app/core/` | 2 | config.py (Settings), ml_loader.py (MLRegistry) |
| `app/api/` | 4 | health, lpr, forecasting, anti_fraud, reports |
| `app/ml/lpr/` | 2 | detector (YOLO), recognizer (PaddleOCR) |
| `tests/test_smoke.py` | 1 | smoke /health + /lpr |
| `Dockerfile`, `pyproject.toml` | 2 | Build artifact |

### 1.5 `web-admin/` (Next.js 14)

| Carpeta | Archivos | Resumen |
|---------|----------|---------|
| `src/app/` | 2 páginas | `/` portada, `/dashboard` (4 KPIs + gráfico Recharts) |
| `src/components/` | 3 | KPICard, RevenueChart, Providers |
| `src/hooks/useDashboardData.ts` | 1 | TanStack Query → backend |
| `src/lib/api-client.ts` | 1 | fetch wrapper con bearer |
| Config | 4 | next.config.mjs, tailwind, tsconfig, postcss |

---

## 2. Mapa de uso — qué referencia qué

### 2.1 ComposeApp → Shared (consumido)

- `domain.model.*`: PlateNumber, Money, VehicleType, SessionStatus, ParkingSession, Tariff, Zone, ChargeBreakdown.
- `domain.tariff.TariffCalculator` (en `AppState.previewCharge`).
- `data.local.*`, `data.sync.*`, `data.api.*`: **declarados pero no cableados** en la UI actual (AppState usa MockData en memoria).

### 2.2 Backend → Shared (consumido)

- DTOs: SessionDto, ReceiptDto, TariffDto, ZoneDto, LoginRequest, LoginResponse, UserDto, RevenueReportDto, OccupancyReportDto, SyncPushRequest, SyncPullResponse.
- Enums: SessionStatus, VehicleType.

### 2.3 Backend → web-admin (consumido)

- `GET /api/v1/parkings/{id}/reports/revenue` (con fallback a datos sintéticos).
- `GET /api/v1/parkings/{id}/reports/occupancy`.

### 2.4 ai-service ↔ todo lo demás

- Backend NO invoca al ai-service (acoplamiento no implementado).
- Web-admin y composeApp NO invocan al ai-service.

---

## 3. Código no utilizado / huérfano

### 3.1 En `shared/`

| Símbolo | Definido en | Razón | Acción sugerida |
|---------|-------------|-------|----------------|
| `Vehicle` (data class) | `domain/model/Models.kt` | No referenciado en composeApp ni backend | Mantener (estructura para futura gestión de vehículos del cliente alquilado) |
| `Customer` (data class) | idem | Referenciado por `CustomerRepository` y SQLDelight `customers.sq`, pero ninguna UI lo usa todavía | Mantener (consumirá el módulo Admin > Clientes) |
| `Receipt` (data class dominio) | idem | API usa `ReceiptDto`; el modelo dominio se usaría en `LocalRepository` cuando UI cablee outbox | Mantener |
| DTOs LPR (`LprResultDto`, `BoundingBoxDto`, `LprAlternative`) | `data/api/dto/LprDtos.kt` | No consumido por composeApp ni backend | Mantener (espera integración con ai-service) |

### 3.2 En `composeApp/`

| Símbolo | Definido en | Estado | Acción |
|---------|-------------|--------|--------|
| `LoginScreen` | (carpeta `login/` vacía) | No existe pantalla mapeada en `Screen.kt`. El proyecto no tiene flujo de login (rol se cambia desde header). | Eliminar carpeta vacía si existe; o crear LoginScreen real cuando se cablee auth |
| `SettingsScreen` | `ui/screens/settings/SettingsScreen.kt` | Definida pero NO mapeada en `Screen.kt` ni en `TabNavBar` | Decidir: mapear como sub-pantalla del Admin o eliminar |
| `PlaceholderScreen` | `ui/screens/placeholder/PlaceholderScreen.kt` | Genérica "en construcción", no referenciada | Mantener (uso interno futuro para tabs no listos) o eliminar |
| `ZonesScreen` | `ui/screens/zones/ZonesScreen.kt` | NO referenciada en `Screen.kt`; reemplazada por `ParkingAvailabilityScreen` | **Eliminar** o reemplazar la pantalla actual |

**Recomendación:** crear ticket "limpieza UI" para borrar `zones/` y `placeholder/` si efectivamente no se usan; mapear `settings/` desde el módulo Admin.

### 3.3 En `backend/`

| Símbolo | Ubicación | Estado |
|---------|-----------|--------|
| `data class AuthUser` | `AuthRepository.kt` | Solo uso interno (paquete-privado de hecho) — OK |
| `data class ZoneOccupancySnapshot` | `SessionRepository.kt` | Solo uso interno — OK |
| Tabla `audit_log` (DB) | Flyway V1 | **DEFINIDA EN DB pero NUNCA escrita ni leída por código** |
| Tablas `customers`, `monthly_payments`, `vehicles` | Flyway V1 | **DEFINIDAS pero sin repositorio ni endpoint** |
| Tablas `permissions`, `role_permissions` | Flyway V1 | **DEFINIDAS pero sin uso** |

**Implicación:** existe esquema de DB para auditoría, clientes mensuales y permisos granulares, pero el código no los toca. Esto es **deuda de implementación**, no código muerto — la estructura está lista para construir el módulo Admin.

### 3.4 En `web-admin/`

- Sin código muerto detectado. Es un scaffold pequeño (~15–20 % implementado).
- `setToken` en `lib/api-client.ts` nunca se llama (sin flujo de login). Pendiente de integrar.

### 3.5 En `ai-service/`

- Endpoints `/api/v1/forecast/occupancy`, `/api/v1/anti-fraud/scan`, `/api/v1/reports/insights` están construidos **pero nadie los consume**. Esperan integración.

---

## 4. Deudas técnicas detectadas

### 4.1 Reglas del proyecto — estado actualizado (2026-05-22)

| Regla | Estado original (2026-05-21) | Estado actual (2026-05-22) |
|-------|----|----|
| **3** (archivos < 300 líneas) | ❌ CashierScreen 405, DashboardScreen 462, TariffManagementScreen 405 | ✅ los 3 partidos en archivos < 300 (Fase A.3) |
| **6** (audit hash chain) | ❌ tabla definida pero sin inserts | ✅ `AuditRepository.append()` con SHA-256 monotónico ms-truncado; integrado en login/refresh/session-opened/session-closed + 19 mutaciones admin (tariff/plan/special/holiday/zone) |
| **9** (offline-first cliente) | ❌ MockData en memoria | ✅ AppState consume Flow de SQLDelight via Koin; Seeder siembra DB en primer arranque; SyncManager.pull persiste tariff_plans + special_tariffs + holidays localmente |
| **13** (modo degradado IA) | ❌ ai-service sin integrar | ⚠ sigue pendiente (no requerido por Fases A-C) |

### 4.2 Otros gaps de calidad — estado actualizado

- **Tests backend** ✅ ahora 20 (4 AuditRepository + 8 AdminTariff + 8 AdminZone). Antes era 1.
- **Tests composeApp / web-admin** ❌ siguen en cero.
- **OpenAPI** ✅ extendido con 19 endpoints admin nuevos + schemas DTOs (Consolidación 1).
- **Logging estructurado** ❌ pendiente.
- **Métricas/observabilidad** ❌ pendiente.
- **CI/CD** ⚠ no verificado.
- **Periféricos** ⚠ implementados, sigue sin integración a UI (no requerido por Fases A-C).
- **AppState in-memory** ✅ ya consume Flow del LocalRepository real (Fase A.1).

### 4.3 Deudas vivas tras consolidación

- UI de `Customers` (clientes con mensualidades) — pendiente Fase E.
- UI de `Users` (operadores) — pendiente Fase D.
- Reportes con filtros + export — pendiente Fase F.
- Configuración del parqueadero (datos fiscales) — pendiente Fase G.
- Consulta de audit_log en UI — pendiente Fase G.
- Periféricos UI — pendiente Fase H.
- Modelo de dominio "limpio" para `TariffPlan` / `SpecialTariff` / `Holiday` (hoy persisten como DTOs en `LocalRepository` — viola tácticamente Regla 2). Refactor opcional.

---

## 5. Resumen ejecutivo de salud por módulo

| Módulo | Completitud | Salud | Riesgo |
|--------|------------|-------|--------|
| `shared/` | 85 % | 🟢 Bien estructurado, motor de tarifas robusto, sync funcional, contratos cross-platform claros | Bajo |
| `composeApp/` UI | 70 % | 🟡 Todas las pantallas existen, pero la mayoría no persiste (MockData). 3 archivos > 300 líneas. | Medio (lograr la integración con repo real es trabajo significativo) |
| `composeApp/` periféricos | 90 % implementado, 0 % integrado | 🟡 Clases listas, falta cableado a UI | Medio |
| `backend/` core (auth, sesiones, sync) | 75 % | 🟢 Compila (tras fix Exposed), endpoints clave existen, seguridad sólida | Bajo |
| `backend/` admin/CRUD | 0 % | 🔴 Sin CRUD tarifas/zonas/usuarios/clientes/auditoría/config | Alto |
| `ai-service/` | 50 % | 🟡 Endpoints listos, modelos funcionan en stub, no integrado | Bajo (es opcional para v1) |
| `web-admin/` | 15–20 % | 🟡 Scaffold limpio, solo 1 página de dashboard | Bajo (solo lectura, no bloquea v1) |
| `iosApp/` | ? | ⚪ No auditado en profundidad (depende de Xcode/macOS) | — |
| `infrastructure/` | ? | ⚪ No auditado en profundidad | — |
| `docs/` | 60 % | 🟢 ADRs + arquitectura básica; ahora completado con PRD/UC/HU/audit/admin spec | Bajo |

---

## 6. Sugerencias de cleanup inmediatas

1. **Partir** `CashierScreen.kt`, `DashboardScreen.kt`, `TariffManagementScreen.kt` en archivos < 300 líneas (sub-secciones / sub-composables).
2. **Decidir** sobre carpetas no usadas: `composeApp/.../ui/screens/zones/`, `placeholder/`, `login/`, `settings/`. Eliminar o mapear.
3. **Cablear** AppState a `LocalRepository` (Regla 9) — cambio quirúrgico: en `App.kt` inyectar repo via Koin, AppState lo consume con Flow.
4. **Activar** el `audit_log`: crear `AuditRepository` en backend, llamar desde rutas críticas, sellar con hash chain.
5. **Validar** `openapi.yaml`: re-generar desde el código o anotar diferencias.
6. **Eliminar** del repo el proyecto raíz `parqueaderos` (template stock) si no tiene propósito, o documentar su uso como sandbox.

---

## 7. Métricas brutas

| Métrica | Valor |
|---------|-------|
| Líneas Kotlin productivas (shared + composeApp + backend) | ~12.000 LOC (estimación) |
| Archivos Kotlin productivos | ~80 |
| Tablas PostgreSQL | 15 + 2 particionadas |
| Endpoints HTTP backend | 11 documentados |
| Pantallas Compose registradas | 12 + 3 internas |
| Tests automatizados | 1 suite (TariffCalculator, 6 escenarios) + smoke ai-service |
| Plataformas cubiertas | Android, iOS, JVM, Wasm |
| Tamaño módulo `ai-service` | ~10 archivos Python |
| Tamaño módulo `web-admin` | ~8 archivos TS/TSX |
