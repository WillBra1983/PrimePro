import SwiftUI

private struct MenuCard: Identifiable {
    var id: String { title }
    let title: String
    let emoji: String
    let description: String
    let guide: String
    let destination: () -> AnyView
}

struct MenuView: View {
    @EnvironmentObject private var appState: AppState
    @State private var selectedGuide: MenuCard?

    private var cards: [MenuCard] {
        [
            MenuCard(title: "Primos por Intervalo", emoji: "📊", description: "Liste todos os primos até N.",
                     guide: "Informe o limite superior e escolha entre abrir o relatório ou salvar a lista em TXT. Com N = 20, você encontra 2, 3, 5, 7, 11, 13, 17 e 19. Limites maiores exigem mais tempo e espaço.") { AnyView(IntervalPrimesView()) },
            MenuCard(title: "Primos Especiais", emoji: "✨", description: "Gêmeos, Sophie Germain, Fermat e mais.",
                     guide: "Escolha uma família e a quantidade. Gêmeos são pares de primos separados por 2, como 11 e 13. O sorteio respeita o limite informado. Fermat lista os cinco primos conhecidos dessa família.") { AnyView(SpecialPrimesView()) },
            MenuCard(title: "Primos Aleatórios", emoji: "🎲", description: "Gere primos com o tamanho escolhido.",
                     guide: "Informe quantos primos deseja e o tamanho de cada um em bits. Um primo de 4 bits pode ser 11 ou 13. Para valores grandes, o tempo varia porque o app testa candidatos até encontrar primos.") { AnyView(RandomPrimesView()) },
            MenuCard(title: "Conjectura de Legendre", emoji: "📐", description: "Explore a conjectura de Legendre.",
                     guide: "Informe um valor de n, uma faixa ou inicie uma busca contínua. Para n = 3, aparecem 11 e 13 entre 9 e 16. Use Parar e ver resultado para consultar uma busca em andamento. A verificação vale para os valores testados.") { AnyView(LegendreView()) },
            MenuCard(title: "Números de Mersenne", emoji: "🔷", description: "Números da forma 2^p − 1.",
                     guide: "Calcule M(p) para um expoente ou procure primos de Mersenne até um limite de p. Para p = 5, M(p) = 31. Um expoente primo pode gerar um composto, como M(11) = 2047 = 23 × 89.") { AnyView(MersenneView()) },
            MenuCard(title: "Números Perfeitos", emoji: "💎", description: "Números iguais à soma dos divisores próprios.",
                     guide: "Verifique um número, busque perfeitos pares por limite ou por dígitos, ou teste uma amostra de candidatos ímpares. O número 28 é perfeito porque 1 + 2 + 4 + 7 + 14 = 28. Encontrar zero candidatos numa amostra não decide se existem perfeitos ímpares.") { AnyView(PerfectNumbersView()) },
            MenuCard(title: "Segurança Digital", emoji: "🔐", description: "Chaves RSA, hashes e assinaturas.",
                     guide: "Use uma mensagem de exemplo. Compare o hash antes e depois de alterar o texto, cifre com RSA e confira a recuperação dos bytes, ou teste como a assinatura reage a uma mensagem alterada. A chave privada da demonstração fica na sessão.") { AnyView(SecurityView()) },
            MenuCard(title: "Teste de Primalidade", emoji: "🔍", description: "Analise inteiros e expressões.",
                     guide: "Digite um inteiro ou uma expressão, como 2^31-1. O relatório mostra o valor calculado e o resultado do teste. Para compostos, mostra fatores pequenos quando encontrados.") { AnyView(PrimeTestView()) },
            MenuCard(title: "Estatísticas", emoji: "📈", description: "Conte e compare a distribuição dos primos.",
                     guide: "Informe um limite para contar os primos e medir a densidade e as distâncias entre eles. Compare a contagem com a aproximação logarítmica para observar como a distribuição muda.") { AnyView(StatisticsView()) },
            MenuCard(title: "Jogo dos Primos", emoji: "🎯", description: "Combine peças e forme somas primas.",
                     guide: "Arraste uma peça para uma vizinha ou toque em duas peças vizinhas. Forme três ou mais peças iguais em linha. Somas primas pontuam, e as estrelas desbloqueiam novas fases.") { AnyView(PrimeMatchView()) },
            MenuCard(title: "Corrida Prima", emoji: "🏃", description: "Pule compostos e capture primos.",
                     guide: "Toque na pista ou use Pular para saltar. Desvie dos números compostos e encoste nos primos para ganhar pontos extras e melhorar o recorde.") { AnyView(PrimeArcadeView(mode: .runner)) },
            MenuCard(title: "Resgate Espacial", emoji: "🚀", description: "Atire em compostos e resgate primos.",
                     guide: "Arraste com um dedo para desviar e com dois dedos para mover e atirar. Recolha primos com a nave para ganhar pontos e ativar o escudo. A pontuação do voo em andamento é salva quando você sai.") { AnyView(PrimeArcadeView(mode: .shooter)) }
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
                ScrollView {
                    Text(card.guide)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding()
                }
                .navigationTitle(card.title)
                .navigationBarTitleDisplayMode(.inline)
                .toolbar { Button("Fechar") { selectedGuide = nil } }
            }
        }
    }
}
