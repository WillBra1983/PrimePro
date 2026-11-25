import SwiftUI
import AVFoundation

struct PrimeMatchTile: Identifiable, Equatable {
    let id = UUID()
    var value: Int
    var colorIndex: Int
}

private struct PrimeMatchGroup {
    let cells: [(row: Int, col: Int)]
    let sum: Int
    let blackCount: Int
}

private enum PrimeMatchPower: String, CaseIterable {
    case hammer = "Martelo"
    case plus1 = "+1 na soma"
    case plus2 = "+2 na soma"
    case plus3 = "+3 na soma"
    case shuffle = "Reorganizar tabuleiro"
    case swap = "Troca livre"
    case copyColor = "Copiar cor"
    case polishUp = "Lapidar +1"
    case polishDown = "Lapidar −1"
    case rowRay = "Raio da linha"
    case columnRay = "Raio da coluna"
    case hint = "Dica prima"
    case multiplier = "Combo ×2"
}

private final class PrimeMatchSoundPlayer: ObservableObject {
    private var players: [String: AVAudioPlayer] = [:]

    init() {
        [
            "ppf_pm_tap",
            "ppf_pm_prime",
            "ppf_pm_composite",
            "ppf_pm_combo",
            "ppf_pm_power",
            "ppf_pm_black",
            "ppf_pm_victory"
        ].forEach(load)
    }

    func play(_ name: String, enabled: Bool) {
        guard enabled, let player = players[name] else { return }
        player.currentTime = 0
        player.volume = 0.58
        player.play()
    }

    private func load(_ name: String) {
        let url = Bundle.main.url(forResource: name, withExtension: "wav")
            ?? Bundle.main.url(forResource: name, withExtension: "wav", subdirectory: "Sounds")
            ?? Bundle.main.url(forResource: name, withExtension: "wav", subdirectory: "Resources/Sounds")
        guard let url else { return }
        do {
            let player = try AVAudioPlayer(contentsOf: url)
            player.prepareToPlay()
            players[name] = player
        } catch {
            players[name] = nil
        }
    }
}

struct PrimeMatchView: View {
    private let rows = 6
    private let cols = 6
    private let minChain = 3
    private let levelCount = 24
    private var target: Int { 760 + level * 140 }
    private var unlockedLevel: Int {
        guard let index = levelStars.lastIndex(where: { $0 > 0 }) else { return 1 }
        return min(levelCount, index + 2)
    }
    private let blackColorIndex = 99
    private let colors: [Color] = [
        Color(red: 0.05, green: 0.34, blue: 0.79),
        Color(red: 0.02, green: 0.52, blue: 0.29),
        Color(red: 0.73, green: 0.48, blue: 0.03),
        Color(red: 0.50, green: 0.22, blue: 0.77),
        Color(red: 0.79, green: 0.17, blue: 0.29)
    ]
    private let colorNames = ["Azul", "Verde", "Dourada", "Roxa", "Vermelha"]
    private let colorSymbols = ["circle.fill", "triangle.fill", "diamond.fill", "square.fill", "hexagon.fill"]

    @State private var board: [[PrimeMatchTile]] = []
    @State private var selection: [(row: Int, col: Int)] = []
    @State private var dragStart: (row: Int, col: Int)?
    @State private var dragOffset = CGSize.zero
    @State private var tapSource: (row: Int, col: Int)?
    @State private var showHistory = false
    @State private var showLevels = false
    @State private var level = 1
    @State private var score = 0
    @State private var moves = 20
    @State private var status = ""
    @State private var history: [String] = []
    @State private var historyCounter = 0
    @State private var active = true
    @State private var rewardGranted = false
    @State private var tilePower: PrimeMatchPower?
    @State private var pendingAddition: PrimeMatchPower?
    @State private var comboMultiplier = false
    @State private var resolving = false
    @State private var effectCells: Set<Int> = []
    @State private var effectText: String?
    @StateObject private var soundPlayer = PrimeMatchSoundPlayer()
    @AppStorage("prime_match_sound_enabled") private var soundEnabled = true
    @AppStorage("prime_match_best_score") private var bestScore = 0
    @AppStorage("prime_match_wins") private var wins = 0
    @AppStorage("prime_match_level_stars") private var levelStars = Data()
    @AppStorage("prime_match_hammer") private var hammerCount = 0
    @AppStorage("prime_match_plus_1") private var plus1Count = 0
    @AppStorage("prime_match_plus_2") private var plus2Count = 0
    @AppStorage("prime_match_plus_3") private var plus3Count = 0
    @AppStorage("prime_match_reshuffle") private var reshuffleCount = 0
    @AppStorage("prime_match_free_swap") private var freeSwapCount = 0
    @AppStorage("prime_match_color_copy") private var colorCopyCount = 0
    @AppStorage("prime_match_polish_up") private var polishUpCount = 0
    @AppStorage("prime_match_polish_down") private var polishDownCount = 0
    @AppStorage("prime_match_row_ray") private var rowRayCount = 0
    @AppStorage("prime_match_col_ray") private var colRayCount = 0
    @AppStorage("prime_match_hint") private var hintCount = 0
    @AppStorage("prime_match_combo_mult") private var comboMultCount = 0

    var body: some View {
        GeometryReader { geometry in
            let side = max(1, min(geometry.size.width - 12, geometry.size.height - 142, 540))
            VStack(spacing: 8) {
                HStack(spacing: 10) {
                    metric(title: "Valor", value: "\(score) / \(target)")
                    metric(title: "Jogadas", value: "\(max(0, moves))")
                }
                .padding(.horizontal, 10)

                if board.count == rows && board.allSatisfy({ $0.count == cols }) {
                    gameBoard(side: side)
                }

                HStack {
                    Button {
                        soundEnabled.toggle()
                        soundPlayer.play("ppf_pm_tap", enabled: soundEnabled)
                    } label: {
                        Image(systemName: soundEnabled ? "speaker.wave.2.fill" : "speaker.slash.fill")
                            .frame(minWidth: 36, minHeight: 32)
                    }
                    .accessibilityLabel(soundEnabled ? "Desligar som" : "Ligar som")
                    Button {
                        showHistory = true
                    } label: {
                        Image(systemName: "clock.arrow.circlepath")
                            .frame(minWidth: 36, minHeight: 32)
                    }
                    .accessibilityLabel("Histórico")
                    powerMenu
                    Spacer()
                    Button {
                        cancelDrag()
                        showLevels = true
                    } label: {
                        Label("Fases", systemImage: "square.grid.2x2")
                    }
                    .disabled(resolving)
                    Button("Rejogar") { reset() }
                        .disabled(resolving)
                }
                .buttonStyle(.bordered)
                .padding(.horizontal, 10)

                ScrollView {
                    VStack(alignment: .leading, spacing: 8) {
                        Text(status)
                            .font(.subheadline.weight(.semibold))
                        if totalSavedPowers > 0 {
                            Text(inventoryText())
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 12)
                }
            }
        }
        .sheet(isPresented: $showHistory) {
            NavigationStack {
                ScrollView {
                    Text(historyText()).frame(maxWidth: .infinity, alignment: .leading).padding()
                }
                .navigationTitle("Histórico")
                .toolbar { Button("Fechar") { showHistory = false } }
            }
        }
        .sheet(isPresented: $showLevels) { levelPicker }
        .onAppear {
            if board.isEmpty {
                reset()
            }
        }
        .navigationTitle("Prime Match · Fase \(level)")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var powerMenu: some View {
        Menu {
            ForEach(PrimeMatchPower.allCases, id: \.self) { power in
                let count = stock(for: power).wrappedValue
                if count > 0 {
                    Button("\(power.rawValue) · \(count)") { arm(power) }
                }
            }
            if tilePower != nil || pendingAddition != nil || comboMultiplier {
                Button("Cancelar poderes armados") { clearPowers() }
            }
        } label: {
            Image(systemName: "bolt.fill")
                .frame(minWidth: 32, minHeight: 32)
                .foregroundStyle(tilePower != nil || pendingAddition != nil || comboMultiplier ? .orange : .primary)
        }
        .accessibilityLabel("Poderes")
        .disabled(!active || resolving || totalSavedPowers == 0)
    }

    private func stock(for power: PrimeMatchPower) -> Binding<Int> {
        switch power {
        case .hammer: return $hammerCount
        case .plus1: return $plus1Count
        case .plus2: return $plus2Count
        case .plus3: return $plus3Count
        case .shuffle: return $reshuffleCount
        case .swap: return $freeSwapCount
        case .copyColor: return $colorCopyCount
        case .polishUp: return $polishUpCount
        case .polishDown: return $polishDownCount
        case .rowRay: return $rowRayCount
        case .columnRay: return $colRayCount
        case .hint: return $hintCount
        case .multiplier: return $comboMultCount
        }
    }

    private func clearPowers() {
        tilePower = nil
        pendingAddition = nil
        comboMultiplier = false
        cancelDrag()
    }

    private func arm(_ power: PrimeMatchPower) {
        guard active, !resolving, stock(for: power).wrappedValue > 0 else { return }
        cancelDrag()
        tilePower = nil
        switch power {
        case .plus1, .plus2, .plus3:
            pendingAddition = pendingAddition == power ? nil : power
            status = pendingAddition == nil ? "Ajuste cancelado." : "Ajuste armado para a próxima combinação."
        case .multiplier:
            comboMultiplier.toggle()
            status = comboMultiplier ? "O próximo combo primo em cascata vale o dobro." : "Multiplicador cancelado."
        case .shuffle:
            reshuffleCount -= 1
            rearrangeBoard()
        case .hint:
            if let (start, end) = playableSwap(primeOnly: true) {
                hintCount -= 1
                selection = [start, end]
                status = "Troque as duas peças destacadas."
            } else {
                status = "Nenhuma troca com soma prima foi encontrada."
            }
        default:
            tilePower = power
            status = power == .swap || power == .copyColor ? "Escolha a primeira peça." : "Escolha a peça para aplicar \(power.rawValue)."
        }
    }

    private var levelPicker: some View {
        NavigationStack {
            ScrollView {
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 88), spacing: 8)], spacing: 8) {
                    ForEach(1...levelCount, id: \.self) { number in
                        let best = number <= levelStars.count ? Int(levelStars[number - 1]) : 0
                        Button {
                            level = number
                            reset()
                            showLevels = false
                        } label: {
                            VStack(spacing: 4) {
                                Text("\(number)").font(.headline)
                                if number > unlockedLevel {
                                    Image(systemName: "lock.fill")
                                } else {
                                    Text(starsText(for: best))
                                }
                            }
                            .frame(maxWidth: .infinity, minHeight: 48)
                        }
                        .buttonStyle(.bordered)
                        .disabled(number > unlockedLevel)
                        .accessibilityLabel("Fase \(number), \(best) de 3 estrelas")
                    }
                }
                .padding()
            }
            .navigationTitle("Fases")
            .toolbar { Button("Fechar") { showLevels = false } }
        }
    }

    private func gameBoard(side: CGFloat) -> some View {
        let inset: CGFloat = 4
        let pitch = max(1, (side - 2 * inset) / CGFloat(cols))
        return ZStack(alignment: .topLeading) {
            ForEach(0..<(rows * cols), id: \.self) { idx in
                let row = idx / cols
                let col = idx % cols
                let moving = dragStart?.row == row && dragStart?.col == col
                tileView(row: row, col: col, size: max(1, pitch - 4))
                    .position(x: inset + (CGFloat(col) + 0.5) * pitch,
                              y: inset + (CGFloat(row) + 0.5) * pitch)
                    .offset(moving ? dragOffset : .zero)
                    .zIndex(moving ? 1 : 0)
            }
        }
        .frame(width: side, height: side)
        .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 8))
        .overlay(alignment: .bottom) {
            if let effectText {
                Text(effectText)
                    .font(.caption.bold().monospacedDigit())
                    .multilineTextAlignment(.center)
                    .padding(8)
                    .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 8))
                    .padding(4)
                    .allowsHitTesting(false)
            }
        }
        .overlay {
            if !active && score >= target {
                ZStack {
                    ForEach(0..<12) { ray in
                        Capsule()
                            .fill(.yellow.opacity(0.45))
                            .frame(width: side * 0.025, height: side * 0.25)
                            .offset(y: -side * 0.35)
                            .rotationEffect(.degrees(Double(ray) * 30))
                    }
                    VStack(spacing: 4) {
                        Text(starsText(for: 3))
                            .font(.title.bold())
                            .foregroundStyle(.yellow)
                        Text("Fase concluída")
                            .font(.headline)
                    }
                    .padding(14)
                    .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 8))
                }
                .frame(width: side, height: side)
                .transition(.scale.combined(with: .opacity))
                .allowsHitTesting(false)
            }
        }
        .contentShape(Rectangle())
        .gesture(
            DragGesture(minimumDistance: 7)
                .onChanged { value in
                    guard active, let start = cell(at: value.startLocation, inset: inset, pitch: pitch) else { return }
                    updateDrag(row: start.row, col: start.col, translation: value.translation, pitch: pitch)
                }
                .onEnded { value in
                    guard let start = cell(at: value.startLocation, inset: inset, pitch: pitch) else {
                        cancelDrag()
                        return
                    }
                    finishDrag(row: start.row, col: start.col, translation: value.translation, pitch: pitch)
                }
        )
    }

    private func cell(at location: CGPoint, inset: CGFloat, pitch: CGFloat) -> (row: Int, col: Int)? {
        let row = Int(floor((location.y - inset) / pitch))
        let col = Int(floor((location.x - inset) / pitch))
        guard (0..<rows).contains(row), (0..<cols).contains(col) else { return nil }
        return (row, col)
    }

    private func metric(title: String, value: String) -> some View {
        VStack(spacing: 3) {
            Text(title).font(.caption).foregroundStyle(.secondary)
            Text(value).font(.headline)
        }
        .frame(maxWidth: .infinity)
        .padding(10)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    private func historyText() -> String {
        guard !history.isEmpty else {
            return "Histórico da fase\nAinda sem cálculos."
        }
        return history.joined(separator: "\n\n")
    }

    private var totalSavedPowers: Int {
        [
            hammerCount,
            plus1Count,
            plus2Count,
            plus3Count,
            reshuffleCount,
            freeSwapCount,
            colorCopyCount,
            polishUpCount,
            polishDownCount,
            rowRayCount,
            colRayCount,
            hintCount,
            comboMultCount
        ].reduce(0, +)
    }

    private func inventoryText() -> String {
        var items: [String] = []
        appendInventory(&items, "Martelo", hammerCount)
        appendInventory(&items, "+1", plus1Count)
        appendInventory(&items, "+2", plus2Count)
        appendInventory(&items, "+3", plus3Count)
        appendInventory(&items, "Reorganizar", reshuffleCount)
        appendInventory(&items, "Troca livre", freeSwapCount)
        appendInventory(&items, "Coringa", colorCopyCount)
        appendInventory(&items, "Lapidar +1", polishUpCount)
        appendInventory(&items, "Lapidar -1", polishDownCount)
        appendInventory(&items, "Raio linha", rowRayCount)
        appendInventory(&items, "Raio coluna", colRayCount)
        appendInventory(&items, "Dica", hintCount)
        appendInventory(&items, "Combo x2", comboMultCount)
        return "Carteira permanente: " + items.joined(separator: " • ")
    }

    private func appendInventory(_ items: inout [String], _ name: String, _ quantity: Int) {
        guard quantity > 0 else { return }
        items.append("\(name) x\(quantity)")
    }

    private func isBlack(_ colorIndex: Int) -> Bool {
        colorIndex == blackColorIndex
    }

    private func colorsConnect(_ first: Int, _ second: Int) -> Bool {
        first == second || isBlack(first) || isBlack(second)
    }

    private func tileView(row: Int, col: Int, size: CGFloat) -> some View {
        let tile = board[row][col]
        let selected = selection.contains { $0.row == row && $0.col == col }
        let black = isBlack(tile.colorIndex)
        let exploding = effectCells.contains(row * cols + col)
        let index = max(0, tile.colorIndex) % colors.count
        let color = black ? Color(red: 0.04, green: 0.05, blue: 0.07) : colors[index]
        return VStack(spacing: 0) {
            Image(systemName: black ? "star.fill" : colorSymbols[index])
                .font(.system(size: max(4, size * 0.16), weight: .bold))
            Text(tile.value == 0 ? "" : "\(tile.value)")
                .font(.system(size: max(6, size * 0.36), weight: .heavy, design: .rounded))
                .lineLimit(1)
                .minimumScaleFactor(0.48)
                .padding(.horizontal, 3)
        }
            .foregroundStyle(black ? Color(red: 0.97, green: 0.86, blue: 0.57) : .white)
            .shadow(color: .black.opacity(0.62), radius: 1.5, x: 0, y: 1)
            .frame(width: size, height: size)
            .background(glassMarble(color: color, selected: selected))
            .overlay {
                if exploding {
                    ZStack(alignment: .topTrailing) {
                        Circle().strokeBorder(.yellow, lineWidth: 4)
                        Image(systemName: "bolt.circle.fill")
                            .foregroundStyle(.yellow, .black)
                            .font(.system(size: max(10, size * 0.29)))
                    }
                }
            }
            .scaleEffect(selected ? 1.05 : 1)
            .contentShape(Rectangle())
            .onTapGesture { tapTile(row: row, col: col) }
            .accessibilityLabel("\(tile.value), \(black ? "especial" : colorNames[index])")
            .accessibilityAddTraits(.isButton)
            .accessibilityAction { tapTile(row: row, col: col) }
    }

    private func glassMarble(color: Color, selected: Bool) -> some View {
        GeometryReader { proxy in
            let size = min(proxy.size.width, proxy.size.height)
            ZStack(alignment: .topLeading) {
                Circle()
                    .fill(.black.opacity(0.20))
                    .offset(y: size * 0.07)
                    .blur(radius: 2)

                Circle()
                    .fill(
                        RadialGradient(
                            colors: [
                                color,
                                color,
                                color,
                                .black.opacity(0.45)
                            ],
                            center: UnitPoint(x: 0.34, y: 0.24),
                            startRadius: 1,
                            endRadius: size * 0.72
                        )
                    )
                    .overlay(
                        Circle()
                            .stroke(selected ? .white : .white.opacity(0.36), lineWidth: selected ? 3 : 1)
                    )
                    .shadow(color: .black.opacity(selected ? 0.32 : 0.22), radius: selected ? 8 : 4, x: 0, y: selected ? 5 : 3)

                Circle()
                    .fill(
                        RadialGradient(
                            colors: [.white.opacity(0.86), .white.opacity(0.16), .clear],
                            center: .center,
                            startRadius: 1,
                            endRadius: size * 0.24
                        )
                    )
                    .frame(width: size * 0.42, height: size * 0.32)
                    .offset(x: size * 0.14, y: size * 0.10)

                Capsule()
                    .fill(
                        LinearGradient(
                            colors: [.white.opacity(0.60), .white.opacity(0.04)],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    )
                    .frame(width: size * 0.20, height: size * 0.08)
                    .rotationEffect(.degrees(-13))
                    .offset(x: size * 0.56, y: size * 0.12)
            }
        }
        .aspectRatio(1, contentMode: .fit)
    }

    private func reset() {
        guard !resolving else { return }
        clearPowers()
        board = (0..<rows).map { _ in
            (0..<cols).map { _ in PrimeMatchTile(value: 0, colorIndex: 0) }
        }
        for row in 0..<rows {
            for col in 0..<cols {
                _ = randomTileAvoidingMatch(row: row, col: col)
            }
        }
        selection.removeAll()
        history.removeAll()
        historyCounter = 0
        cancelDrag()
        score = 0
        moves = max(16, 22 - min(6, level / 4))
        active = true
        rewardGranted = false
        status = "Meta: acumule \(target) pontos. Tente criar primos cada vez maiores."
        ensurePlayableBoard()
    }

    private func randomTile() -> PrimeMatchTile {
        PrimeMatchTile(value: Int.random(in: 1...19), colorIndex: Int.random(in: 0..<colors.count))
    }

    private func randomTileAvoidingMatch(row: Int, col: Int) -> PrimeMatchTile {
        var chosen = randomTile()
        for _ in 0..<40 {
            let candidate = randomTile()
            board[row][col] = candidate
            chosen = candidate
            if !formsMatch(row: row, col: col) {
                return candidate
            }
        }
        board[row][col] = chosen
        return chosen
    }

    private func formsMatch(row: Int, col: Int) -> Bool {
        guard row >= 0, row < rows, col >= 0, col < cols else { return false }
        let tile = board[row][col]
        guard tile.value != 0 else { return false }
        let horizontal = 1
            + countColor(row: row, col: col, dr: 0, dc: -1, colorIndex: tile.colorIndex)
            + countColor(row: row, col: col, dr: 0, dc: 1, colorIndex: tile.colorIndex)
        let vertical = 1
            + countColor(row: row, col: col, dr: -1, dc: 0, colorIndex: tile.colorIndex)
            + countColor(row: row, col: col, dr: 1, dc: 0, colorIndex: tile.colorIndex)
        return horizontal >= minChain || vertical >= minChain
    }

    private func countColor(row: Int, col: Int, dr: Int, dc: Int, colorIndex: Int) -> Int {
        var total = 0
        var r = row + dr
        var c = col + dc
        while r >= 0,
              r < rows,
              c >= 0,
              c < cols,
              board[r][c].value != 0,
              board[r][c].colorIndex == colorIndex {
            total += 1
            r += dr
            c += dc
        }
        return total
    }

    private func updateDrag(row: Int, col: Int, translation: CGSize, pitch: CGFloat) {
        guard active, !resolving, tilePower == nil else { return }
        if dragStart == nil {
            dragStart = (row, col)
            tapSource = nil
            selection = [(row, col)]
            soundPlayer.play("ppf_pm_tap", enabled: soundEnabled)
            status = "Arraste para uma bolinha vizinha."
        }
        guard let start = dragStart else { return }
        let target = targetForDrag(row: start.row, col: start.col, translation: translation, threshold: pitch * 0.30)
        selection = [start] + (target.map { [$0] } ?? [])
        if abs(translation.width) > abs(translation.height) {
            dragOffset = CGSize(width: max(-pitch, min(pitch, translation.width)), height: 0)
        } else {
            dragOffset = CGSize(width: 0, height: max(-pitch, min(pitch, translation.height)))
        }
    }

    private func finishDrag(row: Int, col: Int, translation: CGSize, pitch: CGFloat) {
        guard !resolving, tilePower == nil else { return }
        cancelDrag()
        guard active, let target = targetForDrag(row: row, col: col, translation: translation,
                                                  threshold: pitch * 0.30) else { return }
        playSwap((row, col), target)
    }

    private func tapTile(row: Int, col: Int) {
        guard active, !resolving, dragStart == nil else { return }
        let tile = (row: row, col: col)
        if let tilePower {
            apply(tilePower, to: tile)
            return
        }
        if let start = tapSource, isNeighbor(start, tile) {
            cancelDrag()
            playSwap(start, tile)
        } else if tapSource?.row == row && tapSource?.col == col {
            cancelDrag()
        } else {
            tapSource = tile
            selection = [tile]
        }
    }

    private func apply(_ power: PrimeMatchPower, to tile: (row: Int, col: Int)) {
        let count = stock(for: power)
        guard count.wrappedValue > 0 else { return }
        var removed: Set<Int> = []
        switch power {
        case .hammer:
            removed.insert(tile.row * cols + tile.col)
        case .rowRay:
            removed = Set((0..<cols).map { tile.row * cols + $0 })
        case .columnRay:
            removed = Set((0..<rows).map { $0 * cols + tile.col })
        case .polishUp, .polishDown:
            let newValue = board[tile.row][tile.col].value + (power == .polishUp ? 1 : -1)
            guard (1...19).contains(newValue) else {
                status = "Escolha uma peça cujo valor possa ficar entre 1 e 19."
                return
            }
            board[tile.row][tile.col].value = newValue
        case .swap, .copyColor:
            guard let first = tapSource else {
                tapSource = tile
                selection = [tile]
                status = "Escolha a segunda peça."
                return
            }
            guard first.row != tile.row || first.col != tile.col else { return }
            if power == .swap {
                swapTiles(first, tile)
            } else {
                board[first.row][first.col].colorIndex = board[tile.row][tile.col].colorIndex
            }
        default:
            return
        }
        count.wrappedValue -= 1
        let first = tapSource
        tilePower = nil
        cancelDrag()
        soundPlayer.play("ppf_pm_power", enabled: soundEnabled)
        animateResolution(anchor: tile, fallback: first, startingRemoval: removed)
    }

    private func playSwap(_ start: (row: Int, col: Int), _ target: (row: Int, col: Int)) {
        swapTiles(start, target)

        if findMatchGroups().isEmpty {
            swapTiles(start, target)
            soundPlayer.play("ppf_pm_composite", enabled: soundEnabled)
            status = "Essa troca não formou trio. Tente outra combinação."
            ensurePlayableBoard()
        } else {
            moves -= 1
            animateResolution(anchor: target, fallback: start)
        }
    }

    private func cancelDrag() {
        dragStart = nil
        dragOffset = .zero
        tapSource = nil
        selection.removeAll()
    }

    private func targetForDrag(row: Int, col: Int, translation: CGSize, threshold: CGFloat) -> (row: Int, col: Int)? {
        let dx = translation.width
        let dy = translation.height
        guard max(abs(dx), abs(dy)) >= threshold else { return nil }

        var target = (row: row, col: col)
        if abs(dx) > abs(dy) {
            target.col += dx > 0 ? 1 : -1
        } else {
            target.row += dy > 0 ? 1 : -1
        }

        guard target.row >= 0, target.row < rows, target.col >= 0, target.col < cols else {
            return nil
        }
        return target
    }

    private func isNeighbor(_ first: (row: Int, col: Int), _ second: (row: Int, col: Int)) -> Bool {
        abs(first.row - second.row) + abs(first.col - second.col) == 1
    }

    private func swapTiles(_ first: (row: Int, col: Int), _ second: (row: Int, col: Int)) {
        let tile = board[first.row][first.col]
        board[first.row][first.col] = board[second.row][second.col]
        board[second.row][second.col] = tile
    }

    private func hasPlayableSwap() -> Bool {
        playableSwap(primeOnly: false) != nil
    }

    private func playableSwap(primeOnly: Bool) -> ((row: Int, col: Int), (row: Int, col: Int))? {
        for row in 0..<rows {
            for col in 0..<cols {
                let start = (row: row, col: col)
                for end in [(row: row + 1, col: col), (row: row, col: col + 1)]
                    where end.row < rows && end.col < cols {
                    let first = board[start.row][start.col].colorIndex
                    let second = board[end.row][end.col].colorIndex
                    guard first != second else { continue }
                    swapTiles(start, end)
                    let matched = findMatchGroups().contains { group in
                        (!primeOnly || PPFMath.isPrime(group.sum)) && group.cells.contains { cell in
                            (cell.row == start.row && cell.col == start.col) ||
                            (cell.row == end.row && cell.col == end.col)
                        }
                    }
                    swapTiles(start, end)
                    if matched { return (start, end) }
                }
            }
        }
        return nil
    }

    private func ensurePlayableBoard() {
        if !findMatchGroups().isEmpty || !hasPlayableSwap() { rearrangeBoard() }
    }

    private func rearrangeBoard() {
            let pieces = board.flatMap { $0 }
            for _ in 0..<60 {
                let shuffled = pieces.shuffled()
                board = (0..<rows).map { row in
                    Array(shuffled[(row * cols)..<((row + 1) * cols)])
                }
                if findMatchGroups().isEmpty, hasPlayableSwap() {
                    status = "Tabuleiro embaralhado. Nenhuma jogada foi descontada."
                    return
                }
            }
            // A-B-A over C-A-D guarantees a vertical swap that creates a horizontal trio.
            let palette = colors.indices.shuffled()
            board = (0..<rows).map { row in
                (0..<cols).map { col in
                    let pattern = (row == 0 && col == 2) || (row == 1 && col == 1) ? 0 : (row + col) % palette.count
                    return PrimeMatchTile(value: Int.random(in: 1...19), colorIndex: palette[pattern])
                }
            }
            status = "Tabuleiro renovado. Nenhuma jogada foi descontada."
    }

    private func animateResolution(anchor: (row: Int, col: Int)?,
                                   fallback: (row: Int, col: Int)?,
                                   startingRemoval: Set<Int> = []) {
        resolving = true
        Task { @MainActor in
            defer {
                resolving = false
                effectText = nil
                effectCells = []
            }
            if !startingRemoval.isEmpty {
                effectCells = startingRemoval
                effectText = "Poder aplicado"
                try? await Task.sleep(nanoseconds: 450_000_000)
                remove(startingRemoval)
                compactBoard(fixed: emptyMask())
            }
            let initialScore = score
            for chain in 0..<12 {
                let groups = findMatchGroups()
                guard !groups.isEmpty else { break }
                let matched = Set(groups.flatMap { $0.cells.map { $0.row * cols + $0.col } })
                var removed = matched
                var countedExtras: Set<Int> = []
                var creations: [Int: Int] = [:]
                var delta = 0
                var previews: [(text: String, cells: Set<Int>, prime: Bool)] = []
                let adjustmentGroup = groups.firstIndex { group in
                    [anchor, fallback].compactMap { $0 }.contains { position in
                        group.cells.contains { $0.row == position.row && $0.col == position.col }
                    }
                } ?? 0

                for (index, group) in groups.enumerated() {
                    var adjustment = 0
                    if chain == 0, index == adjustmentGroup, let power = pendingAddition {
                        adjustment = power == .plus1 ? 1 : (power == .plus2 ? 2 : 3)
                        stock(for: power).wrappedValue -= 1
                        pendingAddition = nil
                    }
                    let sum = group.sum + adjustment
                    let prime = PPFMath.isPrime(sum)
                    var points = prime ? sum : 0
                    var highlighted = Set(group.cells.map { $0.row * cols + $0.col })
                    if prime {
                        let blast = blastTiles(for: group)
                        removed.formUnion(blast)
                        highlighted.formUnion(blast)
                        let extras = blast.subtracting(matched).subtracting(countedExtras)
                        let extraValue = extras.reduce(0) { $0 + board[$1 / cols][$1 % cols].value }
                        if group.blackCount == 2 || PPFMath.isPrime(sum + extraValue) {
                            points += extraValue
                            countedExtras.formUnion(extras)
                        }
                        if chain > 0, comboMultiplier, comboMultCount > 0 {
                            points *= 2
                            comboMultCount -= 1
                            comboMultiplier = false
                        }
                        let position = anchorFor(group: group, preferred: chain == 0 ? anchor : nil,
                                                 fallback: chain == 0 ? fallback : nil)
                        let key = position.row * cols + position.col
                        creations[key] = max(creations[key] ?? 0, sum)
                    }
                    let numbers = group.cells.map { String(board[$0.row][$0.col].value) }.joined(separator: " + ")
                    let equation = numbers + (adjustment > 0 ? " + \(adjustment)" : "") + " = \(sum)"
                    let detail = equation + (prime ? " · +\(points) pontos" : " · composto")
                    historyCounter += 1
                    history.insert("\(historyCounter). \(detail)", at: 0)
                    if history.count > 50 { history.removeLast() }
                    previews.append((detail, highlighted, prime))
                    delta += points
                }
                for (index, preview) in previews.enumerated() {
                    effectCells = preview.cells
                    effectText = previews.count > 1
                        ? "\(index + 1)/\(previews.count) · \(preview.text)" : preview.text
                    soundPlayer.play(preview.prime ? (chain > 0 ? "ppf_pm_combo" : "ppf_pm_prime") : "ppf_pm_composite", enabled: soundEnabled)
                    try? await Task.sleep(nanoseconds: 1_400_000_000)
                }
                var fixed = emptyMask()
                withAnimation(.easeInOut(duration: 0.22)) {
                    remove(removed)
                    for (key, value) in creations {
                        board[key / cols][key % cols] = PrimeMatchTile(value: value, colorIndex: blackColorIndex)
                        fixed[key / cols][key % cols] = true
                    }
                    compactBoard(fixed: fixed)
                    score += delta
                    effectCells = []
                }
                try? await Task.sleep(nanoseconds: 220_000_000)
            }
            status = score > initialScore ? "+\(score - initialScore) pontos." : "Jogada concluída."
            checkEnd()
            if active { ensurePlayableBoard() }
        }
    }

    private func emptyMask() -> [[Bool]] {
        Array(repeating: Array(repeating: false, count: cols), count: rows)
    }

    private func remove(_ cells: Set<Int>) {
        for cell in cells { board[cell / cols][cell % cols].value = 0 }
    }

    private func blastTiles(for group: PrimeMatchGroup) -> Set<Int> {
        guard group.blackCount > 0 else { return [] }
        var tiles: Set<Int> = []
        for row in 0..<rows {
            for col in 0..<cols {
                let adjacentToBlack = group.cells.contains { cell in
                    isBlack(board[cell.row][cell.col].colorIndex) &&
                    abs(cell.row - row) <= 1 && abs(cell.col - col) <= 1
                }
                if group.blackCount >= 3 ||
                    (group.blackCount == 2 && PPFMath.isPrime(board[row][col].value)) ||
                    (group.blackCount == 1 && adjacentToBlack) {
                    tiles.insert(row * cols + col)
                }
            }
        }
        return tiles
    }

    private func findMatchGroups() -> [PrimeMatchGroup] {
        var groups: [PrimeMatchGroup] = []
        appendLinearMatchGroups(to: &groups, horizontal: true)
        appendLinearMatchGroups(to: &groups, horizontal: false)
        return groups
    }

    private func appendLinearMatchGroups(to groups: inout [PrimeMatchGroup], horizontal: Bool) {
        let lineCount = horizontal ? rows : cols
        let length = horizontal ? cols : rows
        for line in 0..<lineCount {
            var start = 0
            while start < length {
                while start < length, !linearPositionOccupied(horizontal: horizontal, line: line, position: start) {
                    start += 1
                }
                guard start < length else { break }

                var end = start
                var baseColor: Int?
                while end < length, linearPositionOccupied(horizontal: horizontal, line: line, position: end) {
                    let cell = linearCell(horizontal: horizontal, line: line, position: end)
                    let colorIndex = board[cell.row][cell.col].colorIndex
                    if !isBlack(colorIndex) {
                        if baseColor == nil {
                            baseColor = colorIndex
                        } else if baseColor! != colorIndex {
                            break
                        }
                    }
                    end += 1
                }

                if end - start >= minChain {
                    groups.append(makeLinearGroup(
                        horizontal: horizontal,
                        line: line,
                        start: start,
                        end: end - 1
                    ))
                    start = end
                } else {
                    // A trailing wild tile can start a match of the next color.
                    start += 1
                }
            }
        }
    }

    private func linearPositionOccupied(horizontal: Bool, line: Int, position: Int) -> Bool {
        let cell = linearCell(horizontal: horizontal, line: line, position: position)
        return board[cell.row][cell.col].value != 0
    }

    private func linearCell(horizontal: Bool, line: Int, position: Int) -> (row: Int, col: Int) {
        horizontal ? (row: line, col: position) : (row: position, col: line)
    }

    private func makeLinearGroup(
        horizontal: Bool,
        line: Int,
        start: Int,
        end: Int
    ) -> PrimeMatchGroup {
        var cells: [(row: Int, col: Int)] = []
        var sum = 0
        var blackCount = 0

        for position in start...end {
            let cell = linearCell(horizontal: horizontal, line: line, position: position)
            cells.append(cell)
            sum += board[cell.row][cell.col].value
            if isBlack(board[cell.row][cell.col].colorIndex) {
                blackCount += 1
            }
        }

        return PrimeMatchGroup(cells: cells, sum: sum, blackCount: blackCount)
    }

    private func anchorFor(
        group: PrimeMatchGroup,
        preferred: (row: Int, col: Int)?,
        fallback: (row: Int, col: Int)?
    ) -> (row: Int, col: Int) {
        if let preferred, group.cells.contains(where: { $0.row == preferred.row && $0.col == preferred.col }) {
            return preferred
        }
        if let fallback, group.cells.contains(where: { $0.row == fallback.row && $0.col == fallback.col }) {
            return fallback
        }
        return group.cells.max { first, second in
            if first.row == second.row {
                return first.col > second.col
            }
            return first.row < second.row
        } ?? group.cells[0]
    }

    private func compactBoard(fixed: [[Bool]]? = nil) {
        for col in 0..<cols {
            var bottom = rows - 1
            while bottom >= 0 {
                while bottom >= 0, fixed?[bottom][col] == true {
                    bottom -= 1
                }
                guard bottom >= 0 else { break }

                var top = bottom
                while top >= 0, fixed?[top][col] != true {
                    top -= 1
                }

                compactSegment(col: col, top: top + 1, bottom: bottom)
                bottom = top - 1
            }
        }
    }

    private func compactSegment(col: Int, top: Int, bottom: Int) {
        guard top <= bottom else { return }
        var stack: [PrimeMatchTile] = []
        for row in stride(from: bottom, through: top, by: -1) where board[row][col].value != 0 {
            stack.append(board[row][col])
        }

        for row in stride(from: bottom, through: top, by: -1) {
            if !stack.isEmpty {
                board[row][col] = stack.removeFirst()
            } else {
                board[row][col] = PrimeMatchTile(value: 0, colorIndex: 0)
            }
        }

        for row in top...bottom where board[row][col].value == 0 {
            _ = randomTileAvoidingMatch(row: row, col: col)
        }
    }

    private func checkEnd() {
        if score >= target {
            withAnimation(.easeOut(duration: 0.35)) { active = false }
            recordLevelStars(3)
            bestScore = max(bestScore, score)
            let rewards = grantVictoryRewards()
            soundPlayer.play("ppf_pm_victory", enabled: soundEnabled)
            status = "Fase concluída: \(starsText(for: 3)) 100% da meta com \(score) pontos."
            if !rewards.isEmpty {
                status += "\n\(rewards)."
            }
        } else if moves <= 0 {
            active = false
            bestScore = max(bestScore, score)
            let percent = progressPercent(for: score)
            let earned = stars(for: score)
            recordLevelStars(earned)
            status = "Fim da fase: \(starsText(for: earned)) \(percent)% da meta. Valor: \(score). Melhor tentativa: \(bestScore)."
        }
    }

    private func recordLevelStars(_ earned: Int) {
        var stored = Array(levelStars.prefix(levelCount))
        stored += Array(repeating: 0, count: levelCount - stored.count)
        stored[level - 1] = max(stored[level - 1], UInt8(earned))
        levelStars = Data(stored)
    }

    private func grantVictoryRewards() -> String {
        guard !rewardGranted else { return "" }
        rewardGranted = true
        wins += 1

        switch wins % 6 {
        case 1:
            hammerCount += 1
            plus1Count += 1
            return "Recompensas: Martelo +1, +1 +1"
        case 2:
            reshuffleCount += 1
            plus2Count += 1
            hintCount += 1
            return "Recompensas: Reorganizar +1, +2 +1, Dica prima +1"
        case 3:
            plus3Count += 1
            freeSwapCount += 1
            return "Recompensas: +3 +1, Troca livre +1"
        case 4:
            colorCopyCount += 1
            polishUpCount += 1
            polishDownCount += 1
            return "Recompensas: Coringa +1, Lapidar +1 +1, Lapidar -1 +1"
        case 5:
            rowRayCount += 1
            colRayCount += 1
            return "Recompensas: Raio linha +1, Raio coluna +1"
        default:
            hintCount += 1
            comboMultCount += 1
            hammerCount += 1
            return "Recompensas: Dica prima +1, Combo x2 +1, Martelo +1"
        }
    }

    private func progressPercent(for value: Int) -> Int {
        guard target > 0 else { return 0 }
        return min(100, max(0, value * 100 / target))
    }

    private func stars(for value: Int) -> Int {
        let percent = progressPercent(for: value)
        if percent >= 100 { return 3 }
        if percent >= 66 { return 2 }
        if percent >= 33 { return 1 }
        return 0
    }

    private func starsText(for stars: Int) -> String {
        (0..<3).map { $0 < stars ? "★" : "☆" }.joined()
    }
}
