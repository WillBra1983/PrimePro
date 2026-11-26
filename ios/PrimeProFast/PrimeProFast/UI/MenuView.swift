import SwiftUI
import WebKit

private struct MenuCard: Identifiable {
    var id: String { title }
    let title: String
    let emoji: String
    let description: String
    let destination: () -> AnyView
}

struct MenuView: View {
    @EnvironmentObject private var appState: AppState
    @State private var selectedGuide: MenuCard?

    private var cards: [MenuCard] {
        [
            MenuCard(title: "Primos por Intervalo", emoji: "📊", description: "Liste todos os primos até N.") { AnyView(IntervalPrimesView()) },
            MenuCard(title: "Primos Especiais", emoji: "✨", description: "Gêmeos, Sophie Germain, Fermat e mais.") { AnyView(SpecialPrimesView()) },
            MenuCard(title: "Primos Aleatórios", emoji: "🎲", description: "Gere primos com o tamanho escolhido.") { AnyView(RandomPrimesView()) },
            MenuCard(title: "Conjectura de Legendre", emoji: "📐", description: "Explore a conjectura de Legendre.") { AnyView(LegendreView()) },
            MenuCard(title: "Números de Mersenne", emoji: "🔷", description: "Números da forma 2^p − 1.") { AnyView(MersenneView()) },
            MenuCard(title: "Números Perfeitos", emoji: "💎", description: "Números iguais à soma dos divisores próprios.") { AnyView(PerfectNumbersView()) },
            MenuCard(title: "Segurança Digital", emoji: "🔐", description: "Chaves RSA, hashes e assinaturas.") { AnyView(SecurityView()) },
            MenuCard(title: "Teste de Primalidade", emoji: "🔍", description: "Analise inteiros e expressões.") { AnyView(PrimeTestView()) },
            MenuCard(title: "Estatísticas", emoji: "📈", description: "Conte e compare a distribuição dos primos.") { AnyView(StatisticsView()) },
            MenuCard(title: "Jogo dos Primos", emoji: "🎯", description: "Combine peças e forme somas primas.") { AnyView(PrimeMatchView()) },
            MenuCard(title: "Corrida Prima", emoji: "🏃", description: "Pule compostos e capture primos.") { AnyView(PrimeArcadeView(mode: .runner)) },
            MenuCard(title: "Resgate Espacial", emoji: "🚀", description: "Atire em compostos e resgate primos.") { AnyView(PrimeArcadeView(mode: .shooter)) }
        ]
    }

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                ForEach(cards) { card in
                    HStack(spacing: 4) {
                        NavigationLink {
                            card.destination()
                        } label: {
                            HStack(spacing: 12) {
                                Text(card.emoji).font(.largeTitle)
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(card.title).font(.headline)
                                    Text(card.description).font(.caption).foregroundStyle(.secondary)
                                }
                                Spacer(minLength: 0)
                            }
                            .padding(12)
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                        Button {
                            selectedGuide = card
                        } label: {
                            Image(systemName: "info.circle")
                                .font(.title3)
                                .frame(width: 44, height: 44)
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel("Tutorial de \(card.title)")
                    }
                    .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 8))
                }
            }
            .padding()
        }
        .background(PPFTheme.background(appState.darkMode))
        .sheet(item: $selectedGuide) { card in
            NavigationStack {
                Group {
                    if let sections = GuideContent.catalog[card.title] {
                        GuideContent(sections: sections)
                    } else {
                        Text("Guia indisponível.")
                    }
                }
                .navigationTitle(card.title)
                .navigationBarTitleDisplayMode(.inline)
                .toolbar { Button("Fechar") { selectedGuide = nil } }
            }
        }
    }
}

private struct GuideContent: UIViewRepresentable {
    let sections: [[String]]

    static let catalog: [String: [[String]]] = {
        guard let url = Bundle.main.url(forResource: "primeprofast-tutorials", withExtension: "json"),
              let data = try? Data(contentsOf: url) else { return [:] }
        return (try? JSONDecoder().decode([String: [[String]]].self, from: data)) ?? [:]
    }()

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.defaultWebpagePreferences.allowsContentJavaScript = false
        let view = WKWebView(frame: .zero, configuration: configuration)
        let body = sections.compactMap { section -> String? in
            guard section.count == 2 else { return nil }
            return "<section><h2>\(section[0])</h2>\(section[1])</section>"
        }.joined()
        view.loadHTMLString("""
            <!doctype html><html lang="pt-BR"><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
            :root { color-scheme: light dark; }
            body { margin: 0 auto; max-width: 46rem; padding: 0 18px 24px;
                   font: -apple-system-body; line-height: 1.55; }
            h2 { font-size: 1.15em; margin: 0 0 8px; }
            p { margin: 0 0 10px; }
            section { padding: 18px 0; border-bottom: 1px solid rgba(128,128,128,.35); }
            .exemplo { padding: 8px 12px; border-left: 3px solid #2e7d6f; }
            </style></head><body>\(body)</body></html>
            """, baseURL: nil)
        return view
    }

    func updateUIView(_ view: WKWebView, context: Context) {}
}
