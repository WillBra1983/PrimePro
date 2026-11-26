import SwiftUI

struct PerfectNumbersView: View {
    @State private var limite = "100000000"
    @State private var number = "28"
    @State private var digits = "3"
    @State private var oddDigits = "6"
    @State private var attempts = "1000"
    @State private var oddSearch: PPFNative.Cancellation?
    @State private var showOddReport = false
    @AppStorage("odd_perfect_next_n") private var oddCheckpoint = 0
    @EnvironmentObject private var appState: AppState

    var body: some View {
        FeatureScaffold(title: "Números Perfeitos", subtitle: "Via primos de Mersenne (Euclides–Euler).") { result, loading in
            TextField("Limite superior", text: $limite)
                .keyboardType(.numberPad)
                .textFieldStyle(.roundedBorder)
            Button("Buscar perfeitos") {
                Task { await buscar(result: result, loading: loading) }
            }
            .buttonStyle(.borderedProminent)
            .disabled(loading.wrappedValue)

            TextField("Número a verificar (ex.: 28 ou 2^4+12)", text: $number)
                .textFieldStyle(.roundedBorder)
                .autocorrectionDisabled()
            Button("Verificar número perfeito") {
                Task { await verify(result: result, loading: loading) }
            }
            .buttonStyle(.bordered)
            .disabled(loading.wrappedValue)

            Divider()
            TextField("Dígitos para buscar perfeitos pares", text: $digits)
                .keyboardType(.numberPad)
                .textFieldStyle(.roundedBorder)
            Button("Buscar por quantidade de dígitos") {
                Task { await searchByDigits(result: result, loading: loading) }
            }
            .buttonStyle(.bordered)
            .disabled(loading.wrappedValue)

            Divider()
            TextField("Dígitos da busca ímpar (1 a 12)", text: $oddDigits)
                .keyboardType(.numberPad)
                .textFieldStyle(.roundedBorder)
            TextField("Tentativas (1 a 100.000)", text: $attempts)
                .keyboardType(.numberPad)
                .textFieldStyle(.roundedBorder)
            Button("Testar candidatos ímpares") {
                Task { await searchOdd(result: result, loading: loading) }
            }
            .buttonStyle(.bordered)
            .disabled(loading.wrappedValue)
            Button("Iniciar busca sequencial") {
                Task { await searchOdd(result: result, loading: loading, sequential: true) }
            }
            .buttonStyle(.bordered)
            .disabled(loading.wrappedValue)
            if oddCheckpoint > 0 {
                Button("Continuar busca salva") {
                    let start = Int64(oddCheckpoint)
                    Task { await searchOdd(result: result, loading: loading, sequential: true, resume: start) }
                }
                .disabled(loading.wrappedValue)
            }
            if let oddSearch {
                Button("Parar e ver resultado") { oddSearch.request() }
                    .buttonStyle(.bordered)
            }
        }
        .onDisappear {
            showOddReport = false
            oddSearch?.request()
        }
    }

    @MainActor
    private func searchByDigits(result: Binding<String>, loading: Binding<Bool>) async {
        guard !loading.wrappedValue else { return }
        guard let count = Int(digits), (1...10_000).contains(count) else {
            result.wrappedValue = "Informe de 1 a 10.000 dígitos."
            return
        }
        loading.wrappedValue = true
        let report = await PPFNative.run {
            PPFNative.perfectReport(mode: Int32(PPF_PERFECT_BY_DIGITS), value: count)
        }
        loading.wrappedValue = false
        guard !report.isEmpty else {
            result.wrappedValue = "Não foi possível calcular o resultado."
            return
        }
        appState.saveTemporaryResultAndOpenViewer(report, prefix: "numeros_perfeitos_digitos", statusMessage: result)
    }

    @MainActor
    private func searchOdd(result: Binding<String>, loading: Binding<Bool>, sequential: Bool = false,
                           resume: Int64? = nil) async {
        guard !loading.wrappedValue else { return }
        let maximum = ppf_perfect_verify_max() - 1
        let limit = Int(PPF_ODD_PERFECT_MAX_ATTEMPTS)
        let digitLimit = Int(PPF_ODD_PERFECT_MAX_DIGITS)
        let digits = Int(oddDigits) ?? 0
        let requested = Int(attempts) ?? 0
        guard (resume != nil || (1...digitLimit).contains(digits)),
              sequential || (1...limit).contains(requested) else {
            result.wrappedValue = "Use de 1 a 12 dígitos e de 1 a 100.000 tentativas."
            return
        }

        let lower = resume == nil ? (1..<digits).reduce(Int64(1)) { value, _ in value * 10 } : 1
        let first = lower | 1
        let last = sequential ? maximum : lower * 10 - 1
        let pool = (last - first) / 2 + 1
        let count = min(Int64(requested), pool)
        var next = resume ?? (sequential ? first : first + 2 * Int64.random(in: 0..<pool))

        let token = PPFNative.Cancellation()
        oddSearch = token
        showOddReport = true
        loading.wrappedValue = true
        result.wrappedValue = "Testando candidatos ímpares…"

        var tested: Int64 = 0
        var found: Int64 = 0
        var failed = false
        var lastUpdate = ProcessInfo.processInfo.systemUptime
        if sequential { oddCheckpoint = Int(next) }
        while !token.isRequested && found == 0 && (sequential ? next <= last : tested < count) {
            if next > last { next = first }
            let start = next
            let size = sequential ? 64 : Int32(min(64, count - tested))
            let batch = await Task.detached { ppf_scan_odd_perfect(start, last, size) }.value
            if batch.next == 0 { failed = true; break }
            next = batch.next
            tested += Int64(batch.tested)
            found = batch.found
            let uptime = ProcessInfo.processInfo.systemUptime
            if uptime - lastUpdate >= 0.25 {
                lastUpdate = uptime
                if sequential { oddCheckpoint = next <= last && found == 0 ? Int(next) : 0 }
                result.wrappedValue = "Candidatos testados: \(tested.formatted())"
            }
        }
        if sequential { oddCheckpoint = next <= last && found == 0 ? Int(next) : 0 }
        oddSearch = nil
        loading.wrappedValue = false
        let outcome: String
        if found > 0 {
            outcome = "Candidato: \(found). Confirme o resultado por um método independente."
        } else if failed {
            outcome = "Não foi possível terminar a busca."
        } else {
            let reason = token.isRequested ? "Busca interrompida. " : ""
            outcome = reason + "Nenhum número perfeito ímpar entre os candidatos testados."
        }
        if showOddReport {
            let report = "BUSCA POR PERFEITOS ÍMPARES\n\nCandidatos testados: \(tested)\n\n\(outcome)"
            appState.saveTemporaryResultAndOpenViewer(report, prefix: "numeros_perfeitos_impares", statusMessage: result)
        }
    }

    private func verify(result: Binding<String>, loading: Binding<Bool>) async {
        guard !loading.wrappedValue else { return }
        let input = number
        loading.wrappedValue = true
        let report: String? = await PPFNative.run {
            guard let decimal = PPFNative.evaluateInteger(input),
                  !decimal.hasPrefix("-"), decimal != "0", decimal.utf8.count <= 10_000 else { return nil }
            return PPFNative.verifyPerfect(decimal)
        }
        loading.wrappedValue = false
        guard let report else {
            result.wrappedValue = "Informe um inteiro positivo de até 10.000 dígitos."
            return
        }
        guard !report.isEmpty else {
            result.wrappedValue = "Não foi possível calcular o resultado."
            return
        }
        appState.saveTemporaryResultAndOpenViewer(report, prefix: "numeros_perfeitos_verificacao", statusMessage: result)
    }

    private func buscar(result: Binding<String>, loading: Binding<Bool>) async {
        guard !loading.wrappedValue else { return }
        guard let n = Int(limite), n >= 1 else {
            result.wrappedValue = "Limite inválido."
            return
        }
        loading.wrappedValue = true
        let texto = await PPFNative.run {
            PPFNative.perfectReport(mode: Int32(PPF_PERFECT_BY_LIMIT), value: n)
        }
        await MainActor.run {
            loading.wrappedValue = false
            guard !texto.isEmpty else {
                result.wrappedValue = "Não foi possível calcular o resultado."
                return
            }
            appState.saveTemporaryResultAndOpenViewer(
                texto,
                prefix: "numeros_perfeitos_busca_sequencial",
                statusMessage: result
            )
        }
    }
}
