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
    @AppStorage("legendre_next_n") private var resumeAt = 0
    @AppStorage("legendre_next_decimal") private var savedDecimal = ""
    @EnvironmentObject private var appState: AppState

    private var checkpoint: String {
        savedDecimal.isEmpty && resumeAt > 0 ? String(resumeAt) : savedDecimal
    }

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
            } else if mode == .continuous && !checkpoint.isEmpty {
                Button("Continuar busca salva") {
                    let checkpoint = self.checkpoint
                    Task { await searchContinuously(result: result, loading: loading, resume: checkpoint) }
                }
                .buttonStyle(.bordered)
                .disabled(loading.wrappedValue)
            }
        }
        .onDisappear {
            presentContinuousReport = false
            continuousSearch?.request()
        }
    }

    private func verificar(result: Binding<String>, loading: Binding<Bool>) async {
        guard !loading.wrappedValue else { return }
        let firstInput = x
        let lastInput = mode == .range ? final : x
        loading.wrappedValue = true
        let texto: String? = await PPFNative.run {
            guard let first = PPFNative.evaluateInteger(firstInput),
                  let last = PPFNative.evaluateInteger(lastInput) else {
                return nil
            }
            return PPFNative.legendreReport(first: first, last: last)
        }
        await MainActor.run {
            loading.wrappedValue = false
            guard let texto else {
                result.wrappedValue = "Use valores positivos de até 10.000 dígitos, com n final maior ou igual ao inicial."
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
    private func searchContinuously(result: Binding<String>, loading: Binding<Bool>, resume: String? = nil) async {
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
        let initial = await PPFNative.run { resume ?? PPFNative.evaluateInteger(input) }
        guard let start = initial, start != "0", !start.hasPrefix("-"),
              start.utf8.count <= Int(PPF_LEGENDRE_MAX_DIGITS) else {
            result.wrappedValue = "Informe um n positivo com até 10.000 dígitos."
            return
        }

        var next = start
        var checked = 0
        var lastWitness = ""
        var inconclusive = false
        var lastUpdate = ProcessInfo.processInfo.systemUptime
        savedDecimal = start
        resumeAt = 0
        while !next.isEmpty && !token.isRequested && !inconclusive {
            let first = next
            let batch = await Task.detached {
                var n = first
                var latest = ""
                var examined = 0
                let count = first.utf8.count <= 10 ? 100 : 1
                for _ in 0..<count where !token.isRequested {
                    let witness = PPFNative.legendreWitness(n)
                    if witness.isEmpty {
                        return (n, latest, examined, !token.isRequested)
                    }
                    latest = witness
                    examined += 1
                    guard let following = PPFNative.evaluateInteger(n + "+1"),
                          following.utf8.count <= Int(PPF_LEGENDRE_MAX_DIGITS) else {
                        return ("", latest, examined, false)
                    }
                    n = following
                }
                return (n, latest, examined, false)
            }.value
            next = batch.0
            if !batch.1.isEmpty { lastWitness = batch.1 }
            checked += batch.2
            inconclusive = batch.3
            let uptime = ProcessInfo.processInfo.systemUptime
            if uptime - lastUpdate >= 1 {
                lastUpdate = uptime
                savedDecimal = next
                result.wrappedValue = "Intervalos com candidato: \(checked.formatted())"
            }
        }
        savedDecimal = next

        let outcome: String
        if inconclusive {
            outcome = "Busca inconclusiva para n = \(next). A ausência de candidato não refuta a conjectura."
        } else {
            outcome = token.isRequested ? "Busca interrompida." : "Limite de n atingido."
        }
        let report = """
        CONJECTURA DE LEGENDRE - BUSCA CONTÍNUA

        n inicial: \(start)
        Intervalos com primo ou provável primo: \(checked.formatted())
        Último candidato: \(lastWitness.isEmpty ? "nenhum" : lastWitness)
        \(outcome)
        """
        if presentContinuousReport {
            appState.saveTemporaryResultAndOpenViewer(report, prefix: "conjectura_legendre_continua", statusMessage: result)
        }
    }
}
