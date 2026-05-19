# parking-system

Sistema de gestión de parqueaderos **cloud-native multiplataforma** que
moderniza un legacy desktop (VB6 / Delphi / .NET WinForms).

## 🎯 Plataformas

| Plataforma | Módulo | Stack |
|---|---|---|
| 📱 Android | `composeApp/androidMain` | Compose Multiplatform + Jetpack |
| 🍎 iOS | `composeApp/iosMain` + `iosApp/` | Compose UI + SwiftUI host |
| 🖥 Desktop | `composeApp/desktopMain` | Compose for Desktop (JVM) |
| 🌐 Web PWA | `composeApp/wasmJsMain` | Compose for Web (Wasm) |
| 🌐 Portal admin | `web-admin/` | Next.js 14 + Tailwind + shadcn |
| ⚙️ Backend | `backend/` | Ktor 3 + PostgreSQL 16 + TimescaleDB |
| 🤖 IA | `ai-service/` | FastAPI + YOLO + PaddleOCR |

## 💾 Doble nivel de base de datos

- **Local (cliente):** SQLite vía SQLDelight 2 (Android, iOS, Desktop, Web).
- **Remoto (servidor):** PostgreSQL 16 + TimescaleDB, fuente de verdad.

La caja de escritorio funciona **100 % offline** y sincroniza vía
outbox pattern (`receipts_queue`) cuando vuelve la conexión.

## 🚀 Quickstart

### Requisitos

- JDK 17 (Temurin recomendado)
- Android SDK 34 + NDK
- Node.js 20+
- Python 3.11+
- Docker Desktop
- macOS + Xcode 15 (solo para iOS)

### Abrir el monorepo

1. Clonar el repo y `cd parking-system`
2. Abrir IntelliJ IDEA 2024.1+ → **Open** → seleccionar la carpeta raíz
3. Esperar a que Gradle haga el sync (5–10 minutos la primera vez)
4. **File → Reload All from Disk** si añades archivos por fuera del IDE

### Comandos por plataforma

| Plataforma | Comando |
|---|---|
| Backend | `./gradlew :backend:run` |
| Caja escritorio | `./gradlew :composeApp:run` |
| Android | `./gradlew :composeApp:installDebug` |
| iOS | `open iosApp/iosApp.xcodeproj` y Run |
| Web PWA (Wasm) | `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` |
| Portal admin | `cd web-admin && npm install && npm run dev` |
| AI service | `cd ai-service && uvicorn app.main:app --reload` |

### Levantar todo el stack

```bash
docker compose -f infrastructure/docker/docker-compose.yml up -d
```

### Tests del motor de tarifas (corren en TODAS las plataformas)

```bash
./gradlew :shared:allTests
```

## 📂 Estructura

```
parking-system/
├── shared/         # KMP: dominio, motor de tarifas, repos, DTOs, sync
├── composeApp/     # KMP: UI Compose (Android + iOS + Desktop + Web)
├── backend/        # Ktor server
├── iosApp/         # Proyecto Xcode host
├── ai-service/     # FastAPI + YOLO
├── web-admin/      # Next.js 14
├── infrastructure/ # Docker + K8s + Terraform
└── docs/           # OpenAPI + ADRs
```

## 🔐 Seguridad

- bcrypt cost 12, JWT HS256 (1 h), refresh tokens (30 d), 2FA TOTP.
- Rate limit 60 req/min global, 5/min en `/auth`.
- Headers HSTS, CSP, X-Frame-Options DENY, TLS 1.3 en prod.

## 📜 ADRs

Decisiones de arquitectura en `docs/decisions/`:

- ADR-001 — Kotlin Multiplatform como base
- ADR-002 — Python aparte para el subsistema de IA
- ADR-003 — Offline-first en todos los clientes
- ADR-004 — Estrategia de sincronización con outbox pattern
