import SwiftUI

struct MersenneView: View {
    @State private var expoenteMax = "30"
    @State private var expoente = "127"
    @EnvironmentObject private var appState: AppState

    var body: some View {
        FeatureScaffold(title: "Números de Mersenne", subtitle: "Primos da forma 2^p − 1.") { result, loading in
            TextField("Expoente máximo p", text: $expoenteMax)
                .keyboardType(.numberPad)
                .textFieldStyle(.roundedBorder)
            Button("Listar Mersenne primos") {
                Task { await listar(result: result, loading: loading) }
            }
            .buttonStyle(.borderedProminent)
            .disabled(loading.wrappedValue)

            TextField("Expoente p (ex.: 127 ou 2^7-1)", text: $expoente)
                .textFieldStyle(.roundedBorder)
                .autocorrectionDisabled()
            Button("Calcular número de Mersenne") {
                Task { await calcular(result: result, loading: loading) }
            }
            .buttonStyle(.bordered)
            .disabled(loading.wrappedValue)
        }
    }

    private func calcular(result: Binding<String>, loading: Binding<Bool>) async {
        let input = expoente
        loading.wrappedValue = true
        let report: String? = await PPFNative.run {
            guard let decimal = PPFNative.evaluateInteger(input),
                  let p = Int(decimal), (2...33_219).contains(p),
                  let number = PPFNative.evaluateInteger("2^\(p)-1") else { return nil }
            let prime = PPFNative.testarPrimalidade(number)
            let status = prime ? "Provavelmente primo (teste GMP)." : "Composto."
            return "M(\(p)) = 2^\(p) - 1\n\n\(number)\n\n\(number.count) dígitos. \(status)"
        }
        loading.wrappedValue = false
        guard let report else {
            result.wrappedValue = "Informe um expoente inteiro entre 2 e 33.219."
            return
        }
        appState.saveTemporaryResultAndOpenViewer(report, prefix: "numeros_mersenne", statusMessage: result)
    }

    private func listar(result: Binding<String>, loading: Binding<Bool>) async {
        guard let p = Int(expoenteMax), (2...10_000).contains(p) else {
            result.wrappedValue = "Use um expoente máximo entre 2 e 10.000."
            return
        }
        loading.wrappedValue = true
        let texto = await PPFNative.run {
            PPFMath.mersennePrimes(maxExponent: p)
        }
        await MainActor.run {
            loading.wrappedValue = false
            appState.saveTemporaryResultAndOpenViewer(
                texto,
                prefix: "numeros_mersenne",
                statusMessage: result
            )
        }
    }
}
