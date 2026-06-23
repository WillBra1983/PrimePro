import SwiftUI

struct LegendreView: View {
    @State private var x = "100"
    @EnvironmentObject private var appState: AppState

    var body: some View {
        FeatureScaffold(
            title: "Conjectura de Legendre",
            subtitle: "Verifica primos entre n² e (n+1)² para n informado."
        ) { result, loading in
            TextField("n", text: $x)
                .keyboardType(.numberPad)
                .textFieldStyle(.roundedBorder)
            Button("Verificar intervalo") {
                Task { await verificar(result: result, loading: loading) }
            }
            .buttonStyle(.borderedProminent)
        }
    }

    private func verificar(result: Binding<String>, loading: Binding<Bool>) async {
        guard let n = Int(x), n > 0 else {
            result.wrappedValue = "Informe n > 0."
            return
        }
        loading.wrappedValue = true
        let low = n * n
        let high = (n + 1) * (n + 1)
        let texto = await Task.detached {
            let primos = PPFMath.primesUpTo(high).filter { $0 > low }
            return """
            📐 CONJECTURA DE LEGENDRE
            Intervalo (\(low), \(high))
            Primos encontrados: \(primos.count)
            \(primos.map(String.init).joined(separator: ", "))
            """
        }.value
        await MainActor.run {
            loading.wrappedValue = false
            appState.saveTemporaryResultAndOpenViewer(
                texto,
                prefix: "conjectura_legendre_intervalo",
                statusMessage: result
            )
        }
    }
}
