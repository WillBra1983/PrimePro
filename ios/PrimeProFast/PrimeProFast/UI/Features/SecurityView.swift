import SwiftUI

struct SecurityView: View {
    @State private var bits = "2048"
    @EnvironmentObject private var appState: AppState

    var body: some View {
        FeatureScaffold(
            title: "Segurança Digital",
            subtitle: "Geração de primos grandes para RSA via engine nativa."
        ) { result, loading in
            TextField("Bits da chave (par de primos)", text: $bits)
                .keyboardType(.numberPad)
                .textFieldStyle(.roundedBorder)
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
        appState.cancelRequested = false
        loading.wrappedValue = true
        result.wrappedValue = "⏳ Gerando dois primos de ~\(half) bits…"

        let report = await PPFNative.run {
            let file: URL
            do {
                file = try PPFResultFiles.createTempGiganticPrimesFile(prefix: "primos_nativo_rsa_")
            } catch {
                return "Erro ao criar arquivo: \(error.localizedDescription)"
            }
            let relatorio = PPFNative.executarJob(
                bits: half,
                quantidade: 2,
                threads: ProcessInfo.processInfo.processorCount,
                caminho: file,
                onStatus: { status in
                    Task { @MainActor in
                        result.wrappedValue = status
                    }
                },
                shouldCancel: { appState.cancelRequested }
            )
            return PPFResultFiles.appendFullDecimals(from: file, to: relatorio)
        }

        await MainActor.run {
            loading.wrappedValue = false
            appState.saveTemporaryResultAndOpenViewer(
                report,
                prefix: "seguranca_digital_rsa",
                statusMessage: result
            )
        }
    }
}
