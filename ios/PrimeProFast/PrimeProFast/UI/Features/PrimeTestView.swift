import SwiftUI

struct PrimeTestView: View {
    @State private var entrada = "97"

    var body: some View {
        FeatureScaffold(
            title: "Teste de Primalidade",
            subtitle: "Números grandes usam GMP nativo (mesmo algoritmo do Android)."
        ) { result, loading in
            TextField("Número decimal", text: $entrada)
                .textFieldStyle(.roundedBorder)

            Button("Analisar") {
                Task { await analisar(result: result, loading: loading) }
            }
            .buttonStyle(.borderedProminent)
            .tint(PPFTheme.primary)
        }
    }

    private func analisar(result: Binding<String>, loading: Binding<Bool>) async {
        let numero = entrada.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !numero.isEmpty, numero.allSatisfy(\.isNumber) else {
            result.wrappedValue = "Digite um inteiro decimal válido."
            return
        }
        if numero.count > 10000 {
            result.wrappedValue = "Máximo: 10.000 dígitos."
            return
        }
        loading.wrappedValue = true
        result.wrappedValue = "⏳ Testando primalidade…"
        let primo: Bool = await PPFNative.run {
            PPFNative.testarPrimalidade(numero, repeticoes: 25)
        }
        let status = primo ? "✅ É PRIMO" : "❌ É COMPOSTO (ou 1)"
        result.wrappedValue = "Entrada: \(numero)\nDígitos: \(numero.count)\n\n\(status)"
        loading.wrappedValue = false
    }
}
