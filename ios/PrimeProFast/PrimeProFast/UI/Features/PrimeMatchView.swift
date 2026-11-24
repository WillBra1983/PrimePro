import SwiftUI
import AVFoundation

struct PrimeMatchTile: Identifiable, Equatable {
    let id = UUID()
    var value: Int
    var colorIndex: Int
}

private struct PrimeMatchGroup {
    var cells: [(row: Int, col: Int)]
    var sum: Int
    var colorIndex: Int
    var blackCount: Int
    var blackAnchor: (row: Int, col: Int)?
    var blackValue: Int
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
    private let target = 1000
    private let blackColorIndex = 99
    private let colors: [Color] = [
        Color(red: 0.11, green: 0.31, blue: 0.54),
        Color(red: 0.18, green: 0.49, blue: 0.44),
        Color(red: 0.71, green: 0.42, blue: 0.27),
        Color(red: 0.54, green: 0.35, blue: 0.51),
        Color(red: 0.77, green: 0.35, blue: 0.23)
    ]

    @State private var board: [[PrimeMatchTile]] = []
    @State private var selection: [(row: Int, col: Int)] = []
    @State private var dragStart: (row: Int, col: Int)?
    @State private var dragTarget: (row: Int, col: Int)?
    @State private var score = 0
    @State private var moves = 20
    @State private var status = "Meta: acumule 1000 pontos. Tente criar primos cada vez maiores."
    @State private var history: [String] = []
    @State private var historyCounter = 0
    @State private var active = true
    @State private var rewardGranted = false
    @StateObject private var soundPlayer = PrimeMatchSoundPlayer()
    @AppStorage("prime_match_sound_enabled") private var soundEnabled = true
    @AppStorage("prime_match_best_score") private var bestScore = 0
    @AppStorage("prime_match_wins") private var wins = 0
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
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                Text("Prime Match")
                    .font(.title2.bold())

                Text("Arraste uma bolinha para uma vizinha. Soma prima vira uma peça acumulada; soma composta apenas explode.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)

                HStack(spacing: 10) {
                    metric(title: "Valor", value: "\(score) / \(target)")
                    metric(title: "Jogadas", value: "\(max(0, moves))")
                }

                Button(soundEnabled ? "Som ligado" : "Som desligado") {
                    soundEnabled.toggle()
                    soundPlayer.play("ppf_pm_tap", enabled: soundEnabled)
                }
                .font(.caption.weight(.bold))
                .buttonStyle(.bordered)
                .tint(soundEnabled ? PPFTheme.primary : Color.secondary)

                Text(status)
                    .font(.subheadline.weight(.semibold))
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(12)
                    .background(Color(.secondarySystemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 8))

                Text(historyText())
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(10)
                    .background(Color(.secondarySystemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 8))

                if totalSavedPowers > 0 {
                    Text(inventoryText())
                        .font(.caption2.weight(.semibold))
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(10)
                        .background(Color(.tertiarySystemBackground))
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                }

                if board.count == rows && board.allSatisfy({ $0.count == cols }) {
                    LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 6), count: cols), spacing: 6) {
                        ForEach(0..<(rows * cols), id: \.self) { idx in
                            let row = idx / cols
                            let col = idx % cols
                            tileView(row: row, col: col)
                        }
                    }
                    .padding(6)
                    .background(Color(.secondarySystemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                }

                Button("Nova fase") {
                    reset()
                }
                .buttonStyle(.borderedProminent)
                .tint(PPFTheme.primary)
            }
            .padding()
        }
        .onAppear {
            if board.isEmpty {
                reset()
            }
        }
        .navigationTitle("Jogo dos Primos")
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
        return "Histórico da fase\n" + history.prefix(6).joined(separator: "\n")
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

    private func tileView(row: Int, col: Int) -> some View {
        let tile = board[row][col]
        let selected = selection.contains { $0.row == row && $0.col == col }
        let black = isBlack(tile.colorIndex)
        let color = black ? Color(red: 0.04, green: 0.05, blue: 0.07) : colors[max(0, tile.colorIndex) % colors.count]
        return Text(tile.value == 0 ? "" : "\(tile.value)")
            .font(.headline.bold())
            .foregroundStyle(black ? Color(red: 0.97, green: 0.86, blue: 0.57) : .white)
            .shadow(color: .black.opacity(0.62), radius: 1.5, x: 0, y: 1)
            .lineLimit(1)
            .minimumScaleFactor(0.52)
            .frame(maxWidth: .infinity)
            .aspectRatio(1, contentMode: .fit)
            .background(glassMarble(color: color, selected: selected || black))
            .scaleEffect(selected ? 1.12 : (black ? 1.08 : 1))
            .opacity(active ? 1 : 0.62)
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { value in
                        updateDrag(row: row, col: col, translation: value.translation)
                    }
                    .onEnded { value in
                        finishDrag(row: row, col: col, translation: value.translation)
                    }
            )
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
                                .white.opacity(0.88),
                                color.opacity(0.96),
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
        dragStart = nil
        dragTarget = nil
        score = 0
        moves = 20
        active = true
        rewardGranted = false
        status = "Meta: acumule \(target) pontos. Tente criar primos cada vez maiores."
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

    private func updateDrag(row: Int, col: Int, translation: CGSize) {
        guard active else { return }
        if dragStart == nil {
            dragStart = (row, col)
            selection = [(row, col)]
            soundPlayer.play("ppf_pm_tap", enabled: soundEnabled)
            status = "Arraste para uma bolinha vizinha."
        }
        guard let start = dragStart else { return }
        if let target = targetForDrag(row: start.row, col: start.col, translation: translation) {
            dragTarget = target
            selection = [start, target]
            status = "Solte para trocar as bolinhas."
        }
    }

    private func finishDrag(row: Int, col: Int, translation: CGSize) {
        guard active else {
            cancelDrag()
            return
        }
        if dragStart == nil {
            dragStart = (row, col)
        }
        guard let start = dragStart else { return }
        let resolvedTarget = targetForDrag(row: start.row, col: start.col, translation: translation) ?? dragTarget
        guard let target = resolvedTarget, isNeighbor(start, target) else {
            cancelDrag()
            status = "Arraste para uma bolinha vizinha para fazer a troca."
            return
        }

        swapTiles(start, target)
        selection.removeAll()

        let result = resolveMatches(anchor: target, fallback: start)
        if result.removed == 0 {
            swapTiles(start, target)
            soundPlayer.play("ppf_pm_composite", enabled: soundEnabled)
            status = "Essa troca não formou trio. Tente outra combinação."
        } else {
            moves -= 1
            soundPlayer.play(result.delta > 0 ? (result.combos > 1 ? "ppf_pm_combo" : "ppf_pm_prime") : "ppf_pm_composite", enabled: soundEnabled)
            status = result.delta > 0
                ? "Resolvido: \(result.removed) bolinhas em \(result.combos) combo(s). +\(result.delta) pontos acumulados."
                : "Resolvido: \(result.summary). Sem desconto."
        }

        dragStart = nil
        dragTarget = nil
        checkEnd()
    }

    private func cancelDrag() {
        dragStart = nil
        dragTarget = nil
        selection.removeAll()
    }

    private func targetForDrag(row: Int, col: Int, translation: CGSize) -> (row: Int, col: Int)? {
        let dx = translation.width
        let dy = translation.height
        guard max(abs(dx), abs(dy)) > 18 else { return nil }

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

    private func resolveMatches(anchor: (row: Int, col: Int)? = nil, fallback: (row: Int, col: Int)? = nil) -> (combos: Int, removed: Int, delta: Int, summary: String) {
        var combos = 0
        var removedTotal = 0
        var deltaTotal = 0
        var summaries: [String] = []
        var preferredAnchor = anchor
        var fallbackAnchor = fallback

        while combos < 12 {
            let groups = findMatchGroups()
            guard !groups.isEmpty else { break }

            let removed = groups.reduce(0) { $0 + $1.cells.count }
            var delta = 0
            var fixed = Array(repeating: Array(repeating: false, count: cols), count: rows)
            removedTotal += removed

            for group in groups {
                let prime = PPFMath.isPrime(group.sum)
                registerHistory(group: group, combo: combos + 1, prime: prime)
                summaries.append("\(group.sum) " + (prime ? "primo" : "composto"))
                for cell in group.cells {
                    board[cell.row][cell.col].value = 0
                }
                if prime {
                    let anchorCell = anchorFor(group: group, preferred: preferredAnchor, fallback: fallbackAnchor)
                    board[anchorCell.row][anchorCell.col] = PrimeMatchTile(value: group.sum, colorIndex: blackColorIndex)
                    fixed[anchorCell.row][anchorCell.col] = true
                    delta += group.sum
                }
            }
            score += delta
            deltaTotal += delta
            combos += 1
            compactBoard(fixed: fixed)
            preferredAnchor = nil
            fallbackAnchor = nil
        }

        let summary = summaries.prefix(3).joined(separator: ", ")
        return (combos, removedTotal, deltaTotal, summary.isEmpty ? "soma composta explodiu tudo" : summary)
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
                        end: end - 1,
                        colorIndex: baseColor ?? blackColorIndex
                    ))
                }
                start = max(end, start + 1)
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
        end: Int,
        colorIndex: Int
    ) -> PrimeMatchGroup {
        var cells: [(row: Int, col: Int)] = []
        var sum = 0
        var blackCount = 0
        var blackAnchor: (row: Int, col: Int)?
        var blackValue = 0

        for position in start...end {
            let cell = linearCell(horizontal: horizontal, line: line, position: position)
            cells.append(cell)
            sum += board[cell.row][cell.col].value
            if isBlack(board[cell.row][cell.col].colorIndex) {
                blackCount += 1
                if board[cell.row][cell.col].value > blackValue {
                    blackValue = board[cell.row][cell.col].value
                    blackAnchor = cell
                }
            }
        }

        return PrimeMatchGroup(
            cells: cells,
            sum: sum,
            colorIndex: colorIndex,
            blackCount: blackCount,
            blackAnchor: blackAnchor,
            blackValue: blackValue
        )
    }

    private func registerHistory(group: PrimeMatchGroup, combo: Int, prime: Bool) {
        historyCounter += 1
        let type = combo == 1 ? "Jogada" : "Combo \(combo)"
        let numbers = group.cells
            .map { board[$0.row][$0.col].value }
            .map(String.init)
            .joined(separator: " + ")
        let result = prime ? "primo, +\(group.sum), virou preta" : "composto, explodiu tudo, sem desconto"
        history.insert("\(historyCounter). \(type): \(numbers) = \(group.sum) | \(result)", at: 0)
        if history.count > 8 {
            history.removeLast()
        }
    }

    private func baseColorForGroup(matches: [[Bool]], row: Int, col: Int) -> Int {
        let initial = board[row][col].colorIndex
        guard isBlack(initial) else { return initial }

        let directions = [(0, -1), (0, 1), (-1, 0), (1, 0)]
        for direction in directions {
            var nextRow = row + direction.0
            var nextCol = col + direction.1
            while nextRow >= 0,
                  nextRow < rows,
                  nextCol >= 0,
                  nextCol < cols,
                  matches[nextRow][nextCol] {
                let colorIndex = board[nextRow][nextCol].colorIndex
                if !isBlack(colorIndex) {
                    return colorIndex
                }
                nextRow += direction.0
                nextCol += direction.1
            }
        }

        return initial
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

    private func findMatches() -> [[Bool]] {
        var matches = Array(repeating: Array(repeating: false, count: cols), count: rows)

        for row in 0..<rows {
            for start in 0..<cols {
                var baseColor: Int?
                for end in start..<cols {
                    guard board[row][end].value != 0 else { break }
                    let colorIndex = board[row][end].colorIndex
                    if !isBlack(colorIndex) {
                        if baseColor == nil {
                            baseColor = colorIndex
                        } else if baseColor != colorIndex {
                            break
                        }
                    }
                    if end - start + 1 >= minChain {
                        for col in start...end {
                            matches[row][col] = true
                        }
                    }
                }
            }
        }

        for col in 0..<cols {
            for start in 0..<rows {
                var baseColor: Int?
                for end in start..<rows {
                    guard board[end][col].value != 0 else { break }
                    let colorIndex = board[end][col].colorIndex
                    if !isBlack(colorIndex) {
                        if baseColor == nil {
                            baseColor = colorIndex
                        } else if baseColor != colorIndex {
                            break
                        }
                    }
                    if end - start + 1 >= minChain {
                        for row in start...end {
                            matches[row][col] = true
                        }
                    }
                }
            }
        }

        return matches
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
            active = false
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
            status = "Fim da fase: \(starsText(for: stars(for: score))) \(percent)% da meta. Valor: \(score). Melhor tentativa: \(bestScore)."
        }
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
