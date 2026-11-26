import SwiftUI

struct IntervalPrimesView: View {
    @State private var maxN = "1000000"
    @State private var textExport: PPFReportExport?
    @EnvironmentObject private var appState: AppState

    private let maxAllowed: Int64 = 50_000_000_000

    var body: some View {
        FeatureScaffold(
            title: "Primos por Intervalo",
            subtitle: "Engine nativa C++/GMP — cálculos no próprio iPhone."
        ) { result, loading in
            TextField("Valor máximo N", text: $maxN)
                .keyboardType(.numberPad)
                .textFieldStyle(.roundedBorder)

            Button("📱 Calcular e Mostrar na Tela") {
                Task { await calcular(modo: .mostrarNaTela, result: result, loading: loading) }
            }
            .buttonStyle(.borderedProminent)
            .tint(PPFTheme.primary)
            .disabled(loading.wrappedValue)

            Button("💾 Calcular e Salvar em TXT") {
                Task { await calcular(modo: .salvarTxt, result: result, loading: loading) }
            }
            .buttonStyle(.bordered)
            .disabled(loading.wrappedValue)
            .sheet(item: $textExport) { item in
                PPFReportExportSheet(export: item) { saved in
                    textExport = nil
                    result.wrappedValue = saved ? "Arquivo TXT salvo." : "Salvamento em TXT cancelado."
                    try? FileManager.default.removeItem(at: item.url.deletingLastPathComponent())
                }
                .interactiveDismissDisabled()
            }
        }
    }

    private enum ModoCalculo {
        case mostrarNaTela
        case salvarTxt
    }

    @MainActor private func calcular(
        modo: ModoCalculo,
        result: Binding<String>,
        loading: Binding<Bool>
    ) async {
        guard !loading.wrappedValue else { return }
        guard let n = Int64(maxN.trimmingCharacters(in: .whitespacesAndNewlines)), n > 0 else {
            result.wrappedValue = "Digite um número maior que 0."
            return
        }
        if n > maxAllowed {
            result.wrappedValue = "Número muito grande. Use até 50.000.000.000."
            return
        }

        loading.wrappedValue = true
        result.wrappedValue = "⏳ Calculando primos até \(n.formatted())…"

        let texto: String = await PPFNative.run {
            PPFNative.calcularPrimos(n)
        }

        switch modo {
        case .mostrarNaTela:
            do {
                let url = try PPFResultFiles.writeIntervalTempViewFile(texto)
                await MainActor.run {
                    loading.wrappedValue = false
                    appState.openExistingResultViewer(url: url, title: "Primos por Intervalo",
                                                      text: texto, statusMessage: result)
                }
            } catch {
                await MainActor.run {
                    loading.wrappedValue = false
                    result.wrappedValue = "Erro ao salvar arquivo temporário: \(error.localizedDescription)"
                }
            }

        case .salvarTxt:
            do {
                let url = try await Task.detached {
                    try PPFResultFiles.writeIntervalTextExport(texto, n: n)
                }.value
                await MainActor.run {
                    loading.wrappedValue = false
                    result.wrappedValue = "Escolha onde salvar o arquivo TXT."
                    textExport = PPFReportExport(url: url, save: true)
                }
            } catch {
                await MainActor.run {
                    loading.wrappedValue = false
                    result.wrappedValue = "Erro ao salvar arquivo TXT: \(error.localizedDescription)"
                }
            }
        }
    }
}
