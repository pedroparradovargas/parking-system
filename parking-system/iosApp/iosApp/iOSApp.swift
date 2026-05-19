import SwiftUI

// Punto de entrada de la app iOS.
// @main delega al sistema operativo para arrancar la WindowGroup principal.
@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea(edges: .bottom) // dar todo el alto a Compose
        }
    }
}
