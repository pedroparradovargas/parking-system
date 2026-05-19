# iosApp — proyecto Xcode host

Este directorio contiene el shell SwiftUI que aloja el framework Kotlin/Native
producido por `:composeApp` (target `iosArm64` / `iosSimulatorArm64` / `iosX64`).

## Estructura

- `iosApp/iOSApp.swift` — entry point `@main`.
- `iosApp/ContentView.swift` — `UIViewControllerRepresentable` que llama
  `MainViewControllerKt.MainViewController()` del framework Kotlin.
- `iosApp/Info.plist` — permisos (cámara, bluetooth), bundle ID, orientaciones.
- `Configuration/Config.xcconfig` — TEAM_ID, BUNDLE_ID, deployment target.

## Cómo abrir el proyecto

Esta carpeta NO incluye el `.xcodeproj` empaquetado (es muy ruidoso en Git).
Para crear el proyecto Xcode:

1. Abre Xcode → **File → New → Project → App** (iOS).
2. Product Name: `iosApp`. Interface: SwiftUI. Language: Swift.
3. Guarda en esta carpeta sobrescribiendo `iosApp/`.
4. Reemplaza el `iOSApp.swift` y `ContentView.swift` generados con los de aquí.
5. En **Build Phases → Link Binary With Libraries**, añade el framework
   `Shared.framework` generado por Gradle (`./gradlew :shared:assembleSharedXCFramework`).
6. En **Build Settings → Framework Search Paths**, añade
   `$(SRCROOT)/../shared/build/XCFrameworks/release`.

## Build Gradle del framework

```bash
./gradlew :shared:assembleSharedXCFramework
./gradlew :composeApp:assembleComposeAppXCFramework
```

Esto produce un XCFramework universal en `build/XCFrameworks/`.
