import SwiftUI

struct RandomPrimesView: View {
    @State private var quantidade = "5"
    @State private var bits = "1024"
    @State private var startedAt: Date?
    @State private var cancellation: PPFNative.Cancellation?
    @EnvironmentObject private var appState: AppState

    var body: some View {
        FeatureScaffold(
            title: "Primos Aleatórios",
            subtitle: "Acima de \(appState.limiarBitsNativo) bits usa job nativo assíncrono."
        ) { result, loading in
            TextField("Quantidade (1–50)", text: $quantidade)
                .keyboardType(.numberPad)
                .textFieldStyle(.roundedBorder)
            TextField("Bits por primo", text: $bits)
                .keyboardType(.numberPad)
                .textFieldStyle(.roundedBorder)

            Button("Gerar primos aleatórios") {
                Task { await gerar(result: result, loading: loading) }
            }
            .buttonStyle(.borderedProminent)
            .tint(PPFTheme.accent)
            .disabled(loading.wrappedValue)

            if let startedAt {
                TimelineView(.periodic(from: startedAt, by: 1)) { context in
                    Text("Tempo: \(max(0, Int(context.date.timeIntervalSince(startedAt)))) s")
                        .monospacedDigit()
                }
            }

            if let cancellation {
                Button("Cancelar") {
                    cancellation.request()
                    result.wrappedValue = "Cancelando geração…"
                }
                .buttonStyle(.bordered)
            }
        }
        .onDisappear { cancellation?.request() }
    }

    private func gerar(result: Binding<String>, loading: Binding<Bool>) async {
        guard let q = Int(quantidade), (1...50).contains(q),
              let b = Int(bits), b > 0, b <= 33000 else {
            result.wrappedValue = "Quantidade 1–50 e bits 1–33000."
            return
        }

        if b <= 4 {
            let lower = 1 << (b - 1)
            let upper = (1 << b) - 1
            let candidates = (lower...upper).filter(PPFMath.isPrime)
            guard !candidates.isEmpty else {
                result.wrappedValue = "Não existe número primo de 1 bit."
                return
            }
            guard q <= candidates.count else {
                result.wrappedValue = "Há apenas \(candidates.count) primo(s) distinto(s) de \(b) bits. Reduza a quantidade ou aumente os bits."
                return
            }
            let numbers = candidates.shuffled().prefix(q).map(String.init).joined(separator: "\n")
            let report = "PRIMOS ALEATÓRIOS\n\nBits: \(b)\nQuantidade: \(q)\n\n\(numbers)"
            appState.saveTemporaryResultAndOpenViewer(report, prefix: "primos_aleatorios_bits", statusMessage: result)
            return
        }

        let limiar = appState.limiarBitsNativo
        let token = b > limiar ? PPFNative.Cancellation() : nil
        cancellation = token
        startedAt = Date()
        loading.wrappedValue = true
        result.wrappedValue = "🚀 INICIANDO GERAÇÃO…\n   Buscando primos grandes com algoritmos otimizados."

        let report = await PPFNative.run {
            buildRandomPrimesReport(
                bits: b,
                quantity: q,
                limiarNativo: limiar,
                shouldCancel: { token?.isRequested == true },
                onStatus: { status in
                    Task { @MainActor in
                        if loading.wrappedValue && token?.isRequested != true {
                            result.wrappedValue = status
                        }
                    }
                }
            )
        }

        await MainActor.run {
            loading.wrappedValue = false
            startedAt = nil
            cancellation = nil
            if token?.isRequested == true {
                result.wrappedValue = "Operação cancelada."
                return
            }
            appState.saveTemporaryResultAndOpenViewer(report, prefix: "primos_aleatorios_bits", statusMessage: result)
        }
    }

    /// Espelha `executarThreadPrimosAleatorios` no Android.
    private func buildRandomPrimesReport(
        bits: Int,
        quantity: Int,
        limiarNativo: Int,
        shouldCancel: @escaping () -> Bool,
        onStatus: @escaping (String) -> Void
    ) -> String {
        let usarRotaNativa = bits > limiarNativo
        let arquivoPrimos: URL
        do {
            arquivoPrimos = try PPFResultFiles.createTempGiganticPrimesFile(prefix: "primos_gigantes_")
        } catch {
            return "Erro ao criar arquivo temporário: \(error.localizedDescription)"
        }

        if usarRotaNativa {
            let relatorio = PPFNative.executarJob(
                bits: bits,
                quantidade: quantity,
                threads: ProcessInfo.processInfo.processorCount,
                caminho: arquivoPrimos,
                onStatus: onStatus,
                shouldCancel: shouldCancel
            )
            guard !shouldCancel() else { return relatorio }
            return PPFResultFiles.appendFullDecimals(from: arquivoPrimos, to: relatorio)
        }

        let relatorio = PPFNative.gerarPrimosGrandes(
            bits: Int32(bits),
            quantidade: Int32(quantity),
            arquivo: arquivoPrimos.path,
            salvar: true
        )
        return PPFResultFiles.appendFullDecimals(from: arquivoPrimos, to: relatorio)
    }
}
