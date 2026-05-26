import SwiftUI

struct IntervalPrimesView: View {
    @State private var maxN = "1000000"
    @EnvironmentObject private var appState: AppState

    var body: some View {
        FeatureScaffold(
            title: "Primos por Intervalo",
            subtitle: "Engine nativa C++/GMP — mesmo núcleo do Android."
        ) { result, loading in
            TextField("Valor máximo N", text: $maxN)
                .keyboardType(.numberPad)
                .textFieldStyle(.roundedBorder)

            Button("Calcular na tela") {
                Task { await calcular(salvar: false, result: result, loading: loading) }
            }
            .buttonStyle(.borderedProminent)
            .tint(PPFTheme.primary)

            Button("Calcular e salvar TXT") {
                Task { await calcular(salvar: true, result: result, loading: loading) }
            }
            .buttonStyle(.bordered)
        }
    }

    private func calcular(salvar: Bool, result: Binding<String>, loading: Binding<Bool>) async {
        guard let n = Int64(maxN), n >= 2 else {
            result.wrappedValue = "Informe N ≥ 2."
            return
        }
        appState.cancelRequested = false
        loading.wrappedValue = true
        result.wrappedValue = "⏳ Calculando primos até \(n)…"
        let output: String = await PPFNative.run {
            let texto = PPFNative.calcularPrimos(n)
            if salvar {
                let url = FileManager.default.temporaryDirectory
                    .appendingPathComponent("primos_intervalo_\(n).txt")
                try? texto.write(to: url, atomically: true, encoding: .utf8)
                return texto + "\n\n💾 Salvo em: \(url.path)"
            }
            return texto
        }
        result.wrappedValue = output
        loading.wrappedValue = false
    }
}
