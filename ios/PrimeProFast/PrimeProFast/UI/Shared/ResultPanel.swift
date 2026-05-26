import SwiftUI

struct ResultPanel: View {
    let text: String
    let isLoading: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if isLoading {
                ProgressView("Processando…")
            }
            ScrollView {
                Text(text.isEmpty ? "Aguardando resultado…" : text)
                    .font(.system(.body, design: .monospaced))
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .textSelection(.enabled)
            }
            .frame(minHeight: 180)
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

struct FeatureScaffold<Content: View>: View {
    let title: String
    let subtitle: String
    @ViewBuilder let content: (Binding<String>, Binding<Bool>) -> Content

    @State private var result = ""
    @State private var loading = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text(title).font(.title2.bold())
                Text(subtitle).font(.subheadline).foregroundStyle(.secondary)
                content($result, $loading)
                ResultPanel(text: result, isLoading: loading)
            }
            .padding()
        }
    }
}
