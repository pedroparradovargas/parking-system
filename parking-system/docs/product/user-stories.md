# Historias de Usuario — parking-system

**Formato:** `Como <actor>, quiero <acción>, para <beneficio>`.
**Convención:** HU-AAA-nnn (AAA = épica). Criterios de aceptación (CA) numerados.
**Prioridad:** P0 (bloqueante v1), P1 (importante v1), P2 (nice-to-have v1 o v2).
**Estado actual:** ✅ hecho, 🟡 parcial, ❌ no hecho.

---

## Épica 1 — Caja (operación diaria)

### HU-CAJ-001 · P0 · 🟡

> **Como** operador de caja, **quiero** registrar la entrada de un vehículo escaneando su placa, **para** liberar el carril de entrada en menos de 10 segundos.

**CA:**
1. La placa se puede ingresar por LPR, lector HID o teclado.
2. Si la placa no es válida (regex 5–10 caracteres alfanuméricos), se muestra error y no se crea sesión.
3. Si la placa pertenece a un cliente con plan vigente, el sistema lo identifica y muestra el badge "Mensualidad activa".
4. La sesión se persiste localmente aun sin internet.
5. Se imprime un tiquete con placa, hora y zona.

---

### HU-CAJ-002 · P0 · 🟡

> **Como** operador de caja, **quiero** ver inmediatamente el cobro calculado para una placa con sesión activa, **para** comunicárselo al cliente sin demoras.

**CA:**
1. La búsqueda por placa devuelve la sesión en < 200 ms (DB local).
2. El desglose muestra: minutos transcurridos, horas facturadas, base, recargo nocturno (si aplica), subtotal, IVA, total.
3. Si está dentro de gracia o tiene mensualidad activa, el total muestra "$0".
4. El cálculo usa exclusivamente `TariffCalculator` del módulo `shared`.

---

### HU-CAJ-003 · P0 · 🟡

> **Como** operador de caja, **quiero** cerrar la sesión y cobrar, **para** entregar el vehículo y liberar el cupo.

**CA:**
1. Al confirmar el cobro, la sesión cambia a `CLOSED` y se decrementa la ocupación de la zona.
2. Se crea un recibo con `localId` único.
3. Se imprime el recibo físico.
4. Si el medio de pago es efectivo, se abre el cajón monedero automáticamente.
5. El recibo queda en outbox (`PENDING`) si no hay internet, y se sincroniza al reconectar.

---

### HU-CAJ-004 · P0 · ✅ (motor) · ❌ (integración UI)

> **Como** operador de caja, **quiero** seguir operando si se cae el internet, **para** no perder ingresos durante una interrupción.

**CA:**
1. Todas las operaciones de caja (entrada, cobro, impresión) funcionan con DB local SQLite.
2. Un banner persistente "Sin conexión" se muestra cuando `ConnectivityMonitor.isOnline = false`.
3. Al reconectar, `SyncManager.runOnce()` se dispara automáticamente.
4. Los recibos pendientes se empujan en lote idempotente.
5. Los conflictos (sesión cerrada en otra caja) se marcan como `CONFLICT` para revisión manual.

---

### HU-CAJ-005 · P0 · ❌

> **Como** operador de caja al final del turno, **quiero** cerrar mi caja con un resumen impreso, **para** entregar al siguiente turno sin discrepancias.

**CA:**
1. El cierre lista todas las sesiones cerradas en mi turno.
2. Muestra totales por método de pago (efectivo, tarjeta, otros).
3. Se imprime un resumen con timestamp y hash de auditoría.
4. Después del cierre, no se pueden modificar las sesiones del turno.

---

### HU-CAJ-006 · P1 · ❌

> **Como** operador de caja, **quiero** corregir errores de placa antes de confirmar la entrada, **para** evitar cobros incorrectos en la salida.

**CA:**
1. Tras LPR, el sistema muestra la placa detectada con confianza %.
2. Si la confianza < 90 %, se exige confirmación manual.
3. El operador puede editar la placa antes de confirmar.
4. Cualquier corrección queda en audit log con OCR original vs. corregida.

---

## Épica 2 — Admin: gestión de tarifas

### HU-TAR-001 · P0 · ❌

> **Como** administrador, **quiero** crear una nueva tarifa para un tipo de vehículo, **para** ajustar precios según la temporada o ubicación.

**CA:**
1. Formulario solicita: tipo de vehículo, primera hora, subsiguientes, gracia, recargo nocturno (% + franja), IVA, `validFrom`, `validTo` (opcional).
2. Sistema valida: precios > 0; gracia ∈ [0, 60]; recargo ∈ [0, 100]; franja nocturna válida (hh:mm-hh:mm); `validFrom` ≥ ahora.
3. Si existe tarifa vigente del mismo tipo, se cierra (`validTo = nuevaTarifa.validFrom - 1ms`).
4. Audit log: `tariff.created` con payload.
5. Las cajas reciben la nueva tarifa en su siguiente sync (`/sync/pull`).

---

### HU-TAR-002 · P0 · ❌

> **Como** administrador, **quiero** ver el histórico de tarifas para auditar cambios pasados, **para** explicar cobros antiguos o cumplir requisitos fiscales.

**CA:**
1. Listado paginado de tarifas con `validFrom` y `validTo`.
2. Filtros: tipo de vehículo, vigentes/históricas/futuras.
3. Click en una tarifa muestra payload completo y la entrada de audit log que la creó.

---

### HU-TAR-003 · P0 · ❌

> **Como** administrador, **quiero** configurar planes de mensualidad (mensual, trimestral, semestral, anual), **para** ofrecer descuentos a clientes recurrentes.

**CA:**
1. Formulario: nombre del plan, duración en días, precio.
2. Sistema calcula y muestra el descuento implícito vs. cobrar por hora 1 mes.
3. Se pueden activar/desactivar planes sin borrarlos.

---

### HU-TAR-004 · P1 · ❌

> **Como** administrador, **quiero** definir tarifas especiales (fin de semana, festivos), **para** cobrar diferente cuando hay alta demanda.

**CA:**
1. Calendario de festivos editable (importar desde lista por defecto Colombia).
2. Multiplicador o tarifa alterna por día.
3. Preview: "Un auto que entra a 18:00 del 2026-12-25 paga X (festivo + nocturno)".

---

### HU-TAR-005 · P0 · ❌ (UI 🟡 read-only)

> **Como** administrador, **quiero** ver el listado de tarifas vigentes claramente diferenciadas por tipo de vehículo, **para** comunicárselo a los operadores.

**CA:**
1. Tabla con tipo, primera hora, subsiguientes, gracia, IVA, recargo nocturno, vigencia.
2. Botón "Editar" abre flujo de versionado (HU-TAR-001).
3. Indicador visual de tarifa "Próxima a entrar en vigencia".

---

## Épica 3 — Admin: gestión de zonas

### HU-ZON-001 · P0 · ❌

> **Como** administrador, **quiero** crear/editar/desactivar zonas, **para** reflejar la realidad física del parqueadero.

**CA:**
1. Crear: código (único por parking), capacidad, tipos de vehículo permitidos.
2. Editar: cambiar capacidad o tipos permitidos.
3. Desactivar: marca `enabled = false`; no permite nuevos ingresos pero respeta los activos.
4. Audit log.

---

### HU-ZON-002 · P0 · 🟡 (visualización ya existe)

> **Como** administrador, **quiero** ver la ocupación de cada zona en tiempo real, **para** dirigir clientes a las zonas con cupo.

**CA:**
1. Grilla de zonas con badge "X / Y ocupado" y barra de progreso.
2. Actualización por WebSocket `/ws/occupancy`.
3. Color del badge: verde < 70 %, naranja 70–95 %, rojo ≥ 95 %.

---

### HU-ZON-003 · P1 · ❌

> **Como** administrador, **quiero** marcar cupos en mantenimiento, **para** que el sistema no los considere disponibles.

**CA:**
1. Botón "+ Mantenimiento" en zona; pide cantidad y motivo.
2. Capacidad efectiva = capacidad - mantenimiento.
3. Audit log.

---

## Épica 4 — Admin: gestión de usuarios y roles

### HU-USR-001 · P0 · ❌

> **Como** administrador, **quiero** crear usuarios operadores con roles, **para** controlar el acceso a la caja.

**CA:**
1. Formulario: username (único en parking), email, nombre, password inicial, roles (multi-select).
2. Password se hashea con bcrypt cost 12 antes de persistir.
3. Roles disponibles: SUPERADMIN, ADMIN, CASHIER, VIEWER.
4. Audit log con `actor_user_id` del admin.

---

### HU-USR-002 · P0 · ❌

> **Como** administrador, **quiero** revocar acceso a un usuario sin borrarlo, **para** mantener su historial de operaciones.

**CA:**
1. Botón "Deshabilitar" cambia `enabled = false`.
2. El usuario no puede loguear pero sus sesiones cerradas siguen siendo válidas.
3. Audit log.

---

### HU-USR-003 · P1 · ❌ (backend soporta TOTP, falta UI admin)

> **Como** administrador, **quiero** exigir 2FA a usuarios con privilegios altos, **para** reforzar la seguridad.

**CA:**
1. Toggle "Requiere 2FA" en perfil de usuario.
2. Sistema genera QR con `totpSecret`.
3. Usuario confirma con primer código.
4. Login posterior pide TOTP además de password.

---

### HU-USR-004 · P1 · ❌

> **Como** administrador, **quiero** resetear el password de un operador olvidado, **para** restablecer el acceso sin contactar IT.

**CA:**
1. Botón "Reset password" en perfil.
2. Sistema genera password temporal de un solo uso.
3. Operador debe cambiarlo en el primer login.
4. Audit log.

---

## Épica 5 — Admin: gestión de clientes alquilados

### HU-CLI-001 · P0 · ❌ (UI registro mock 🟡)

> **Como** administrador, **quiero** registrar clientes con sus vehículos y plan, **para** habilitar el cobro automático.

**CA:**
1. Formulario: documento, nombre, email, teléfono, placas asociadas (multi).
2. Validación: documento único por parking.
3. Audit log.

---

### HU-CLI-002 · P0 · ❌

> **Como** administrador, **quiero** asignar y renovar planes de mensualidad, **para** que los clientes accedan sin pagar por uso.

**CA:**
1. Elección de plan (los configurados en HU-TAR-003).
2. Registro de pago: monto, método, fecha.
3. Sistema calcula `validFrom` y `validTo`.
4. Si el cliente tiene plan vigente al momento de renovar, la nueva vigencia comienza al expirar la actual.
5. Audit log: `monthly.created` o `monthly.renewed`.

---

### HU-CLI-003 · P0 · ❌

> **Como** administrador, **quiero** ver clientes con plan próximo a vencer, **para** contactarlos y ofrecer renovación.

**CA:**
1. Listado con filtro "Vence en próximos 7 días".
2. Por cada cliente: contacto, plan actual, fecha vencimiento.
3. Acción rápida "Renovar".

---

### HU-CLI-004 · P0 · 🟡 (TariffCalculator soporta `appliedMonthly`)

> **Como** cliente alquilado, **quiero** entrar y salir sin que me cobren, **para** no perder tiempo en la caja.

**CA:**
1. Al entrar, el sistema identifica al cliente por placa.
2. La sesión queda marcada con `customerId`.
3. Al salir, `TariffCalculator` aplica mensualidad: total = 0, registra horas.
4. Recibo indica "Mensualidad - Cliente: NOMBRE - $0".

---

## Épica 6 — Admin: reportes ejecutivos

### HU-REP-001 · P0 · 🟡 (endpoint backend OK, UI sin filtros)

> **Como** administrador, **quiero** ver mis ingresos por rango de fechas, **para** entender mi negocio.

**CA:**
1. Selector de rango (presets + custom).
2. KPIs: total, IVA, conteo sesiones.
3. Gráfico de ingresos por día.
4. Desglose por tipo de vehículo.

---

### HU-REP-002 · P0 · ❌

> **Como** administrador, **quiero** exportar reportes a CSV/PDF, **para** compartir con contabilidad.

**CA:**
1. Botón "Exportar" con opciones CSV / PDF.
2. CSV incluye todas las columnas del listado.
3. PDF incluye encabezado del parqueadero (datos fiscales), totales y firma.

---

### HU-REP-003 · P1 · ❌

> **Como** administrador, **quiero** filtrar reportes por zona, operador y tipo de vehículo, **para** encontrar problemas operativos.

**CA:**
1. Filtros multi-select.
2. KPIs se recalculan al cambiar filtro.

---

### HU-REP-004 · P1 · ❌

> **Como** administrador, **quiero** ver la ocupación histórica del parqueadero, **para** decidir si amplío capacidad o ajusto tarifas en horarios pico.

**CA:**
1. Serie temporal por zona.
2. Promedio por día de la semana.
3. Top 5 horas de mayor ocupación.

---

## Épica 7 — Admin: configuración del parqueadero

### HU-CFG-001 · P0 · ❌

> **Como** administrador, **quiero** editar los datos fiscales de mi parqueadero, **para** cumplir con DIAN.

**CA:**
1. Formulario: NIT, razón social, dirección, ciudad, resolución DIAN, serie de facturación.
2. Validación de formato de NIT (10 dígitos + dígito verificador).
3. Audit log.

---

### HU-CFG-002 · P1 · ❌

> **Como** administrador, **quiero** configurar el horario de operación, **para** que el sistema rechace entradas fuera de horario.

**CA:**
1. Modo "24/7" o por franjas (lun-dom).
2. Si un vehículo intenta entrar fuera de horario, la caja avisa al operador (no bloquea — el operador decide si autorizar como excepción).

---

### HU-CFG-003 · P2 · ❌

> **Como** administrador, **quiero** configurar la zona horaria del parqueadero, **para** que los reportes respeten mi horario local.

**CA:**
1. Selector de timezone (default: America/Bogota).
2. Reportes y recibos usan el timezone configurado.

---

## Épica 8 — Auditoría e integridad

### HU-AUD-001 · P0 · ❌ (tabla existe, sin código)

> **Como** administrador, **quiero** consultar todas las acciones que se han hecho en mi parqueadero, **para** investigar incidentes o cumplir auditoría externa.

**CA:**
1. Listado paginado de entradas (`audit_log`).
2. Filtros: actor, entidad, acción, rango de fechas.
3. Click muestra payload JSON completo.

---

### HU-AUD-002 · P0 · ❌

> **Como** sistema, **debo** registrar en `audit_log` cada acción crítica con hash chain, **para** garantizar inmutabilidad.

**CA:**
1. Acciones cubiertas: login, refresh, cambio de tarifa, alta/baja de zona, alta/baja/cambio de usuario, alta/renovación de mensualidad, cierre de sesión.
2. `current_hash = SHA256(prev_hash || ts || action || entity || entityId || payload)`.
3. La primera entrada tiene `prev_hash = "0".repeat(64)`.
4. Tests verifican la integridad después de N inserciones.

---

### HU-AUD-003 · P1 · ❌

> **Como** administrador, **quiero** que el sistema me alerte si detecta manipulación del audit log, **para** reaccionar a incidentes de seguridad.

**CA:**
1. Job nocturno valida `current_hash` de cada entrada.
2. Si discrepa: alerta por email + Slack.
3. Endpoint `/admin/audit/verify` ejecuta validación bajo demanda.

---

## Épica 9 — Periféricos POS

### HU-PER-001 · P0 · 🟡 (clase `ThermalPrinter` existe; sin cableado UI)

> **Como** operador de caja, **quiero** que el recibo se imprima automáticamente al cobrar, **para** no perder tiempo.

**CA:**
1. Impresión es < 2 s después de confirmar cobro.
2. Si falla: aparece banner rojo y se imprime QR para que el cliente lo lleve digital.
3. Recibo respeta plantilla DIAN (NIT, razón social, secuencia, CUFE si está disponible, IVA discriminado).

---

### HU-PER-002 · P0 · 🟡 (clase `BarcodeScanner` existe; sin cableado UI)

> **Como** operador de caja, **quiero** que el lector HID escriba la placa directamente en el campo, **para** evitar errores de digitación.

**CA:**
1. Foco automático en el input al iniciar la pantalla.
2. Después del Enter del lector, se valida la placa.
3. Si la placa coincide con sesión activa, se va directo al flujo de cobro.

---

### HU-PER-003 · P1 · 🟡 (clase `CashDrawer` existe)

> **Como** operador de caja, **quiero** que el cajón se abra solo al cobrar en efectivo, **para** acelerar el proceso.

**CA:**
1. Apertura ocurre después de imprimir el recibo.
2. Si la impresora está caída, no se intenta abrir cajón (no llegó el comando).

---

### HU-PER-004 · P2 · ❌

> **Como** operador, **quiero** procesar pagos con tarjeta vía datafono, **para** ofrecer más medios de pago.

**CA:**
1. Interfaz abstracta `Datafono` (mock en v1, integración real en v2).
2. Resultado del datafono (aprobado/rechazado) se asocia al recibo.

---

## Épica 10 — IA / LPR

### HU-IA-001 · P1 · 🟡 (ai-service tiene endpoint /lpr, sin integración cliente)

> **Como** operador de caja, **quiero** que la cámara reconozca la placa automáticamente, **para** no digitarla.

**CA:**
1. La caja captura imagen, hace POST `/api/v1/lpr` al ai-service.
2. Si la respuesta llega en < 1 s con confianza > 90 %, se pre-llena el campo.
3. Si no llega o confianza baja, fallback a ingreso manual (RF-IA-002 modo degradado).

---

### HU-IA-002 · P2 · ❌ (ai-service tiene endpoint, sin UI)

> **Como** administrador, **quiero** ver una predicción de ocupación para mañana, **para** planear personal.

**CA:**
1. UI muestra forecast de las próximas 24 h (banda de confianza).
2. Endpoint `/api/v1/forecast/occupancy`.

---

### HU-IA-003 · P2 · ❌

> **Como** administrador, **quiero** que el sistema detecte sesiones sospechosas (muy largas, placas repetidas), **para** investigar fraude.

**CA:**
1. Job o widget que llama al endpoint `/api/v1/anti-fraud/scan`.
2. Resultados con score + razón explicable.

---

## Épica 11 — Supervisor multi-sucursal

### HU-SUP-001 · P1 · 🟡 (web-admin 15-20% funcional)

> **Como** supervisor con varios parqueaderos, **quiero** ver un panel consolidado, **para** monitorear todo mi negocio desde la web.

**CA:**
1. Login con credenciales (rol SUPERADMIN o ADMIN multi-parking).
2. Sistema agrega KPIs de todos los parkings autorizados.
3. Drill-down a un parqueadero específico.

---

### HU-SUP-002 · P1 · ❌

> **Como** supervisor, **quiero** recibir alertas cuando un parqueadero está cerca de su capacidad, **para** redirigir tráfico.

**CA:**
1. Configurar umbral (ej. 90 %).
2. Alertas por email/Slack.
3. Cooldown para evitar spam.

---

## Métricas de salud del backlog

| Épica | Total HU | P0 | Hechas o casi (✅/🟡) | No iniciadas (❌) |
|-------|---------|----|----------------------|-------------------|
| Caja | 6 | 5 | 4 | 2 |
| Tarifas | 5 | 4 | 0 | 5 |
| Zonas | 3 | 2 | 1 | 2 |
| Usuarios | 4 | 2 | 0 | 4 |
| Clientes | 4 | 4 | 1 | 3 |
| Reportes | 4 | 2 | 1 | 3 |
| Configuración | 3 | 1 | 0 | 3 |
| Auditoría | 3 | 2 | 0 | 3 |
| Periféricos | 4 | 2 | 3 | 1 |
| IA / LPR | 3 | 0 | 1 | 2 |
| Supervisor | 2 | 0 | 1 | 1 |
| **TOTAL** | **41** | **24** | **12** | **29** |

→ Foco recomendado para próximas sprints: **HU-TAR-001/003 + HU-USR-001/002 + HU-AUD-001/002 + HU-CLI-001/002** (cubren el grueso del módulo admin y la integridad de auditoría).
