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

            TextField("Expoente p (ex.: 127 ou 7 + 1)", text: $expoente)
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
        guard !loading.wrappedValue else { return }
        let input = expoente
        loading.wrappedValue = true
        let report: String? = await PPFNative.run {
            guard let decimal = PPFNative.evaluateInteger(input),
                  let p = Int(decimal), (2...Int(Int32.max)).contains(p) else { return nil }
            return PPFNative.mersenneReport(exponent: p, list: false)
        }
        loading.wrappedValue = false
        guard let report else {
            result.wrappedValue = "Informe um expoente inteiro entre 2 e 2.147.483.647."
            return
        }
        guard !report.isEmpty else {
            result.wrappedValue = "Não foi possível calcular o número de Mersenne."
            return
        }
        appState.saveTemporaryResultAndOpenViewer(report, prefix: "numeros_mersenne", statusMessage: result)
    }

    private func listar(result: Binding<String>, loading: Binding<Bool>) async {
        guard !loading.wrappedValue else { return }
        guard let p = Int(expoenteMax), (2...Int(PPF_MERSENNE_LIST_MAX)).contains(p) else {
            result.wrappedValue = "Use um expoente máximo entre 2 e 10.000."
            return
        }
        loading.wrappedValue = true
        let texto = await PPFNative.run {
            PPFNative.mersenneReport(exponent: p, list: true)
        }
        await MainActor.run {
            loading.wrappedValue = false
            guard !texto.isEmpty else {
                result.wrappedValue = "Não foi possível listar os primos de Mersenne."
                return
            }
            appState.saveTemporaryResultAndOpenViewer(
                texto,
                prefix: "numeros_mersenne",
                statusMessage: result
            )
        }
    }
}
