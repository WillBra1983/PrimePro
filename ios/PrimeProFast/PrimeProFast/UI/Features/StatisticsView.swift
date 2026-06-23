import SwiftUI

struct StatisticsView: View {
    @EnvironmentObject private var appState: AppState

    var body: some View {
        FeatureScaffold(
            title: "Estatísticas",
            subtitle: "Informações do dispositivo e do motor nativo."
        ) { result, loading in
            Button("Atualizar") {
                Task { await atualizar(result: result, loading: loading) }
            }
            .buttonStyle(.borderedProminent)
            .onAppear {
                result.wrappedValue = "Toque em Atualizar para ver estatísticas."
            }
        }
    }

    private func atualizar(result: Binding<String>, loading: Binding<Bool>) async {
        loading.wrappedValue = true
        let texto = """
        📱 Dispositivo: \(UIDevice.current.model)
        🔢 Processadores: \(ProcessInfo.processInfo.processorCount)
        🧠 Engine: C++/GMP (Algoritmo Especializado)
        📦 Bundle: \(Bundle.main.bundleIdentifier ?? "—")
        ⚡ Limiar rota nativa: \(appState.limiarBitsNativo) bits
        """
        await MainActor.run {
            loading.wrappedValue = false
            appState.saveTemporaryResultAndOpenViewer(
                texto,
                prefix: "estatisticas_primos_completas",
                statusMessage: result
            )
        }
    }
}
