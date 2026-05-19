import SwiftUI
import ComposeApp   // framework Kotlin/Native producido por :composeApp

/// Bridge: aloja el UIViewController de Compose dentro de SwiftUI.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // MainViewController() viene del módulo Kotlin/Native (paquete com.parking.app).
        MainViewControllerKt.MainViewController()
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.keyboard) // teclado iOS empuja la vista sin recalcular layout
    }
}
