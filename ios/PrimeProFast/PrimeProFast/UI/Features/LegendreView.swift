import SwiftUI

struct LegendreView: View {
    private enum Mode: String {
        case single = "Um valor"
        case range = "Faixa"
        case continuous = "Contínua"
    }

    @State private var x = "100"
    @State private var final = "110"
    @State private var mode: Mode = .single
    @State private var continuousSearch: PPFNative.Cancellation?
    @State private var presentContinuousReport = false
    @EnvironmentObject private var appState: AppState

    var body: some View {
        FeatureScaffold(
            title: "Conjectura de Legendre",
            subtitle: "Verifica primos entre n² e (n+1)² para n informado."
        ) { result, loading in
            Picker("Modo", selection: $mode) {
                Text(Mode.single.rawValue).tag(Mode.single)
                Text(Mode.range.rawValue).tag(Mode.range)
                Text(Mode.continuous.rawValue).tag(Mode.continuous)
            }
            .pickerStyle(.segmented)
            .disabled(loading.wrappedValue)
            TextField(mode == .single ? "n" : "n inicial", text: $x)
                .textFieldStyle(.roundedBorder)
                .autocorrectionDisabled()
            if mode == .range {
                TextField("n final", text: $final)
                    .textFieldStyle(.roundedBorder)
                    .autocorrectionDisabled()
            }
            Button(mode == .continuous ? "Iniciar busca contínua" : "Verificar intervalo") {
                Task {
                    if mode == .continuous {
                        await searchContinuously(result: result, loading: loading)
                    } else {
                        await verificar(result: result, loading: loading)
                    }
                }
            }
            .buttonStyle(.borderedProminent)
            .disabled(loading.wrappedValue)
            if let continuousSearch {
                Button("Parar e ver resultado") { continuousSearch.request() }
                    .buttonStyle(.bordered)
            }
        }
        .onDisappear {
            presentContinuousReport = false
            continuousSearch?.request()
        }
    }

    private func verificar(result: Binding<String>, loading: Binding<Bool>) async {
        let firstInput = x
        let lastInput = mode == .range ? final : x
        loading.wrappedValue = true
        let texto: String? = await PPFNative.run {
            guard let first = PPFNative.evaluateInteger(firstInput).flatMap({ Int($0) }),
                  let last = PPFNative.evaluateInteger(lastInput).flatMap({ Int($0) }),
                  (1...1_000_000).contains(first), (first...1_000_000).contains(last),
                  last - first < 100, (last - first + 1) * (2 * last + 1) <= 5_000_000 else {
                return nil
            }
            var sections = ["CONJECTURA DE LEGENDRE"]
            for n in first...last {
                let low = n * n
                let high = (n + 1) * (n + 1)
                let primes = ((low + 1)..<high).filter(PPFMath.isPrime)
                sections.append("n = \(n) | Intervalo (\(low), \(high))\nPrimos encontrados: \(primes.count)\n" +
                                primes.map(String.init).joined(separator: ", "))
            }
            return sections.joined(separator: "\n\n")
        }
        await MainActor.run {
            loading.wrappedValue = false
            guard let texto else {
                result.wrappedValue = "Use valores de n entre 1 e 1.000.000. Para faixas, informe até 100 valores e reduza a faixa se ela for muito grande."
                return
            }
            appState.saveTemporaryResultAndOpenViewer(
                texto,
                prefix: "conjectura_legendre_intervalo",
                statusMessage: result
            )
        }
    }

    @MainActor
    private func searchContinuously(result: Binding<String>, loading: Binding<Bool>) async {
        guard !loading.wrappedValue else { return }
        let input = x
        let token = PPFNative.Cancellation()
        continuousSearch = token
        presentContinuousReport = true
        loading.wrappedValue = true
        result.wrappedValue = "Verificando quadrados consecutivos…"
        defer {
            continuousSearch = nil
            loading.wrappedValue = false
        }
        guard let start = await PPFNative.run({ PPFNative.evaluateInteger(input).flatMap { Int($0) } }),
              (1...1_000_000).contains(start) else {
            result.wrappedValue = "Informe um valor inicial de n entre 1 e 1.000.000."
            return
        }

        var next = start
        var lastWitness: Int?
        var missing: Int?
        while next <= 1_000_000 && !token.isRequested && missing == nil {
            let first = next
            let end = min(1_000_000, first + 1999)
            let batch = await Task.detached {
                var n = first
                var latest: Int?
                while n <= end && !token.isRequested {
                    let lower = n * n
                    let upper = (n + 1) * (n + 1)
                    var candidate = lower + 1
                    while candidate < upper && !PPFMath.isPrime(candidate) {
                        candidate += 1
                        if token.isRequested { return (n, latest, nil as Int?) }
                    }
                    if candidate == upper { return (n, latest, Optional(n)) }
                    latest = candidate
                    n += 1
                }
                return (n, latest, nil as Int?)
            }.value
            next = batch.0
            lastWitness = batch.1 ?? lastWitness
            missing = batch.2
            result.wrappedValue = "Intervalos verificados: \((next - start).formatted())"
        }

        let report = """
        CONJECTURA DE LEGENDRE — BUSCA CONTÍNUA

        n inicial: \(start.formatted())
        Intervalos com pelo menos um primo: \((next - start).formatted())
        Último primo encontrado: \(lastWitness?.formatted() ?? "—")
        \(missing.map { "Nenhum primo encontrado no intervalo para n = \($0)." }
          ?? (token.isRequested ? "Busca interrompida." : "Limite de n atingido."))
        """
        if presentContinuousReport {
            appState.saveTemporaryResultAndOpenViewer(report, prefix: "conjectura_legendre_continua", statusMessage: result)
        }
    }
}
