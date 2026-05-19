# ADR-001 — Adopción de Kotlin Multiplatform

## Estado
Aceptado — 2026-01-15.

## Contexto

El sistema reemplaza un legacy Windows-only (VB6/Delphi).  Se necesita:

- Caja de cobro de escritorio (Windows / Linux / macOS).
- App móvil para operadores en campo (Android + iOS).
- App cliente (reservas + pago) en móvil y web.
- Una **única** lógica de cobro: errores divergentes entre cajas serían
  contables y reportables a la DIAN.

## Opciones consideradas

1. **Apps nativas** (Swift + Kotlin + .NET + JS):
   - Pro: rendimiento nativo, ergonomía.
   - Contra: 4 implementaciones del motor de tarifas → divergencia inevitable.

2. **Flutter**:
   - Pro: una sola base.  Contra: el motor de tarifas debería vivir en Dart,
     pero queremos Kotlin en backend para reuso *exacto*; Dart no compila
     a JVM ni se ejecuta en Ktor.

3. **Kotlin Multiplatform** ← elegido:
   - Pro: el motor de tarifas vive en `shared` y se ejecuta **idéntico** en
     backend, caja, móvil, iOS y web.  Compose Multiplatform unifica UI.
   - Contra: madurez todavía dispar en iOS/Wasm; el ecosistema móvil
     espera Swift en iOS.  Mitigamos con SwiftUI host mínimo (`iosApp/`).

## Decisión

Adoptamos KMP 2.0.21 + Compose Multiplatform 1.7.0 para todo cliente y para
compartir lógica entre cliente y backend.

## Consecuencias

- **Positivas**: garantía de equivalencia funcional; tests del motor
  corren en TODAS las plataformas (commonTest).
- **Negativas**: build más complejo (Gradle KMP); requiere disciplina
  con `expect/actual` para no filtrar APIs específicas al dominio.
