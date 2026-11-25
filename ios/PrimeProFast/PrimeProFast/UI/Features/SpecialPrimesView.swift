import SwiftUI

struct SpecialPrimesView: View {
    @State private var limite = "10000"
    @State private var quantidade = "10"
    @State private var tipo: PPFMath.SpecialFamily = .twins
    @EnvironmentObject private var appState: AppState

    var body: some View {
        FeatureScaffold(title: "Primos Especiais", subtitle: "Gêmeos, Sophie Germain, Cousins, Sexy, Palíndromos e Fermat.") { result, loading in
            Picker("Tipo", selection: $tipo) {
                ForEach(PPFMath.SpecialFamily.allCases) { family in
                    Text(family.rawValue).tag(family)
                }
            }
            .pickerStyle(.menu)
            if tipo == .fermat {
                Text("Cinco primos de Fermat conhecidos.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            } else {
                TextField("Limite N (até 10.000.000)", text: $limite)
                    .keyboardType(.numberPad)
                    .textFieldStyle(.roundedBorder)
            }
            TextField("Quantidade", text: $quantidade)
                .keyboardType(.numberPad)
                .textFieldStyle(.roundedBorder)
            Button(tipo == .fermat ? "Listar primos conhecidos" : "Sortear primos especiais") {
                Task { await buscar(result: result, loading: loading) }
            }
            .buttonStyle(.borderedProminent)
            .disabled(loading.wrappedValue)
        }
    }

    private func buscar(result: Binding<String>, loading: Binding<Bool>) async {
        let family = tipo
        guard let q = Int(quantidade), (1...PPFMath.specialPrimeCountLimit).contains(q) else {
            result.wrappedValue = "Informe uma quantidade entre 1 e 1.000."
            return
        }
        let n = family == .fermat ? 2 : (Int(limite) ?? 0)
        guard (2...PPFMath.specialPrimeLimit).contains(n) else {
            result.wrappedValue = "Informe um limite entre 2 e 10.000.000."
            return
        }
        loading.wrappedValue = true
        let out = await Task.detached {
            PPFMath.specialPrimes(family, limit: n, count: q)
        }.value
        await MainActor.run {
            loading.wrappedValue = false
            appState.saveTemporaryResultAndOpenViewer(
                out,
                prefix: "primos_especiais",
                statusMessage: result
            )
        }
    }
}
