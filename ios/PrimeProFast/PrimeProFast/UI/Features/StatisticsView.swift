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
                approximation(result: result)
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
        guard let n = Int(input), (2...10_000_000).contains(n) else {
            result.wrappedValue = "Para a contagem exata, use um inteiro entre 2 e 10.000.000."
            return
        }
        loading.wrappedValue = true
        let report = await Task.detached {
            let start = Date()
            let primes = PPFMath.primesUpTo(n)
            var largestGap = 0
            var twinPairs = 0
            for (previous, next) in zip(primes, primes.dropFirst()) {
                let gap = next - previous
                largestGap = max(largestGap, gap)
                if gap == 2 { twinPairs += 1 }
            }
            return """
            Limite: \(n.formatted())
            Primos encontrados: \(primes.count.formatted())
            Maior primo: \(primes.last?.formatted() ?? "—")
            Densidade: \((Double(primes.count) / Double(n)).formatted(.percent.precision(.fractionLength(3))))
            Maior distância entre primos consecutivos: \(largestGap.formatted())
            Pares de primos gêmeos: \(twinPairs.formatted())
            Tempo: \(Date().timeIntervalSince(start).formatted(.number.precision(.fractionLength(3)))) s
            """
        }.value
        loading.wrappedValue = false
        appState.saveTemporaryResultAndOpenViewer(report, prefix: "estatisticas_primos_completas", statusMessage: result)
    }

    private func approximation(result: Binding<String>) {
        guard let n = Double(input), n.isFinite, (2...1e100).contains(n) else {
            result.wrappedValue = "Para a estimativa, use um número entre 2 e 10^100 (ex.: 1e100)."
            return
        }
        let steps = 2048
        let start = log(2.0)
        let step = (log(n) - start) / Double(steps)
        var sum = 0.0
        // Integrate exp(u)/u after substituting u = log(t).
        for i in 0...steps {
            let u = start + Double(i) * step
            let weight = i == 0 || i == steps ? 1.0 : (i.isMultiple(of: 2) ? 2.0 : 4.0)
            sum += weight * exp(u) / u
        }
        let report = """
        N = \(n)
        Estimativa N / ln(N): \(n / log(n))
        Integral de 2 até N de 1 / ln(t): \(sum * step / 3)
        """
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
