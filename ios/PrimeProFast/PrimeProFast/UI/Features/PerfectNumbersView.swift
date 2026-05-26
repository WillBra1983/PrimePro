import SwiftUI

struct PerfectNumbersView: View {
    @State private var limite = "100000000"

    var body: some View {
        FeatureScaffold(title: "Números Perfeitos", subtitle: "Via primos de Mersenne (Euclides–Euler).") { result, _ in
            TextField("Limite superior", text: $limite).keyboardType(.numberPad).textFieldStyle(.roundedBorder)
            Button("Buscar perfeitos") {
                guard let n = Int(limite), n > 6 else {
                    result.wrappedValue = "Limite inválido."
                    return
                }
                result.wrappedValue = PPFMath.perfectNumbers(mode: 1, limit: n)
            }
            .buttonStyle(.borderedProminent)
        }
    }
}
