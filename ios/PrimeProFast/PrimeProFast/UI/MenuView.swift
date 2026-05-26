import SwiftUI

struct MenuCard: Identifiable {
    let id = UUID()
    let title: String
    let emoji: String
    let description: String
    let destination: AnyView
}

struct MenuView: View {
    @EnvironmentObject private var appState: AppState

    private var cards: [MenuCard] {
        [
            MenuCard(title: "Primos por Intervalo", emoji: "📊", description: "Liste todos os primos até N.", destination: AnyView(IntervalPrimesView())),
            MenuCard(title: "Primos Especiais", emoji: "✨", description: "Gêmeos, Sophie Germain, Fermat e mais.", destination: AnyView(SpecialPrimesView())),
            MenuCard(title: "Primos Aleatórios", emoji: "🎲", description: "Primos criptográficos aleatórios.", destination: AnyView(RandomPrimesView())),
            MenuCard(title: "Conjectura de Legendre", emoji: "📐", description: "Explore a conjectura de Legendre.", destination: AnyView(LegendreView())),
            MenuCard(title: "Números de Mersenne", emoji: "🔷", description: "Primos da forma 2^p − 1.", destination: AnyView(MersenneView())),
            MenuCard(title: "Números Perfeitos", emoji: "💎", description: "Números iguais à soma dos divisores.", destination: AnyView(PerfectNumbersView())),
            MenuCard(title: "Segurança Digital", emoji: "🔐", description: "RSA, hash e operações relacionadas.", destination: AnyView(SecurityView())),
            MenuCard(title: "Teste de Primalidade", emoji: "🔍", description: "Análise completa de primalidade.", destination: AnyView(PrimeTestView())),
            MenuCard(title: "Estatísticas", emoji: "📈", description: "Resumo de uso e desempenho.", destination: AnyView(StatisticsView()))
        ]
    }

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                ForEach(cards) { card in
                    NavigationLink {
                        card.destination
                    } label: {
                        HStack(spacing: 12) {
                            Text(card.emoji).font(.largeTitle)
                            VStack(alignment: .leading, spacing: 4) {
                                Text(card.title).font(.headline)
                                Text(card.description).font(.caption).foregroundStyle(.secondary)
                            }
                            Spacer()
                            Image(systemName: "chevron.right")
                                .foregroundStyle(.tertiary)
                        }
                        .padding()
                        .background(PPFTheme.cardBg)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding()
        }
        .background(PPFTheme.background(appState.darkMode))
    }
}
