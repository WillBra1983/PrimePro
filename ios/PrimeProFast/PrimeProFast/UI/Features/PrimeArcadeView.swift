import SpriteKit
import SwiftUI
import UIKit

enum PrimeArcadeMode {
    case runner
    case shooter

    var title: String { self == .runner ? "Corrida Prima" : "Resgate Espacial" }
    var storageKey: String { self == .runner ? "prime_runner" : "prime_shooter" }
}

struct PrimeArcadeView: View {
    let mode: PrimeArcadeMode
    @State private var scene: PrimeArcadeScene
    @Environment(\.scenePhase) private var scenePhase

    init(mode: PrimeArcadeMode) {
        self.mode = mode
        _scene = State(initialValue: PrimeArcadeScene(mode: mode))
    }

    var body: some View {
        VStack(spacing: 12) {
            PrimeArcadeSurface(scene: scene)
                .aspectRatio(360.0 / 420.0, contentMode: .fit)
                .accessibilityLabel(mode.title)

            HStack {
                Button {
                    scene.restart()
                } label: {
                    Image(systemName: "arrow.counterclockwise")
                        .frame(minWidth: 36, minHeight: 36)
                }
                .accessibilityLabel("Reiniciar")
                .buttonStyle(.bordered)

                if mode == .runner {
                    Button {
                        scene.jump()
                    } label: {
                        Label("Pular", systemImage: "arrow.up")
                            .frame(maxWidth: .infinity, minHeight: 36)
                    }
                    .buttonStyle(.borderedProminent)
                } else {
                    Text("1 dedo para desviar · 2 dedos para atirar")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity)
                }
            }
        }
        .padding(8)
        .frame(maxWidth: 520)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .navigationTitle(mode.title)
        .navigationBarTitleDisplayMode(.inline)
        .onDisappear { scene.suspend() }
        .onChange(of: scenePhase) { phase in
            if phase != .active { scene.suspend() }
        }
    }
}

private struct PrimeArcadeSurface: UIViewRepresentable {
    let scene: PrimeArcadeScene

    func makeUIView(context: Context) -> SKView {
        let view = SKView()
        view.isMultipleTouchEnabled = true
        view.preferredFramesPerSecond = 60
        view.presentScene(scene)
        return view
    }

    func updateUIView(_ view: SKView, context: Context) {
        if view.scene !== scene { view.presentScene(scene) }
    }
}

private final class PrimeArcadePiece: SKShapeNode {
    let number: Int
    let isPrime: Bool

    init(number: Int, prime: Bool) {
        self.number = number
        self.isPrime = prime
        super.init()
        path = CGPath(ellipseIn: CGRect(x: -17, y: -17, width: 34, height: 34), transform: nil)
        fillColor = prime ? SKColor(red: 0.11, green: 0.69, blue: 0.49, alpha: 1)
                          : SKColor(red: 0.72, green: 0.20, blue: 0.29, alpha: 1)
        strokeColor = .white.withAlphaComponent(0.6)
        let label = SKLabelNode(fontNamed: "AvenirNext-DemiBold")
        label.text = String(number)
        label.fontSize = 15
        label.verticalAlignmentMode = .center
        addChild(label)
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) is unavailable") }
}

private final class PrimeArcadeScene: SKScene {
    private let mode: PrimeArcadeMode
    private let defaults = UserDefaults.standard
    private let player = SKShapeNode()
    private let hud = SKLabelNode(fontNamed: "AvenirNext-DemiBold")
    private let message = SKLabelNode(fontNamed: "AvenirNext-DemiBold")
    private let field = SKNode()
    private let scenery = SKNode()
    private let vehicleIcon = SKLabelNode(fontNamed: "AppleColorEmoji")
    private let shieldRing = SKShapeNode(circleOfRadius: 28)
    private let floor: CGFloat = 67
    private var pieces: [PrimeArcadePiece] = []
    private var shots: [SKShapeNode] = []
    private var lastFrame: TimeInterval?
    private var spawnIn = 1.2
    private var shotDelay = 0.0
    private var shieldTime = 0.0
    private var hitGrace = 0.0
    private var steeringTouches: Set<UITouch> = []
    private var twoFingerOrigin: CGPoint?
    private var firing = false
    private var visualTier = -1
    private var stage = 0
    private var score = 0.0
    private var lives = 3
    private var running = false
    private var ended = false
    private var installed = false
    private var highScore = 0

    init(mode: PrimeArcadeMode) {
        self.mode = mode
        super.init(size: CGSize(width: 360, height: 420))
        scaleMode = .aspectFit
        highScore = max(0, defaults.integer(forKey: mode.storageKey + "_best_score"))
        if mode == .shooter {
            score = Double(max(0, defaults.integer(forKey: mode.storageKey + "_last_score")))
        }
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) is unavailable") }

    override func didMove(to view: SKView) {
        isUserInteractionEnabled = true
        guard !installed else { return }
        installed = true
        backgroundColor = SKColor(red: 0.06, green: 0.10, blue: 0.15, alpha: 1)
        addChild(field)
        scenery.zPosition = -1
        field.addChild(scenery)

        if mode == .runner {
            player.path = CGPath(roundedRect: CGRect(x: -14, y: -17, width: 28, height: 34),
                                 cornerWidth: 6, cornerHeight: 6, transform: nil)
        } else {
            let ship = CGMutablePath()
            ship.move(to: CGPoint(x: 0, y: 20))
            ship.addLines(between: [CGPoint(x: 0, y: 20), CGPoint(x: -18, y: -15),
                                    CGPoint(x: 0, y: -8), CGPoint(x: 18, y: -15)])
            ship.closeSubpath()
            player.path = ship
        }
        player.fillColor = SKColor(red: 0.27, green: 0.76, blue: 0.97, alpha: 1)
        player.strokeColor = .white
        player.zPosition = 2
        vehicleIcon.verticalAlignmentMode = .center
        vehicleIcon.fontSize = 30
        player.addChild(vehicleIcon)
        field.addChild(player)
        shieldRing.fillColor = .cyan.withAlphaComponent(0.16)
        shieldRing.strokeColor = .cyan
        shieldRing.lineWidth = 3
        shieldRing.glowWidth = 5
        shieldRing.zPosition = 3
        shieldRing.isHidden = true
        field.addChild(shieldRing)
        positionPlayer()
        updateScenery()

        hud.fontSize = 14
        hud.position = CGPoint(x: size.width / 2, y: size.height - 26)
        hud.zPosition = 5
        addChild(hud)
        message.fontSize = 17
        message.position = CGPoint(x: size.width / 2, y: size.height / 2)
        message.zPosition = 5
        addChild(message)
        setPrompt()
        refreshHUD()
    }

    private func updateScenery() {
        stage = max(stage, Int(score) / (mode == .runner ? 900 : 1200))
        let tier = stage % 3
        guard tier != visualTier else { return }
        visualTier = tier
        scenery.removeAllChildren()
        let backgrounds: [SKColor] = mode == .runner
            ? [SKColor(red: 0.12, green: 0.34, blue: 0.29, alpha: 1),
               SKColor(red: 0.32, green: 0.19, blue: 0.28, alpha: 1),
               SKColor(red: 0.09, green: 0.15, blue: 0.28, alpha: 1)]
            : [SKColor(red: 0.13, green: 0.28, blue: 0.43, alpha: 1),
               SKColor(red: 0.21, green: 0.11, blue: 0.30, alpha: 1),
               SKColor(red: 0.05, green: 0.24, blue: 0.27, alpha: 1)]
        backgroundColor = backgrounds[tier]
        vehicleIcon.text = mode == .runner ? ["🚲", "🏍️", "🏎️"][tier] : ["✈️", "🚀", "🛸"][tier]
        player.fillColor = .clear
        player.strokeColor = .clear

        let line = SKShapeNode(rectOf: CGSize(width: size.width, height: 3))
        line.position = CGPoint(x: size.width / 2, y: mode == .runner ? floor : 38)
        line.fillColor = SKColor(red: 0.08, green: 0.45, blue: 0.46, alpha: 1)
        line.strokeColor = .clear
        scenery.addChild(line)
        if mode == .runner {
            let skyline = CGMutablePath()
            skyline.move(to: CGPoint(x: 0, y: floor))
            for index in 0...12 {
                let height = CGFloat(18 + (index * 23 + tier * 17) % 68)
                skyline.addLine(to: CGPoint(x: CGFloat(index) * size.width / 12, y: floor + height))
            }
            skyline.addLine(to: CGPoint(x: size.width, y: floor))
            skyline.closeSubpath()
            let horizon = SKShapeNode(path: skyline)
            horizon.fillColor = .black.withAlphaComponent(0.24)
            horizon.strokeColor = .clear
            scenery.addChild(horizon)
        } else {
            let planet = SKShapeNode(circleOfRadius: CGFloat(31 + 14 * tier))
            planet.position = CGPoint(x: CGFloat(281 - tier * 84), y: 301)
            planet.fillColor = .white.withAlphaComponent(0.10)
            planet.strokeColor = .white.withAlphaComponent(0.18)
            scenery.addChild(planet)
        }
        for index in 0..<32 {
            let dot = SKShapeNode(circleOfRadius: index.isMultiple(of: 5) ? 1.7 : 0.8)
            dot.fillColor = .white.withAlphaComponent(index.isMultiple(of: 5) ? 0.5 : 0.23)
            dot.strokeColor = .clear
            dot.position = CGPoint(x: CGFloat((index * 71) % 353) + 4,
                                   y: CGFloat((index * 47) % 293) + 82)
            scenery.addChild(dot)
        }
    }

    private func beginIfNeeded() {
        guard installed else { return }
        if ended { restart() }
        if !running {
            running = true
            field.isPaused = false
            lastFrame = nil
            message.text = ""
        }
    }

    func jump() {
        guard installed, mode == .runner else { return }
        beginIfNeeded()
        guard player.action(forKey: "jump") == nil else { return }
        let up = SKAction.moveTo(y: floor + 101, duration: 0.28)
        up.timingMode = .easeOut
        let down = SKAction.moveTo(y: floor + 17, duration: 0.38)
        down.timingMode = .easeIn
        player.run(.sequence([up, down]), withKey: "jump")
    }

    private func shoot() {
        if shotDelay <= 0 {
            let shot = SKShapeNode(rectOf: CGSize(width: 4, height: 18), cornerRadius: 2)
            shot.fillColor = .white
            shot.strokeColor = .cyan
            shot.position = CGPoint(x: player.position.x, y: player.position.y + 22)
            field.addChild(shot)
            shots.append(shot)
            shotDelay = 0.16
        }
    }

    func restart() {
        guard installed else { return }
        suspend()
        score = 0
        stage = 0
        lives = 3
        ended = false
        pieces.forEach { $0.removeFromParent() }
        shots.forEach { $0.removeFromParent() }
        pieces.removeAll()
        shots.removeAll()
        spawnIn = 1.2
        shotDelay = 0
        shieldTime = 0
        hitGrace = 0
        shieldRing.isHidden = true
        positionPlayer()
        updateScenery()
        setPrompt()
        refreshHUD()
        saveProgress()
    }

    func suspend() {
        running = false
        lastFrame = nil
        field.isPaused = true
        steeringTouches.removeAll()
        twoFingerOrigin = nil
        firing = false
        saveProgress()
        if !ended { setPrompt() }
    }

    private func positionPlayer() {
        player.removeAllActions()
        player.position = CGPoint(x: mode == .runner ? 60 : size.width / 2, y: mode == .runner ? floor + 17 : 49)
    }

    private func setPrompt() {
        message.text = score > 0 ? "Toque para continuar" : "Toque para começar"
    }

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        if mode == .shooter {
            beginIfNeeded()
            steeringTouches.formUnion(touches)
            twoFingerOrigin = steeringTouches.count == 2 ? steeringCenter() : nil
            firing = false
            if steeringTouches.count == 1, let center = steeringCenter() { moveShip(to: center.x) }
        } else {
            jump()
        }
    }

    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard mode == .shooter, running, let center = steeringCenter() else { return }
        moveShip(to: center.x)
        if steeringTouches.count == 2, let origin = twoFingerOrigin,
           hypot(center.x - origin.x, center.y - origin.y) >= 4 {
            firing = true
        }
    }

    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        endSteering(touches)
    }

    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        endSteering(touches)
    }

    private func endSteering(_ touches: Set<UITouch>) {
        steeringTouches.subtract(touches)
        firing = false
        twoFingerOrigin = steeringTouches.count == 2 ? steeringCenter() : nil
    }

    private func steeringCenter() -> CGPoint? {
        guard !steeringTouches.isEmpty else { return nil }
        let total = steeringTouches.reduce(CGPoint.zero) { sum, touch in
            let point = touch.location(in: self)
            return CGPoint(x: sum.x + point.x, y: sum.y + point.y)
        }
        let count = CGFloat(steeringTouches.count)
        return CGPoint(x: total.x / count, y: total.y / count)
    }

    private func moveShip(to x: CGFloat) {
        player.position.x = min(size.width - 20, max(20, x))
    }

    override func update(_ currentTime: TimeInterval) {
        guard running else { return }
        defer { if running { lastFrame = currentTime } }
        guard let lastFrame else { return }
        let delta = min(0.033, max(0, currentTime - lastFrame))
        let sector = 1 + Int(score / 1200)
        score += delta * (mode == .runner ? 24 : 10 + Double(sector) * 1.8)
        spawnIn -= delta
        shotDelay -= delta
        shieldTime = max(0, shieldTime - delta)
        hitGrace = max(0, hitGrace - delta)
        shieldRing.position = player.position
        shieldRing.isHidden = shieldTime <= 0
        if mode == .shooter && firing && steeringTouches.count == 2 { shoot() }
        if spawnIn <= 0 { spawn() }

        for shot in shots { shot.position.y += CGFloat(delta) * 390 }
        shots.removeAll { shot in
            guard shot.position.y > size.height || shot.parent == nil else { return false }
            shot.removeFromParent()
            return true
        }
        for piece in pieces {
            if mode == .runner {
                piece.position.x -= CGFloat(delta * (220 + min(135, score * 0.46)))
            } else {
                piece.position.y -= CGFloat(delta * (122 + min(190, score / 9)))
                if let shot = shots.first(where: { $0.parent != nil && $0.frame.intersects(piece.frame) }) {
                    shot.removeFromParent()
                    piece.removeFromParent()
                    score = max(0, score + Double(piece.isPrime ? -20 : piece.number + 12))
                    continue
                }
            }
            if player.frame.intersects(piece.frame) {
                piece.removeFromParent()
                if piece.isPrime {
                    score += Double(piece.number * 2 + 25)
                    if mode == .shooter {
                        shieldTime = max(shieldTime, 2.5)
                        shieldRing.isHidden = false
                    }
                } else if mode == .runner {
                    finish()
                    break
                } else if shieldTime <= 0 && hitGrace <= 0 {
                    lives -= 1
                    if lives == 0 { finish(); break }
                    hitGrace = 0.8
                }
            } else if piece.position.x < -22 || piece.position.y < -22 {
                piece.removeFromParent()
            }
        }
        pieces.removeAll { $0.parent == nil }
        updateScenery()
        refreshHUD()
    }

    private func spawn() {
        let prime = Int.random(in: 0..<100) < (mode == .runner ? 42 : 38)
        let ceiling = Int.random(in: 97...150)
        let candidates = (2...ceiling).filter { PPFMath.isPrime($0) == prime }
        guard let number = candidates.randomElement() else { return }
        let piece = PrimeArcadePiece(number: number, prime: prime)
        if mode == .runner {
            piece.position = CGPoint(x: size.width + 22, y: floor + 17)
            spawnIn = max(0.92, 1.38 - score * 0.00028) + Double.random(in: 0...0.65)
        } else {
            piece.position = CGPoint(x: CGFloat.random(in: 22...(size.width - 22)), y: size.height + 22)
            spawnIn = max(0.42, 1.1 - score / 5000) + Double.random(in: 0...0.32)
        }
        pieces.append(piece)
        field.addChild(piece)
    }

    private func finish() {
        ended = true
        suspend()
        message.text = "Fim de jogo. Toque para reiniciar"
    }

    private func refreshHUD() {
        let points = Int(score)
        let record = max(highScore, points)
        if mode == .runner {
            hud.text = "Pontos \(points)   •   Recorde \(record)   •   Etapa \(stage + 1)"
        } else {
            let shield = shieldTime > 0 ? "   •   Escudo \(Int(ceil(shieldTime))) s" : ""
            hud.text = "Pontos \(points)   •   Vidas \(lives)   •   Setor \(stage + 1)" + shield
        }
    }

    private func saveProgress() {
        highScore = max(highScore, Int(score))
        defaults.set(highScore, forKey: mode.storageKey + "_best_score")
        if mode == .shooter {
            defaults.set(ended ? 0 : Int(score), forKey: mode.storageKey + "_last_score")
        }
    }
}
