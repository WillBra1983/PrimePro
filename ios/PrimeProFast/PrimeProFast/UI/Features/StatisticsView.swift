import SwiftUI

struct StatisticsView: View {
    var body: some View {
        FeatureScaffold(
            title: "Estatísticas",
            subtitle: "Informações do dispositivo e do motor nativo."
        ) { result, _ in
            Button("Atualizar") {
                result.wrappedValue = """
                📱 Dispositivo: \(UIDevice.current.model)
                🔢 Processadores: \(ProcessInfo.processInfo.processorCount)
                🧠 Engine: C++/GMP (Algoritmo Especializado)
                📦 Bundle: \(Bundle.main.bundleIdentifier ?? "—")
                ⚡ Limiar rota nativa: 8192 bits
                """
            }
            .buttonStyle(.borderedProminent)
            .onAppear {
                result.wrappedValue = "Toque em Atualizar para ver estatísticas."
            }
        }
    }
}
