import SwiftUI

struct StatisticsView: View {
    @EnvironmentObject private var appState: AppState
    @State private var input = "1000"

    var body: some View {
        FeatureScaffold(
            title: "Estatísticas",
            subtitle: "Distribuição e estimativas de números primos."
        ) { result, loading in
            TextField("Limite N", text: $input)
                .textFieldStyle(.roundedBorder)
                .autocorrectionDisabled()
            Button("Estatística completa") {
                Task { await distribution(result: result, loading: loading) }
            }
            .buttonStyle(.borderedProminent)
            .disabled(loading.wrappedValue)
            Button("Aproximação logarítmica") {
                Task { await approximation(result: result, loading: loading) }
            }
            .buttonStyle(.bordered)
            .disabled(loading.wrappedValue)
            Button("Informações do dispositivo") {
                Task { await atualizar(result: result, loading: loading) }
            }
            .buttonStyle(.bordered)
            .disabled(loading.wrappedValue)
        }
    }

    private func distribution(result: Binding<String>, loading: Binding<Bool>) async {
        guard !loading.wrappedValue else { return }
        guard let n = Int(input.trimmingCharacters(in: .whitespacesAndNewlines)),
              (2...Int(ppf_exact_stats_max())).contains(n) else {
            result.wrappedValue = "Para a contagem exata, use um inteiro entre 2 e 10 bilhões."
            return
        }
        loading.wrappedValue = true
        let report = await PPFNative.run { PPFNative.primeStats(limit: n) }
        loading.wrappedValue = false
        guard !report.isEmpty else {
            result.wrappedValue = "Não foi possível calcular as estatísticas."
            return
        }
        appState.saveTemporaryResultAndOpenViewer(report, prefix: "estatisticas_primos_completas", statusMessage: result)
    }

    private func approximation(result: Binding<String>, loading: Binding<Bool>) async {
        guard !loading.wrappedValue else { return }
        let expression = input.trimmingCharacters(in: .whitespacesAndNewlines)
        loading.wrappedValue = true
        let report: String? = await PPFNative.run {
            guard let decimal = PPFNative.evaluateInteger(expression), !decimal.hasPrefix("-"),
                  decimal != "0", decimal != "1",
                  decimal.utf8.count <= Int(PPF_APPROXIMATION_MAX_DIGITS) else { return nil }
            return PPFNative.primeApproximation(limit: decimal)
        }
        loading.wrappedValue = false
        guard let report else {
            result.wrappedValue = "Use um inteiro a partir de 2, com até 100.000 dígitos (ex.: 10^500)."
            return
        }
        guard !report.isEmpty else {
            result.wrappedValue = "Não foi possível calcular a aproximação."
            return
        }
        appState.saveTemporaryResultAndOpenViewer(report, prefix: "aproximacao_logaritmica", statusMessage: result)
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
                prefix: "estatisticas_dispositivo",
                statusMessage: result
            )
        }
    }
}
