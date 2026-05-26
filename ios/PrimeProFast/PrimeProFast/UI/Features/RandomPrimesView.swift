import SwiftUI

struct RandomPrimesView: View {
    @State private var quantidade = "5"
    @State private var bits = "1024"
    @EnvironmentObject private var appState: AppState

    var body: some View {
        FeatureScaffold(
            title: "Primos Aleatórios",
            subtitle: "Acima de \(appState.limiarBitsNativo) bits usa job nativo assíncrono."
        ) { result, loading in
            TextField("Quantidade (1–50)", text: $quantidade).keyboardType(.numberPad).textFieldStyle(.roundedBorder)
            TextField("Bits por primo", text: $bits).keyboardType(.numberPad).textFieldStyle(.roundedBorder)

            Button("Gerar primos aleatórios") {
                Task { await gerar(result: result, loading: loading) }
            }
            .buttonStyle(.borderedProminent)
            .tint(PPFTheme.accent)

            Button("Cancelar") {
                appState.cancelRequested = true
                PPFNative.cancelarJob()
            }
            .buttonStyle(.bordered)
        }
    }

    private func gerar(result: Binding<String>, loading: Binding<Bool>) async {
        guard let q = Int(quantidade), (1...50).contains(q),
              let b = Int(bits), b > 0, b <= 33000 else {
            result.wrappedValue = "Quantidade 1–50 e bits 1–33000."
            return
        }
        appState.cancelRequested = false
        loading.wrappedValue = true

        let output = await PPFNative.run {
            if appState.shouldUseNativeRoute(bits: b) {
                let file = FileManager.default.temporaryDirectory
                    .appendingPathComponent("primos_job_\(UUID().uuidString).txt")
                return PPFNative.executarJob(
                    bits: b,
                    quantidade: q,
                    threads: ProcessInfo.processInfo.processorCount,
                    caminho: file,
                    onStatus: { status in
                        Task { @MainActor in
                            result.wrappedValue = status
                        }
                    },
                    shouldCancel: { appState.cancelRequested }
                )
            }
            return PPFNative.gerarPrimosGrandes(
                bits: Int32(b),
                quantidade: Int32(q),
                arquivo: nil,
                salvar: false
            )
        }
        result.wrappedValue = output
        loading.wrappedValue = false
    }
}
