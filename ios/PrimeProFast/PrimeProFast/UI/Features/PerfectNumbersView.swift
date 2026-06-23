import SwiftUI

struct PerfectNumbersView: View {
    @State private var limite = "100000000"
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
        }
    }

    private func buscar(result: Binding<String>, loading: Binding<Bool>) async {
        guard let n = Int(limite), n > 6 else {
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
