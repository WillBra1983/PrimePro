import SwiftUI

struct PrimeTestView: View {
    @State private var entrada = "97"
    @EnvironmentObject private var appState: AppState

    var body: some View {
        FeatureScaffold(
            title: "Teste de Primalidade",
            subtitle: "Números grandes usam GMP nativo no dispositivo."
        ) { result, loading in
            TextField("Inteiro ou expressão (ex.: 2^31-1)", text: $entrada)
                .textFieldStyle(.roundedBorder)
                .autocorrectionDisabled()

            Button("Analisar") {
                Task { await analisar(result: result, loading: loading) }
            }
            .buttonStyle(.borderedProminent)
            .tint(PPFTheme.primary)
            .disabled(loading.wrappedValue)
        }
    }

    private func analisar(result: Binding<String>, loading: Binding<Bool>) async {
        let expression = entrada.trimmingCharacters(in: .whitespacesAndNewlines)

        loading.wrappedValue = true
        result.wrappedValue = "⏳ Testando primalidade…"

        let inicio = Date()
        let analysis: (String, Bool, String?)? = await PPFNative.run {
            guard let number = PPFNative.evaluateInteger(expression),
                  !number.hasPrefix("-"), number.utf8.count <= 10_000 else {
                return nil
            }
            let prime = PPFNative.testarPrimalidade(number, repeticoes: 25)
            let factors = (prime || number == "0" || number == "1") ? nil : PPFNative.smallFactors(number)
            return (number, prime, factors)
        }
        guard let (numero, primo, fatores) = analysis else {
            loading.wrappedValue = false
            result.wrappedValue = "Informe um inteiro não negativo ou uma expressão válida com até 10.000 dígitos no resultado."
            return
        }
        let tempo = Date().timeIntervalSince(inicio)

        let neither = numero == "0" || numero == "1"
        let status = neither ? "Nem primo nem composto"
            : (primo ? (UInt64(numero) == nil ? "PROVAVELMENTE PRIMO" : "✅ É PRIMO (teste determinístico)") : "❌ É COMPOSTO")
        let detalhesFatores = fatores
            ?? (!primo && !neither ? "Não foi possível preparar a fatoração." : "")
        let relatorio = """
        🔍 TESTE DE PRIMALIDADE
        =======================

        📊 ENTRADA:
           • Expressão: \(expression)
           • Número: \(numero)
           • Dígitos: \(numero.count)

        📋 RESULTADO:
           • Status: \(status)
        \(detalhesFatores)

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
