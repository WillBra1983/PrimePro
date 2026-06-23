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
            TextField("Quantidade (1–50)", text: $quantidade)
                .keyboardType(.numberPad)
                .textFieldStyle(.roundedBorder)
            TextField("Bits por primo", text: $bits)
                .keyboardType(.numberPad)
                .textFieldStyle(.roundedBorder)

            Text(
                "ℹ️ Por se tratar de busca aleatória, o tempo pode variar. "
                + "Se demorar muito, cancele e tente novamente."
            )
            .font(.caption)
            .foregroundStyle(.secondary)

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
        result.wrappedValue = "🚀 INICIANDO GERAÇÃO…\n   Buscando primos grandes com algoritmos otimizados."

        let limiar = appState.limiarBitsNativo
        let report = await PPFNative.run {
            buildRandomPrimesReport(
                bits: b,
                quantity: q,
                limiarNativo: limiar,
                shouldCancel: { appState.cancelRequested },
                onStatus: { status in
                    Task { @MainActor in
                        result.wrappedValue = status
                    }
                }
            )
        }

        await MainActor.run {
            loading.wrappedValue = false
            if appState.cancelRequested && report.contains("cancelad") {
                result.wrappedValue = report
                return
            }
            appState.saveTemporaryResultAndOpenViewer(report, prefix: "primos_aleatorios_bits", statusMessage: result)
        }
    }

    /// Espelha `executarThreadPrimosAleatorios` no Android.
    private func buildRandomPrimesReport(
        bits: Int,
        quantity: Int,
        limiarNativo: Int,
        shouldCancel: @escaping () -> Bool,
        onStatus: @escaping (String) -> Void
    ) -> String {
        let usarRotaNativa = bits > limiarNativo
        let arquivoPrimos: URL
        do {
            arquivoPrimos = try PPFResultFiles.createTempGiganticPrimesFile(prefix: "primos_gigantes_")
        } catch {
            return "Erro ao criar arquivo temporário: \(error.localizedDescription)"
        }

        if usarRotaNativa {
            let relatorio = PPFNative.executarJob(
                bits: bits,
                quantidade: quantity,
                threads: ProcessInfo.processInfo.processorCount,
                caminho: arquivoPrimos,
                onStatus: onStatus,
                shouldCancel: shouldCancel
            )
            return PPFResultFiles.appendFullDecimals(from: arquivoPrimos, to: relatorio)
        }

        let relatorio = PPFNative.gerarPrimosGrandes(
            bits: Int32(bits),
            quantidade: Int32(quantity),
            arquivo: arquivoPrimos.path,
            salvar: true
        )
        return PPFResultFiles.appendFullDecimals(from: arquivoPrimos, to: relatorio)
    }
}
