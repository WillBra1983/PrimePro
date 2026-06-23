import SwiftUI

struct MersenneView: View {
    @State private var expoenteMax = "30"
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
        }
    }

    private func listar(result: Binding<String>, loading: Binding<Bool>) async {
        guard let p = Int(expoenteMax), p >= 2 else {
            result.wrappedValue = "p inválido."
            return
        }
        loading.wrappedValue = true
        let texto = await Task.detached {
            PPFMath.mersennePrimes(maxExponent: p, maxCount: 20)
        }.value
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
