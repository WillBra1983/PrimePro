import SwiftUI

struct SpecialPrimesView: View {
    @State private var limite = "10000"
    @State private var quantidade = "10"
    @State private var tipo = 0
    private let tipos = ["Gêmeos", "Sophie Germain", "Cousins", "Sexy", "Palíndromos", "Fermat"]

    var body: some View {
        FeatureScaffold(title: "Primos Especiais", subtitle: "Famílias clássicas — Swift para faixa moderada.") { result, loading in
            Picker("Tipo", selection: $tipo) {
                ForEach(0..<tipos.count, id: \.self) { i in
                    Text(tipos[i]).tag(i)
                }
            }
            .pickerStyle(.menu)
            TextField("Limite N", text: $limite).keyboardType(.numberPad).textFieldStyle(.roundedBorder)
            TextField("Quantidade", text: $quantidade).keyboardType(.numberPad).textFieldStyle(.roundedBorder)
            Button("Buscar") { Task { await buscar(result: result, loading: loading) } }
                .buttonStyle(.borderedProminent)
        }
    }

    private func buscar(result: Binding<String>, loading: Binding<Bool>) async {
        guard let n = Int(limite), let q = Int(quantidade), n > 2, q > 0 else {
            result.wrappedValue = "Parâmetros inválidos."
            return
        }
        loading.wrappedValue = true
        let out = await Task.detached {
            switch tipo {
            case 0: return PPFMath.twinPrimes(limit: n, count: q)
            case 1: return PPFMath.sophieGermain(limit: n, count: q)
            default:
                return "Tipo \(tipos[tipo]): em expansão para iOS — use Primos por Intervalo ou Teste de Primalidade para valores grandes."
            }
        }.value
        result.wrappedValue = out
        loading.wrappedValue = false
    }
}
