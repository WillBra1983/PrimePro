import SwiftUI

struct PrimeTestView: View {
    @State private var entrada = "97"
    @EnvironmentObject private var appState: AppState

    var body: some View {
        FeatureScaffold(
            title: "Teste de Primalidade",
            subtitle: "Números grandes usam GMP nativo no dispositivo."
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

        let inicio = Date()
        let primo: Bool = await PPFNative.run {
            PPFNative.testarPrimalidade(numero, repeticoes: 25)
        }
        let tempo = Date().timeIntervalSince(inicio)

        let status = primo ? "✅ É PRIMO" : "❌ É COMPOSTO (ou 1)"
        let relatorio = """
        🔍 TESTE DE PRIMALIDADE
        =======================

        📊 ENTRADA:
           • Número: \(numero)
           • Dígitos: \(numero.count)

        📋 RESULTADO:
           • Status: \(status)

        ⏱️ TEMPO DE EXECUÇÃO: \(String(format: "%.3f", tempo)) s
        ⏰ TIMESTAMP: \(formattedNow())
        💡 Se quiser informações estatísticas mais detalhadas, use a função Estatísticas.
        """

        await MainActor.run {
            loading.wrappedValue = false
            appState.saveTemporaryResultAndOpenViewer(
                relatorio,
                prefix: "teste_primalidade",
                statusMessage: result
            )
        }
    }

    private func formattedNow() -> String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "pt_BR")
        f.dateFormat = "yyyy-MM-dd HH:mm:ss"
        return f.string(from: Date())
    }
}
