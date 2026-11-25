import SwiftUI

struct PerfectNumbersView: View {
    @State private var limite = "100000000"
    @State private var number = "28"
    @State private var digits = "3"
    @State private var oddDigits = "6"
    @State private var attempts = "1000"
    @State private var oddSearch: PPFNative.Cancellation?
    @State private var showOddReport = false
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
            TextField("Dígitos da busca ímpar (1 a 9)", text: $oddDigits)
                .keyboardType(.numberPad)
                .textFieldStyle(.roundedBorder)
            TextField("Tentativas (1 a 5.000)", text: $attempts)
                .keyboardType(.numberPad)
                .textFieldStyle(.roundedBorder)
            Button("Testar candidatos ímpares") {
                Task { await searchOdd(result: result, loading: loading) }
            }
            .buttonStyle(.bordered)
            .disabled(loading.wrappedValue)
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
        guard let count = Int(digits), (1...10_000).contains(count) else {
            result.wrappedValue = "Informe de 1 a 10.000 dígitos."
            return
        }
        loading.wrappedValue = true
        let report = await PPFNative.run {
            // GIMPS verified exponents covering perfect numbers up to 10,000 digits.
            let exponents = [2, 3, 5, 7, 13, 17, 19, 31, 61, 89, 107, 127, 521, 607,
                             1279, 2203, 2281, 3217, 4253, 4423, 9689, 9941, 11213]
            let matches = exponents.compactMap { p -> String? in
                guard let value = PPFNative.evaluateInteger("2^(\(p)-1)*(2^\(p)-1)"),
                      value.count == count else { return nil }
                return "Expoente de Mersenne: \(p)\n\(value)"
            }
            return "PERFEITOS PARES — \(count) DÍGITOS\n\n" +
                (matches.isEmpty ? "Nenhum número perfeito par conhecido com esse tamanho." : matches.joined(separator: "\n\n"))
        }
        loading.wrappedValue = false
        appState.saveTemporaryResultAndOpenViewer(report, prefix: "numeros_perfeitos_digitos", statusMessage: result)
    }

    @MainActor
    private func searchOdd(result: Binding<String>, loading: Binding<Bool>) async {
        guard !loading.wrappedValue,
              let digits = Int(oddDigits), (1...9).contains(digits),
              let attempts = Int(attempts), (1...5_000).contains(attempts) else {
            result.wrappedValue = "Use de 1 a 9 dígitos e de 1 a 5.000 tentativas."
            return
        }
        let token = PPFNative.Cancellation()
        oddSearch = token
        showOddReport = true
        loading.wrappedValue = true
        result.wrappedValue = "Testando candidatos ímpares…"
        let report = await Task.detached {
            let lower = (1..<digits).reduce(1) { value, _ in value * 10 }
            let firstOdd = lower.isMultiple(of: 2) ? lower + 1 : lower
            let pool = (lower * 10 - firstOdd + 1) / 2
            let count = min(attempts, pool)
            let offset = Int.random(in: 0..<pool)
            var tested = 0
            var found: Int?
            for step in 0..<count {
                if token.isRequested { break }
                let n = firstOdd + 2 * ((offset + step) % pool)
                tested += 1
                if PPFMath.properDivisorSum(n) == n {
                    found = n
                    break
                }
            }
            let summary = found.map { "Candidato encontrado: \($0)" }
                ?? "Nenhum número perfeito ímpar encontrado entre os candidatos testados."
            return "BUSCA EXPERIMENTAL POR PERFEITOS ÍMPARES\n\nDígitos: \(digits)\nCandidatos testados: \(tested)\n\n\(summary)"
        }.value
        oddSearch = nil
        loading.wrappedValue = false
        if showOddReport {
            appState.saveTemporaryResultAndOpenViewer(report, prefix: "numeros_perfeitos_impares", statusMessage: result)
        }
    }

    private func verify(result: Binding<String>, loading: Binding<Bool>) async {
        let input = number
        loading.wrappedValue = true
        let report: String? = await PPFNative.run {
            guard let decimal = PPFNative.evaluateInteger(input),
                  let n = Int(decimal), (1...1_000_000_000_000).contains(n) else { return nil }
            let sum = PPFMath.properDivisorSum(n)
            let status = sum == n ? "É um número perfeito." : "Não é um número perfeito."
            return "Número: \(n)\nSoma dos divisores próprios: \(sum)\n\n\(status)"
        }
        loading.wrappedValue = false
        guard let report else {
            result.wrappedValue = "Informe um inteiro entre 1 e 1 trilhão."
            return
        }
        appState.saveTemporaryResultAndOpenViewer(report, prefix: "numeros_perfeitos_verificacao", statusMessage: result)
    }

    private func buscar(result: Binding<String>, loading: Binding<Bool>) async {
        guard let n = Int(limite), n >= 6 else {
            result.wrappedValue = "Limite inválido."
            return
        }
        loading.wrappedValue = true
        let texto = await Task.detached {
            PPFMath.perfectNumbers(mode: 1, limit: n)
        }.value
        await MainActor.run {
            loading.wrappedValue = false
            appState.saveTemporaryResultAndOpenViewer(
                texto,
                prefix: "numeros_perfeitos_busca_sequencial",
                statusMessage: result
            )
        }
    }
}
