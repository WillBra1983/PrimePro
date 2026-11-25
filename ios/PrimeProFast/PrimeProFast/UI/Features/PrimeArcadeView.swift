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
            SpriteView(scene: scene, preferredFramesPerSecond: 60)
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

                Button {
                    scene.primaryAction()
                } label: {
                    Label(mode == .runner ? "Pular" : "Atirar",
                          systemImage: mode == .runner ? "arrow.up" : "scope")
                        .frame(maxWidth: .infinity, minHeight: 36)
                }
                .buttonStyle(.borderedProminent)
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
    private let floor: CGFloat = 67
    private var pieces: [PrimeArcadePiece] = []
    private var shots: [SKShapeNode] = []
    private var lastFrame: TimeInterval?
    private var spawnIn = 1.2
    private var shotDelay = 0.0
    private var shieldTime = 0.0
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
        guard !installed else { return }
        installed = true
        backgroundColor = SKColor(red: 0.06, green: 0.10, blue: 0.15, alpha: 1)
        physicsWorld.gravity = CGVector(dx: 0, dy: -15)
        addChild(field)
        decorate()

        if mode == .runner {
            player.path = CGPath(roundedRect: CGRect(x: -14, y: -17, width: 28, height: 34),
                                 cornerWidth: 6, cornerHeight: 6, transform: nil)
            let ground = SKNode()
            ground.physicsBody = SKPhysicsBody(edgeFrom: CGPoint(x: 0, y: floor), to: CGPoint(x: size.width, y: floor))
            field.addChild(ground)
            let body = SKPhysicsBody(rectangleOf: CGSize(width: 28, height: 34))
            body.allowsRotation = false
            body.restitution = 0
            player.physicsBody = body
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
        field.addChild(player)
        positionPlayer()

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

    private func decorate() {
        let line = SKShapeNode(rectOf: CGSize(width: size.width, height: 3))
        line.position = CGPoint(x: size.width / 2, y: mode == .runner ? floor : 38)
        line.fillColor = SKColor(red: 0.08, green: 0.45, blue: 0.46, alpha: 1)
        line.strokeColor = .clear
        field.addChild(line)
        for index in 0..<32 {
            let dot = SKShapeNode(circleOfRadius: index.isMultiple(of: 5) ? 1.7 : 0.8)
            dot.fillColor = .white.withAlphaComponent(index.isMultiple(of: 5) ? 0.5 : 0.23)
            dot.strokeColor = .clear
            dot.position = CGPoint(x: CGFloat((index * 71) % 353) + 4,
                                   y: CGFloat((index * 47) % 293) + 82)
            field.addChild(dot)
        }
    }

    func primaryAction() {
        guard installed else { return }
        if ended { restart() }
        if !running {
            running = true
            field.isPaused = false
            if mode == .runner { player.physicsBody?.isDynamic = true }
            lastFrame = nil
            message.text = ""
        }
        if mode == .runner {
            if player.position.y <= floor + 19, let body = player.physicsBody, abs(body.velocity.dy) < 20 {
                body.velocity.dy = 380
            }
        } else if shotDelay <= 0 {
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
        lives = 3
        ended = false
        pieces.forEach { $0.removeFromParent() }
        shots.forEach { $0.removeFromParent() }
        pieces.removeAll()
        shots.removeAll()
        spawnIn = 1.2
        shotDelay = 0
        shieldTime = 0
        player.alpha = 1
        positionPlayer()
        setPrompt()
        refreshHUD()
        saveProgress()
    }

    func suspend() {
        running = false
        lastFrame = nil
        field.isPaused = true
        player.physicsBody?.isDynamic = false
        saveProgress()
        if !ended { setPrompt() }
    }

    private func positionPlayer() {
        player.position = CGPoint(x: mode == .runner ? 60 : size.width / 2, y: mode == .runner ? floor + 17 : 49)
        player.physicsBody?.velocity = .zero
        player.physicsBody?.isDynamic = false
    }

    private func setPrompt() {
        message.text = score > 0 ? "Toque para continuar" : "Toque para começar"
    }

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        if mode == .shooter, let touch = touches.first {
            moveShip(to: touch.location(in: self).x)
            if !running { primaryAction() }
        } else {
            primaryAction()
        }
    }

    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard mode == .shooter, running, let touch = touches.first else { return }
        moveShip(to: touch.location(in: self).x)
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
        player.alpha = shieldTime > 0 ? 0.66 : 1
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
                    if mode == .shooter { shieldTime = max(shieldTime, 1.5) }
                } else if mode == .runner {
                    finish()
                    break
                } else if shieldTime <= 0 {
                    lives -= 1
                    if lives == 0 { finish(); break }
                    shieldTime = 0.8
                }
            } else if piece.position.x < -22 || piece.position.y < -22 {
                piece.removeFromParent()
            }
        }
        pieces.removeAll { $0.parent == nil }
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
            hud.text = "Pontos \(points)   •   Recorde \(record)"
        } else {
            hud.text = "Pontos \(points)   •   Vidas \(lives)   •   Setor \(1 + points / 1200)"
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
