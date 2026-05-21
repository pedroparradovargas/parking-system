# PRD — parking-system

**Producto:** Sistema integral de gestión de parqueaderos multi-tenant, cloud-native, multiplataforma, offline-first.
**Versión:** 0.1 (auditoría de código existente — 2026-05-21).
**Owner:** equipo parking-system. Repositorio: `E:/aplicaciones moviles/parqueaderos/`.

---

## 1. Visión

Reemplazar el software legacy de parqueaderos (VB6 / Delphi / .NET WinForms) por una solución **moderna, modular y operable sin conexión**, que cubra desde la caja de cobro en el sitio hasta la supervisión ejecutiva en la nube y la inteligencia artificial para lectura de placas.

El producto se posiciona como **plataforma multi-tenant**: una sola instalación da servicio a múltiples parqueaderos, cada uno aislado por `parking_id`, con su propio catálogo de tarifas, zonas, usuarios y reportes.

---

## 2. Objetivos del producto

| # | Objetivo | Métrica de éxito |
|---|----------|------------------|
| O1 | Caja de cobro que no se cae cuando el internet falla | 100 % de operaciones (entrada, cobro, recibo) funcionan offline; sync automática al reconectar |
| O2 | Cálculo de tarifa idéntico en todas las plataformas | Test `TariffCalculatorTest` pasa bit-a-bit en Android, iOS, JVM y Wasm |
| O3 | Tiempo de cobro promedio < 30 s por vehículo | Métrica medida desde escaneo de placa hasta impresión de recibo |
| O4 | Cero divergencia entre caja y servidor | Conciliación diaria sin diferencias en totales o conteos |
| O5 | Visibilidad ejecutiva en tiempo real | Dashboard web con KPIs actualizados en < 5 s vía WebSocket |
| O6 | Reducir errores humanos de digitación de placa | LPR (visión por computadora) ≥ 95 % de aciertos en cámara fija |
| O7 | Cumplir normativa fiscal (DIAN — Colombia) | Recibo con CUFE, numeración secuencial, IVA discriminado |

---

## 3. Alcance

### 3.1 In-scope (v1)

- **Caja desktop (JVM)** para operador con periféricos (impresora ESC/POS, lector de barras HID, cajón monedero).
- **App móvil Android/iOS** para clientes y operadores móviles.
- **Web PWA (Wasm)** para clientes finales que prefieran navegador.
- **Backend Ktor + PostgreSQL** como fuente de verdad.
- **Portal web admin (Next.js)** para visibilidad ejecutiva remota (read-only en v1).
- **Servicio de IA (FastAPI + YOLO + PaddleOCR)** para LPR.
- **Módulo Administrador** completo dentro de la caja desktop y/o portal web (ver `admin-module.md`).
- **Sincronización offline-first** con outbox + idempotencia por `local_id`.
- **Auditoría inmutable** con hash chain SHA-256.

### 3.2 Out-of-scope (v1)

- Cobranzas/recaudo electrónico real (pasarela de pagos, datafono). Se modela la interfaz, no la integración real.
- Reservas anticipadas con pago online.
- Sistema de tarjetas de proximidad (RFID/NFC) para clientes alquilados.
- App de cliente final con pago integrado (placas favoritas, historial).
- Multi-país (sólo Colombia; CUFE DIAN, IVA 19 %, zona horaria America/Bogota).

---

## 4. Personas y actores

| Actor | Descripción | Plataforma principal | Frecuencia de uso |
|-------|-------------|----------------------|-------------------|
| **Operador de caja** | Quien atiende la entrada/salida de vehículos en cabina. Bajo conocimiento técnico, alta presión por velocidad. | Desktop kiosk | Continuo durante turno (8–12 h) |
| **Administrador del parqueadero** | Dueño o jefe del parqueadero. Configura tarifas, ve reportes, gestiona usuarios y clientes alquilados. | Desktop + Web admin | Diario, 1–2 h |
| **Cliente eventual** | Usuario ocasional que paga por uso. Llega, parquea, paga, se va. | Desktop (caja) → recibo físico | Ocasional |
| **Cliente alquilado (mensualidad)** | Cliente con plan mensual/anual. Entrada y salida sin cobro adicional mientras esté vigente. | Móvil + Desktop | Diario |
| **Supervisor ejecutivo (multi-sucursal)** | Gerente regional o dueño con varios parqueaderos. Ve KPIs consolidados. | Web admin | Semanal |
| **Sistema (procesos automáticos)** | SyncManager, ConnectivityMonitor, WebSocket broadcaster, IA. | N/A | Continuo |

---

## 5. KPIs del producto

| KPI | Definición | Meta v1 |
|-----|------------|---------|
| Disponibilidad caja | % del tiempo en que la caja puede operar (incluso offline) | ≥ 99.9 % |
| Tiempo medio de cobro | Desde escaneo de placa a recibo impreso | < 30 s |
| Precisión LPR | Tasa de aciertos del reconocedor de placas | ≥ 95 % |
| Latencia sync | Tiempo entre creación de recibo local y persistencia en servidor (red OK) | < 10 s p95 |
| Divergencia conciliación | Diferencia entre total de caja y total servidor al cierre | $0 |
| Cobertura tests críticos | TariffCalculator + flujo cobro + sync | ≥ 80 % |

---

## 6. Requisitos funcionales (RF)

Codificados como **RF-AAA-nnn**, donde `AAA` es el área. Mapeo a casos de uso en `use-cases.md`.

### 6.1 Caja / sesiones (RF-CAJ)

- **RF-CAJ-001** Registrar entrada de vehículo (placa + tipo + zona opcional + operador + hora).
- **RF-CAJ-002** Buscar sesión activa por placa.
- **RF-CAJ-003** Previsualizar cobro de una sesión sin cerrarla.
- **RF-CAJ-004** Cerrar sesión, calcular tarifa, generar recibo.
- **RF-CAJ-005** Imprimir recibo en impresora ESC/POS.
- **RF-CAJ-006** Abrir cajón monedero al cobrar en efectivo.
- **RF-CAJ-007** Operar 100 % sin conexión a internet; sincronizar al reconectar.
- **RF-CAJ-008** Aplicar mensualidad automáticamente si la placa pertenece a un cliente con plan vigente (cobro = 0, registra horas).

### 6.2 Administración — Tarifas (RF-TAR)

- **RF-TAR-001** Crear tarifa por tipo de vehículo con vigencia (validFrom / validTo).
- **RF-TAR-002** Editar tarifa existente (cambios crean una nueva vigencia; no se sobrescribe la histórica).
- **RF-TAR-003** Listar tarifas vigentes y futuras por parking.
- **RF-TAR-004** Configurar recargo nocturno (rango horario + porcentaje).
- **RF-TAR-005** Configurar minutos de gracia.
- **RF-TAR-006** Configurar IVA aplicable (porcentaje).
- **RF-TAR-007** Configurar planes de mensualidad (precio, periodicidad: mensual / trimestral / semestral / anual; descuento implícito).
- **RF-TAR-008** Configurar tarifas especiales (fin de semana, festivos, eventos) como multiplicadores o tarifas alternas.

### 6.3 Administración — Zonas (RF-ZON)

- **RF-ZON-001** Crear zona (código, capacidad, tipos de vehículo permitidos).
- **RF-ZON-002** Editar zona (capacidad, tipos permitidos, estado).
- **RF-ZON-003** Visualizar ocupación en tiempo real por zona.
- **RF-ZON-004** Marcar cupos en mantenimiento (no contabilizan capacidad).

### 6.4 Administración — Usuarios y roles (RF-USR)

- **RF-USR-001** Crear usuario operador / admin asociado a un parking.
- **RF-USR-002** Asignar y revocar roles (SUPERADMIN, ADMIN, CASHIER, VIEWER).
- **RF-USR-003** Activar 2FA (TOTP) por usuario.
- **RF-USR-004** Resetear contraseña (genera token de un solo uso).
- **RF-USR-005** Deshabilitar usuario sin eliminarlo (preserva auditoría).

### 6.5 Administración — Clientes alquilados (RF-CLI)

- **RF-CLI-001** Registrar cliente (documento, nombre, contacto, vehículos asociados).
- **RF-CLI-002** Asignar plan de mensualidad (vigencia, monto pagado).
- **RF-CLI-003** Renovar plan (nueva vigencia, registro contable).
- **RF-CLI-004** Suspender o cancelar plan.
- **RF-CLI-005** Listar clientes con plan próximo a vencer.

### 6.6 Reportes (RF-REP)

- **RF-REP-001** Reporte de ingresos por rango de fechas (total, IVA, por tipo vehículo, por día).
- **RF-REP-002** Reporte de ocupación por zona (instantáneo y serie temporal).
- **RF-REP-003** Reporte de cierre de caja (sesiones cerradas en el turno + totales en efectivo/tarjeta).
- **RF-REP-004** Reporte de mensualidades (vigentes, vencidas, ingresos por renovación).
- **RF-REP-005** Exportar reportes a CSV/PDF.
- **RF-REP-006** Filtros: por zona, por tipo de vehículo, por operador, por rango horario.

### 6.7 Configuración del parqueadero (RF-CFG)

- **RF-CFG-001** Editar datos fiscales (NIT, razón social, dirección, resolución DIAN).
- **RF-CFG-002** Editar horario de operación (24 h o franjas).
- **RF-CFG-003** Editar zona horaria (default America/Bogota).
- **RF-CFG-004** Configurar serie de facturación / resolución DIAN.
- **RF-CFG-005** Configurar conexión a servicios externos (URL backend, AI service).

### 6.8 Auditoría (RF-AUD)

- **RF-AUD-001** Toda escritura crítica (login, cierre de sesión, edición de tarifa, gestión usuario) genera entrada en `audit_log`.
- **RF-AUD-002** Cada entrada incluye `prev_hash` + `current_hash` (SHA-256) formando cadena inmutable.
- **RF-AUD-003** Consulta de auditoría filtrable por actor, entidad, rango temporal.
- **RF-AUD-004** Verificación de integridad de la cadena (job nocturno + endpoint).

### 6.9 Periféricos (RF-PER)

- **RF-PER-001** Impresora ESC/POS USB o LAN (impresión de recibos, cierre de caja, código QR).
- **RF-PER-002** Lector de código de barras / QR como teclado HID (lectura de placas, IDs de clientes).
- **RF-PER-003** Cajón monedero (apertura por comando ESC/POS).
- **RF-PER-004** Datafono (interfaz abstracta — integración real fuera de v1).

### 6.10 IA / LPR (RF-IA)

- **RF-IA-001** Detección y reconocimiento de placa desde imagen base64.
- **RF-IA-002** Modo degradado (stub) si el servicio IA no está disponible: ingreso manual de placa con timeout agresivo + circuit breaker.
- **RF-IA-003** Forecast de ocupación (siguiente 24 h, basado en serie temporal).
- **RF-IA-004** Detección de fraude (sesiones sospechosamente largas, placas duplicadas, anomalías de cobro).

---

## 7. Requisitos no funcionales (RNF)

| Código | Categoría | Requisito |
|--------|-----------|-----------|
| RNF-001 | Seguridad | bcrypt cost 12; JWT HS256 1 h + refresh 30 d; rate limit 60/min global y 5/min en `/auth`; headers HSTS + CSP + X-Frame-Options DENY; TLS 1.3 en producción. |
| RNF-002 | Privacidad | Tokens nunca en logs. Refresh tokens almacenados con hash SHA-256. Cumplimiento Ley 1581 (Habeas Data Colombia). |
| RNF-003 | Disponibilidad | Caja 99.9 % (offline-tolerant). Backend 99.5 % (single-region en v1). |
| RNF-004 | Performance | Cobro p95 < 1 s end-to-end (cálculo + impresión). Dashboard p95 < 500 ms (con caché 30 s en cliente). |
| RNF-005 | Escalabilidad | Backend escalable horizontalmente con Redis Pub/Sub para WebSockets (flag `realtime.redisEnabled`). |
| RNF-006 | Consistencia monetaria | Todos los montos en centavos (BIGINT). Jamás `Float`/`Double`. Cálculo de IVA con redondeo HALF_UP. |
| RNF-007 | Multi-tenancy | Aislamiento por `parking_id` en cada tabla de negocio. Validación de tenant desde JWT en backend. |
| RNF-008 | Internacionalización | Sólo es-CO en v1; arquitectura no bloquea i18n futura. |
| RNF-009 | Mantenibilidad | Clean architecture en `shared`. Archivos < 300 líneas. Identificadores en inglés, comentarios en español. |
| RNF-010 | Compatibilidad | Android API 26+ (8.0 Oreo). iOS 14+. JVM 17. Chrome/Edge últimas 2 versiones para Wasm. |
| RNF-011 | Testabilidad | Motor de tarifas con tests en `commonTest` corriendo en las 4 plataformas (cobro idéntico bit-a-bit). |
| RNF-012 | Observabilidad | Logs estructurados (logback JSON en backend). Métricas Prometheus en `/metrics` (fase 2). Trazas OpenTelemetry (fase 2). |

---

## 8. Restricciones técnicas y supuestos

- Kotlin 2.0.21 + Compose Multiplatform 1.7 (UI compartida en 4 plataformas).
- Ktor 3.0.1 (backend HTTP/WS).
- PostgreSQL 16 + TimescaleDB (sesiones particionadas por mes; ver `infrastructure/`).
- SQLDelight 2.1 con `web-worker-driver` para Wasm/PWA.
- Flyway para migraciones de DB (`V1__init.sql` ya construido).
- DIAN: numeración de recibo gestionada por el backend; CUFE generado al sincronizar (la caja offline genera secuencia local provisional).
- Zona horaria fija America/Bogota en v1 (configurable por parking en v2).

---

## 9. Dependencias externas

| Dependencia | Uso | Modo degradado |
|-------------|-----|----------------|
| Servicio IA (FastAPI) | LPR, forecast, anti-fraude | Ingreso manual de placa con timeout |
| Pasarela DIAN | CUFE de recibos | Recibo provisional sin CUFE, sincroniza al recuperar |
| Impresora ESC/POS | Recibos físicos | Recibo digital QR; alerta visual |
| Datafono | Pago con tarjeta | Pago en efectivo o transferencia (registro manual) |
| PostgreSQL | Fuente de verdad | Caja opera con DB local SQLite |

---

## 10. Riesgos clave

| Riesgo | Impacto | Mitigación |
|--------|---------|-----------|
| Divergencia de cálculo de tarifa entre plataformas | Cliente paga distinto en caja vs. app | Test exhaustivo en `commonTest`; motor único en `shared` |
| Pérdida de recibos por crash de caja antes de sync | Pérdida de ingresos | Outbox persistente en SQLite + journaling |
| Adopción baja de LPR por falsos positivos | Operador desconfía y digita manual | Mostrar confianza; permitir corregir antes de confirmar entrada |
| Tarifas mal configuradas por admin | Cobro incorrecto | Validaciones en backend; preview antes de guardar; vigencias inmutables (no se sobrescribe historia) |
| Token JWT comprometido | Acceso indebido | Refresh rotation + revocación; rate limit; bcrypt cost 12 |

---

## 11. Roadmap macro (sugerido)

- **Fase 1 (actual)** — Caja desktop operativa + UI Figma portada + backend con sesiones/sync básico.
- **Fase 2** — Módulo Administrador completo (ver `admin-module.md` para detalle).
- **Fase 3** — Web-admin como portal ejecutivo cloud (reportes consolidados, multi-sucursal).
- **Fase 4** — IA real (LPR en producción, forecast, anti-fraude).
- **Fase 5** — App cliente final (placas favoritas, pago digital, reserva).

---

## 12. Glosario

- **Sesión:** instancia de un vehículo dentro del parqueadero (entrada–salida).
- **Recibo:** comprobante fiscal de una sesión cobrada.
- **Outbox:** patrón de cola local para escrituras pendientes de sincronizar.
- **CUFE:** Código Único de Factura Electrónica (DIAN).
- **Hash chain:** sucesión de hashes encadenados para auditoría inmutable.
- **Caja:** estación de trabajo del operador (típicamente JVM desktop con periféricos).
- **Mensualidad / cliente alquilado:** plan de tarifa fija periódica que sustituye el cobro por uso.
