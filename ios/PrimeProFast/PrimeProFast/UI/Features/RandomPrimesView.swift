import SwiftUI

struct RandomPrimesView: View {
    private enum Field: Hashable { case quantity, bits }

    @FocusState private var focusedField: Field?
    @State private var quantidade = "5"
    @State private var bits = "1024"
    @State private var startedAt: Date?
    @State private var cancellation: PPFNative.Cancellation?
    @State private var confirmsLargeRequest = false
    @State private var pendingRequest: GenerationRequest?
    @State private var isVisible = false
    @EnvironmentObject private var appState: AppState

    private struct GenerationRequest {
        let bits: Int
        let quantity: Int
    }

    var body: some View {
        FeatureScaffold(
            title: "Primos Aleatórios",
            subtitle: "O tempo de busca pode variar mesmo com a mesma quantidade de bits."
        ) { result, loading in
            TextField("Quantidade (1–50)", text: $quantidade)
                .keyboardType(.numberPad)
                .textFieldStyle(.roundedBorder)
                .focused($focusedField, equals: .quantity)
            TextField("Bits por primo", text: $bits)
                .keyboardType(.numberPad)
                .textFieldStyle(.roundedBorder)
                .focused($focusedField, equals: .bits)

            Button("Gerar primos aleatórios") {
                guard let q = Int(quantidade), (1...50).contains(q),
                      let b = Int(bits), (1...33000).contains(b) else {
                    result.wrappedValue = "Quantidade 1–50 e bits 1–33000."
                    return
                }
                focusedField = nil
                let request = GenerationRequest(bits: b, quantity: q)
                if b > appState.limiarBitsNativo {
                    pendingRequest = request
                    confirmsLargeRequest = true
                } else {
                    Task { await gerar(request, result: result, loading: loading) }
                }
            }
            .buttonStyle(.borderedProminent)
            .tint(PPFTheme.accent)
            .disabled(loading.wrappedValue)
            .alert("Primos acima de \(appState.limiarBitsNativo) bits", isPresented: $confirmsLargeRequest,
                   presenting: pendingRequest) { request in
                Button("Gerar neste aparelho") {
                    pendingRequest = nil
                    Task { await gerar(request, result: result, loading: loading) }
                }
                Button("Cancelar", role: .cancel) { pendingRequest = nil }
            } message: { _ in
                Text("A geração pode demorar, consumir bateria e aquecer o aparelho.")
            }

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
                .disabled(cancellation.isRequested)
            }
        }
        .scrollDismissesKeyboard(.interactively)
        .toolbar {
            ToolbarItemGroup(placement: .keyboard) {
                Spacer()
                Button("Concluir") { focusedField = nil }
            }
        }
        .onAppear { isVisible = true }
        .onDisappear {
            isVisible = false
            cancellation?.request()
        }
    }

    @MainActor private func gerar(_ request: GenerationRequest, result: Binding<String>, loading: Binding<Bool>) async {
        guard !loading.wrappedValue else { return }
        let q = request.quantity
        let b = request.bits

        if b <= Int(PPF_SHORT_PRIME_MAX_BITS) {
            guard b > 1 else {
                result.wrappedValue = "Não existe número primo de 1 bit."
                return
            }
            let available = ppf_short_prime_count(Int32(b))
            guard available > 0 else {
                result.wrappedValue = "Não foi possível preparar o sorteio."
                return
            }
            guard q <= Int(available) else {
                result.wrappedValue = "Há apenas \(available) primos distintos de \(b) bits. Reduza a quantidade ou aumente os bits."
                return
            }
            let report = PPFNative.shortPrimes(bits: b, count: q)
            guard !report.isEmpty else {
                result.wrappedValue = "Não foi possível preparar o resultado."
                return
            }
            appState.saveTemporaryResultAndOpenViewer(report, prefix: "primos_aleatorios_bits", statusMessage: result)
            return
        }

        let token = PPFNative.Cancellation()
        cancellation = token
        startedAt = Date()
        loading.wrappedValue = true
        result.wrappedValue = "🚀 INICIANDO GERAÇÃO…\n   Buscando primos grandes com algoritmos otimizados."

        let report = await PPFNative.run {
            buildRandomPrimesReport(
                bits: b,
                quantity: q,
                token: token,
                onStatus: { status in
                    Task { @MainActor in
                        if cancellation === token && !token.isRequested {
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
            guard isVisible else { return }
            guard !report.isEmpty else {
                result.wrappedValue = token.isRequested ? "Operação cancelada." : "Não foi possível concluir a geração."
                return
            }
            appState.saveTemporaryResultAndOpenViewer(report, prefix: "primos_aleatorios_bits", statusMessage: result)
        }
    }

    /// Espelha `executarThreadPrimosAleatorios` no Android.
    nonisolated private func buildRandomPrimesReport(
        bits: Int,
        quantity: Int,
        token: PPFNative.Cancellation,
        onStatus: @escaping (String) -> Void
    ) -> String {
        let arquivoPrimos: URL
        do {
            arquivoPrimos = try PPFResultFiles.createTempGiganticPrimesFile(prefix: "primos_gigantes_")
        } catch {
            return "Erro ao criar arquivo temporário: \(error.localizedDescription)"
        }
        defer { try? FileManager.default.removeItem(at: arquivoPrimos) }

        let relatorio = PPFNative.executarJob(
            bits: bits,
            quantidade: quantity,
            threads: 0,
            caminho: arquivoPrimos,
            onStatus: onStatus,
            shouldCancel: { token.isRequested },
            randomCard: true
        )
        guard !relatorio.isEmpty else { return "" }
        return PPFResultFiles.appendFullDecimals(from: arquivoPrimos, to: relatorio)
    }
}
