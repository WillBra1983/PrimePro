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
                TextField("Limite superior da busca", text: $limite)
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
        guard !loading.wrappedValue else { return }
        let family = tipo
        guard let q = Int(quantidade), q > 0, family == .fermat || q <= PPFMath.specialPrimeCountLimit else {
            result.wrappedValue = family == .fermat
                ? "Informe uma quantidade maior que zero."
                : "Informe uma quantidade entre 1 e 10.000."
            return
        }
        let n = family == .fermat ? 0 : (Int(limite) ?? 0)
        guard family == .fermat || n >= 2 else {
            result.wrappedValue = "Informe um inteiro entre 2 e 9.223.372.036.854.775.807."
            return
        }
        loading.wrappedValue = true
        let id = family.nativeID
        let out = await Task.detached {
            PPFNative.specialPrimes(family: id, limit: n, count: q)
        }.value
        await MainActor.run {
            loading.wrappedValue = false
            guard !out.isEmpty else {
                result.wrappedValue = "Não foi possível preparar o resultado."
                return
            }
            appState.saveTemporaryResultAndOpenViewer(
                out,
                prefix: "primos_especiais",
                statusMessage: result
            )
        }
    }
}
