import SwiftUI

struct SecurityView: View {
    @State private var bits = "2048"
    @EnvironmentObject private var appState: AppState

    var body: some View {
        FeatureScaffold(
            title: "Segurança Digital",
            subtitle: "Geração de primos grandes para RSA via engine nativa."
        ) { result, loading in
            TextField("Bits da chave (par de primos)", text: $bits).keyboardType(.numberPad).textFieldStyle(.roundedBorder)
            Button("Gerar par de primos (nativo)") {
                Task { await gerarRSA(result: result, loading: loading) }
            }
            .buttonStyle(.borderedProminent)
        }
    }

    private func gerarRSA(result: Binding<String>, loading: Binding<Bool>) async {
        guard let b = Int(bits), b >= 512, b <= 16384 else {
            result.wrappedValue = "Use entre 512 e 16384 bits."
            return
        }
        let half = b / 2
        loading.wrappedValue = true
        result.wrappedValue = "⏳ Gerando dois primos de ~\(half) bits…"
        let out = await PPFNative.run {
            let file = FileManager.default.temporaryDirectory
                .appendingPathComponent("rsa_primos_\(UUID().uuidString).txt")
            return PPFNative.executarJob(
                bits: half,
                quantidade: 2,
                threads: ProcessInfo.processInfo.processorCount,
                caminho: file,
                onStatus: { _ in },
                shouldCancel: { appState.cancelRequested }
            )
        }
        result.wrappedValue = out
        loading.wrappedValue = false
    }
}
