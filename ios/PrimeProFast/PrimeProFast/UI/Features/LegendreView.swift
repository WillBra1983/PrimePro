import SwiftUI

struct LegendreView: View {
    @State private var x = "100"

    var body: some View {
        FeatureScaffold(
            title: "Conjectura de Legendre",
            subtitle: "Verifica primos entre n² e (n+1)² para n informado."
        ) { result, _ in
            TextField("n", text: $x).keyboardType(.numberPad).textFieldStyle(.roundedBorder)
            Button("Verificar intervalo") {
                guard let n = Int(x), n > 0 else {
                    result.wrappedValue = "Informe n > 0."
                    return
                }
                let low = n * n
                let high = (n + 1) * (n + 1)
                let primos = PPFMath.primesUpTo(high).filter { $0 > low }
                result.wrappedValue = "Intervalo (\(low), \(high)): \(primos.count) primo(s)\n\(primos.map(String.init).joined(separator: ", "))"
            }
            .buttonStyle(.borderedProminent)
        }
    }
}
