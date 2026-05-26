import SwiftUI

struct MersenneView: View {
    @State private var expoenteMax = "30"

    var body: some View {
        FeatureScaffold(title: "Números de Mersenne", subtitle: "Primos da forma 2^p − 1.") { result, _ in
            TextField("Expoente máximo p", text: $expoenteMax).keyboardType(.numberPad).textFieldStyle(.roundedBorder)
            Button("Listar Mersenne primos") {
                guard let p = Int(expoenteMax), p >= 2 else {
                    result.wrappedValue = "p inválido."
                    return
                }
                result.wrappedValue = PPFMath.mersennePrimes(maxExponent: p, maxCount: 20)
            }
            .buttonStyle(.borderedProminent)
        }
    }
}
