import SpriteKit
import SwiftUI
import UIKit

private let arcadeRules = ppf_arcade_rules()

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
        VStack(spacing: 0) {
            PrimeArcadeSurface(scene: scene)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .accessibilityLabel(mode.title)
            if mode == .runner {
                PrimeRunnerJumpControl(onPress: scene.jump)
                    .frame(maxWidth: .infinity)
                    .frame(height: 44)
                    .padding(.horizontal, 8)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(red: 0.06, green: 0.11, blue: 0.16).ignoresSafeArea())
        .navigationTitle(mode.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button { scene.restart() } label: { Image(systemName: "arrow.counterclockwise") }
                    .accessibilityLabel("Reiniciar")
            }
        }
        .onDisappear { scene.suspend() }
        .onChange(of: scenePhase) { phase in
            if phase != .active { scene.suspend() }
        }
    }
}

private struct PrimeRunnerJumpControl: UIViewRepresentable {
    let onPress: () -> Void

    func makeUIView(context: Context) -> JumpButton {
        let button = JumpButton(type: .system)
        var style = UIButton.Configuration.filled()
        style.title = "Pular"
        style.image = UIImage(systemName: "arrow.up")
        style.imagePadding = 6
        button.configuration = style
        button.accessibilityLabel = "Pular"
        button.addTarget(button, action: #selector(JumpButton.activateJump), for: .touchDown)
        button.onPress = onPress
        return button
    }

    func updateUIView(_ button: JumpButton, context: Context) {
        button.onPress = onPress
    }

    final class JumpButton: UIButton {
        var onPress: (() -> Void)?

        @objc func activateJump() { onPress?() }

        override func accessibilityActivate() -> Bool {
            activateJump()
            return true
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
    let speed: CGFloat

    init(number: Int, prime: Bool, flight: Bool, speed: CGFloat) {
        self.number = number
        self.isPrime = prime
        self.speed = speed
        super.init()
        let radius: CGFloat = flight ? (prime ? 22 : 20) : 26
        path = CGPath(ellipseIn: CGRect(x: -radius, y: -radius, width: radius * 2, height: radius * 2), transform: nil)
        fillColor = prime ? SKColor(red: 0.11, green: 0.69, blue: 0.49, alpha: 1)
                          : SKColor(red: 0.72, green: 0.20, blue: 0.29, alpha: 1)
        strokeColor = .white.withAlphaComponent(0.6)
        let label = SKLabelNode(fontNamed: "AvenirNext-DemiBold")
        label.text = String(number)
        label.fontSize = 17
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
    private let vehicleSprite = SKSpriteNode()
    private let pulse = SKLabelNode(fontNamed: "AvenirNext-DemiBold")
    private let wheelMarks = [SKShapeNode(), SKShapeNode()]
    private let exhaust = (0..<6).map { _ in SKShapeNode(circleOfRadius: 4) }
    private let vehicleTextures: [SKTexture?]
    private let backdrop: SKTexture?
    private let shieldRing = SKShapeNode(circleOfRadius: 44)
    private let floor = CGFloat(arcadeRules.floor_y)
    private var pieces: [PrimeArcadePiece] = []
    private var shots: [SKShapeNode] = []
    private var lastFrame: TimeInterval?
    private var spawnIn = Double(arcadeRules.spawn_start_ms) / 1000
    private var previousNumber: Int32 = 0
    private var jumpElapsed: TimeInterval?
    private var played = false
    private var sinceSave: TimeInterval = 0
    private var shotDelay = 0.0
    private var shieldTime = 0.0
    private var hitGrace = 0.0
    private var steeringTouches: Set<UITouch> = []
    private var twoFingerOrigin: CGPoint?
    private var firing = false
    private var visualTier = -1
    private var stage = 0
    private var score: Int32 = 0
    private var lives = Int(arcadeRules.lives)
    private var running = false
    private var ended = false
    private var installed = false
    private var highScore = 0

    init(mode: PrimeArcadeMode) {
        self.mode = mode
        let names = mode == .runner ? ["bicycle", "motorcycle", "car"] : ["airplane", "rocket", "spaceship"]
        vehicleTextures = names.map { Self.texture($0, ext: "png") }
        backdrop = Self.texture(mode == .runner ? "road" : "space", ext: "jpg")
        super.init(size: CGSize(width: 360, height: 420))
        scaleMode = .resizeFill
        highScore = max(0, defaults.integer(forKey: mode.storageKey + "_best_score"))
        if mode == .shooter {
            score = Int32(clamping: max(0, defaults.integer(forKey: mode.storageKey + "_last_score")))
            let savedLives = defaults.object(forKey: mode.storageKey + "_remaining_lives") as? Int
            lives = min(Int(arcadeRules.lives), max(1, savedLives ?? Int(arcadeRules.lives)))
            let savedShield = defaults.integer(forKey: mode.storageKey + "_remaining_shield_ms")
            shieldTime = Double(min(Int(arcadeRules.shield_ms), max(0, savedShield))) / 1000
        }
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) is unavailable") }

    private static func texture(_ name: String, ext: String) -> SKTexture? {
        guard let url = Bundle.main.url(forResource: name, withExtension: ext, subdirectory: "arcade"),
              let image = UIImage(contentsOfFile: url.path) else { return nil }
        return SKTexture(image: image)
    }

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
        player.addChild(vehicleSprite)
        wheelMarks.forEach { vehicleSprite.addChild($0) }
        exhaust.forEach {
            $0.strokeColor = .clear
            $0.zPosition = 1
            $0.isHidden = true
            field.addChild($0)
        }
        field.addChild(player)
        shieldRing.fillColor = .cyan.withAlphaComponent(0.16)
        shieldRing.strokeColor = .cyan
        shieldRing.lineWidth = 3
        shieldRing.glowWidth = 5
        shieldRing.zPosition = 3
        shieldRing.isHidden = shieldTime <= 0
        field.addChild(shieldRing)
        positionPlayer()
        shieldRing.position = player.position
        updateScenery()

        hud.fontSize = 14
        hud.position = CGPoint(x: size.width / 2, y: size.height - 26)
        hud.zPosition = 5
        addChild(hud)
        message.fontSize = 17
        message.position = CGPoint(x: size.width / 2, y: size.height / 2)
        message.zPosition = 5
        addChild(message)
        pulse.fontSize = 16
        pulse.zPosition = 5
        pulse.alpha = 0
        addChild(pulse)
        layoutScene()
        setPrompt()
        refreshHUD()
    }

    override func didChangeSize(_ oldSize: CGSize) {
        super.didChangeSize(oldSize)
        if installed { layoutScene() }
    }

    private func layoutScene() {
        hud.position = CGPoint(x: size.width / 2, y: size.height - 25)
        hud.fontSize = size.width < 350 ? 11 : 14
        message.position = CGPoint(x: size.width / 2, y: size.height / 2)
        pulse.position = CGPoint(x: size.width / 2, y: size.height - 60)
        if mode == .shooter { moveShip(to: player.position.x) }
        visualTier = -1
        updateScenery()
    }

    private func updateScenery() {
        stage = Int(ppf_arcade_stage(score, mode == .shooter))
        let tier = Int(ppf_arcade_vehicle(Int32(stage)))
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
        configureVehicle(tier)

        if let backdrop {
            let photo = SKSpriteNode(texture: backdrop)
            let image = backdrop.size()
            let scale = max(size.width / image.width, size.height / image.height)
            photo.size = CGSize(width: image.width * scale, height: image.height * scale)
            photo.position = CGPoint(x: size.width / 2, y: size.height / 2)
            photo.color = backgrounds[tier]
            photo.colorBlendFactor = 0.25
            photo.zPosition = -2
            scenery.addChild(photo)
        }

        if mode == .shooter {
            let stripHeight = CGFloat(arcadeRules.control_height)
            let strip = SKShapeNode(rectOf: CGSize(width: size.width, height: stripHeight))
            strip.position = CGPoint(x: size.width / 2, y: stripHeight / 2)
            strip.fillColor = .black.withAlphaComponent(0.55)
            strip.strokeColor = .clear
            scenery.addChild(strip)
            let grip = SKShapeNode(rectOf: CGSize(width: 40, height: 3), cornerRadius: 1.5)
            grip.position = CGPoint(x: size.width / 2, y: 22)
            grip.fillColor = .white.withAlphaComponent(0.48)
            grip.strokeColor = .clear
            scenery.addChild(grip)
        }

        let line = SKShapeNode(rectOf: CGSize(width: size.width, height: 3))
        line.position = CGPoint(x: size.width / 2, y: mode == .runner ? floor : CGFloat(arcadeRules.control_height))
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
            dot.position = CGPoint(x: CGFloat((index * 71) % 353) / 353 * size.width,
                                   y: 82 + CGFloat((index * 47) % 293) / 293 * max(0, size.height - 82))
            scenery.addChild(dot)
        }
    }

    private func configureVehicle(_ tier: Int) {
        let texture = vehicleTextures[tier]
        vehicleSprite.texture = texture
        vehicleSprite.isHidden = texture == nil
        player.fillColor = texture == nil ? .systemTeal : .clear
        player.strokeColor = texture == nil ? .white : .clear
        guard let texture else { return }
        let source = texture.size()
        let scale = min((mode == .runner ? 82 : 72) / source.width,
                        (mode == .runner ? 49 : 70) / source.height)
        let w = source.width * scale, h = source.height * scale
        vehicleSprite.size = CGSize(width: w, height: h)
        vehicleSprite.position = CGPoint(x: 0, y: mode == .runner ? h / 2 - CGFloat(arcadeRules.runner_center) : 0)
        let positions: [(CGFloat, CGFloat, CGFloat)] = [(0.18, 0.77, 0.34), (0.23, 0.81, 0.33), (0.18, 0.77, 0.30)]
        for (idx, wheel) in wheelMarks.enumerated() {
            wheel.isHidden = mode == .shooter
            let (left, right, y) = positions[tier]
            wheel.position = CGPoint(x: ((idx == 0 ? left : right) - 0.5) * w, y: (y - 0.5) * h)
            let spoke = CGMutablePath()
            for angle in [0.0, Double.pi / 2] {
                let dx = CGFloat(cos(angle)) * w * 0.09
                let dy = CGFloat(sin(angle)) * w * 0.09
                spoke.move(to: CGPoint(x: -dx, y: -dy))
                spoke.addLine(to: CGPoint(x: dx, y: dy))
            }
            wheel.path = spoke
            wheel.lineWidth = 0.8
            wheel.strokeColor = .white.withAlphaComponent(0.28)
        }
    }

    private func animateVehicle(_ time: TimeInterval) {
        wheelMarks.forEach { $0.zRotation = -CGFloat(time.truncatingRemainder(dividingBy: 100) * 18) }
        for (index, puff) in exhaust.enumerated() {
            let life = (time * 2.2 + Double(index) / 6).truncatingRemainder(dividingBy: 1)
            let p = CGFloat(life)
            puff.isHidden = mode == .runner && visualTier == 0
            puff.position = mode == .runner
                ? CGPoint(x: player.position.x - 37 - p * 40, y: player.position.y - 8 + p * 14)
                : CGPoint(x: player.position.x + CGFloat(index % 3 - 1) * p * 11,
                          y: player.position.y - 32 - p * 32)
            puff.setScale(0.65 + p * 1.45)
            puff.fillColor = (mode == .runner ? SKColor.lightGray : SKColor.cyan)
                .withAlphaComponent((1 - p) * 0.35)
        }
    }

    private func showPoints(_ label: String, _ points: Int32) {
        guard points > 0 else { return }
        score = ppf_arcade_add_score(score, points)
        showPulse("\(label)   +\(points)")
    }

    private func showPulse(_ label: String) {
        pulse.removeAllActions()
        pulse.text = label
        pulse.alpha = 1
        pulse.run(.sequence([.wait(forDuration: 0.65), .fadeOut(withDuration: 0.3)]))
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
        guard jumpElapsed == nil else { return }
        jumpElapsed = 0
    }

    private func shoot() {
        if shotDelay <= 0 {
            let shot = SKShapeNode(rect: CGRect(x: -6, y: -10, width: 12, height: 40), cornerRadius: 4)
            shot.fillColor = .white
            shot.strokeColor = .cyan
            shot.position = CGPoint(x: player.position.x, y: player.position.y + CGFloat(arcadeRules.shot_offset))
            field.addChild(shot)
            shots.append(shot)
            shotDelay = Double(arcadeRules.shot_cooldown_ms) / 1000
        }
    }

    func restart() {
        guard installed else { return }
        suspend()
        score = 0
        stage = 0
        lives = Int(arcadeRules.lives)
        ended = false
        pieces.forEach { $0.removeFromParent() }
        shots.forEach { $0.removeFromParent() }
        pieces.removeAll()
        shots.removeAll()
        spawnIn = Double(arcadeRules.spawn_start_ms) / 1000
        previousNumber = 0
        jumpElapsed = nil
        played = false
        sinceSave = 0
        shotDelay = 0
        shieldTime = 0
        hitGrace = 0
        shieldRing.isHidden = true
        pulse.removeAllActions()
        pulse.alpha = 0
        exhaust.forEach { $0.isHidden = true }
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
        player.position = CGPoint(x: mode == .runner ? CGFloat(arcadeRules.runner_x) : size.width / 2,
                                  y: mode == .runner ? floor + CGFloat(arcadeRules.runner_center) : CGFloat(arcadeRules.ship_y))
    }

    private func setPrompt() {
        message.text = (played || score > 0 || lives < Int(arcadeRules.lives)) ? "Toque para continuar" : "Toque para começar"
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
        if !firing, steeringTouches.count == 2, let origin = twoFingerOrigin,
           hypot(center.x - origin.x, center.y - origin.y) >= 4 {
            firing = true
            shoot()
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
        let margin = min(size.width / 2, CGFloat(arcadeRules.ship_margin))
        player.position.x = min(size.width - margin, max(margin, x))
    }

    override func update(_ currentTime: TimeInterval) {
        guard running else { return }
        defer { if running { lastFrame = currentTime } }
        guard let lastFrame else { return }
        let delta = min(Double(arcadeRules.max_frame_ms) / 1000, max(0, currentTime - lastFrame))
        played = true
        spawnIn -= delta
        shotDelay = max(0, shotDelay - delta)
        shieldTime = max(0, shieldTime - delta)
        hitGrace = max(0, hitGrace - delta)
        shieldRing.position = player.position
        shieldRing.isHidden = shieldTime <= 0
        if mode == .shooter && firing && steeringTouches.count == 2 { shoot() }
        if spawnIn <= 0 { spawn() }
        animateVehicle(currentTime)

        if mode == .runner {
            updateRunner(delta)
        } else {
            updateFlight(delta)
        }
        pieces.removeAll { $0.parent == nil }
        shots.removeAll { $0.parent == nil }
        updateScenery()
        refreshHUD()
        if mode == .shooter && running {
            sinceSave += delta
            if sinceSave >= 1.8 { saveProgress() }
        }
    }

    private func updateRunner(_ delta: TimeInterval) {
        if let started = jumpElapsed {
            let elapsed = started + delta
            let height = ppf_arcade_jump_height(elapsed)
            player.position.y = floor + CGFloat(arcadeRules.runner_center) + CGFloat(height)
            jumpElapsed = elapsed > 0 && height <= 0 ? nil : elapsed
        }
        let distance = CGFloat(delta * ppf_arcade_runner_speed(score))
        for piece in pieces.reversed() {
            piece.position.x -= distance
            if piece.position.x < -CGFloat(arcadeRules.spawn_margin) {
                piece.removeFromParent()
            } else if collides(Int32(PPF_ARCADE_RUNNER), player.position, with: Int32(PPF_ARCADE_RUNNER_NUMBER), piece.position) {
                piece.removeFromParent()
                if piece.isPrime {
                    showPoints("Primo \(piece.number)", ppf_arcade_points(Int32(piece.number), true, true))
                } else {
                    finish()
                    break
                }
            } else if !piece.isPrime && ppf_arcade_runner_passed(Double(piece.position.x)) {
                showPoints("Pulou \(piece.number)", ppf_arcade_points(Int32(piece.number), false, false))
                piece.removeFromParent()
            }
        }
    }

    private func updateFlight(_ delta: TimeInterval) {
        let margin = CGFloat(arcadeRules.spawn_margin)
        for shot in shots.reversed() {
            shot.position.y += CGFloat(delta) * CGFloat(arcadeRules.shot_speed)
            if shot.position.y > size.height + margin {
                shot.removeFromParent()
                continue
            }
            if let hit = pieces.reversed().first(where: { piece in
                piece.parent != nil && collides(Int32(PPF_ARCADE_SHOT), shot.position,
                    with: spaceBox(for: piece), piece.position)
            }) {
                hit.removeFromParent()
                shot.removeFromParent()
                if hit.isPrime {
                    showPulse("Primo perdido")
                } else {
                    showPoints("Composto \(hit.number)", ppf_arcade_points(Int32(hit.number), false, false))
                }
            }
        }
        for piece in pieces.reversed() where piece.parent != nil {
            piece.position.y -= CGFloat(delta) * piece.speed
            if piece.position.y < -margin {
                piece.removeFromParent()
                continue
            }
            guard collides(Int32(PPF_ARCADE_SHIP), player.position, with: spaceBox(for: piece), piece.position) else {
                continue
            }
            piece.removeFromParent()
            switch ppf_arcade_flight_contact(piece.isPrime, shieldTime > 0, hitGrace > 0) {
            case Int32(PPF_ARCADE_CONTACT_RESCUE):
                showPoints("Resgate \(piece.number)", ppf_arcade_points(Int32(piece.number), true, true))
                shieldTime = Double(arcadeRules.shield_ms) / 1000
                shieldRing.isHidden = false
            case Int32(PPF_ARCADE_CONTACT_SHIELD):
                showPulse("Escudo ativo")
            case Int32(PPF_ARCADE_CONTACT_DAMAGE):
                lives -= 1
                hitGrace = Double(arcadeRules.hit_grace_ms) / 1000
                showPulse("Dano")
                if lives <= 0 {
                    finish()
                    return
                }
            default:
                break
            }
        }
    }

    private func spaceBox(for piece: PrimeArcadePiece) -> Int32 {
        Int32(piece.isPrime ? PPF_ARCADE_SPACE_PRIME : PPF_ARCADE_SPACE_COMPOSITE)
    }

    private func collides(_ firstKind: Int32, _ a: CGPoint, with secondKind: Int32, _ b: CGPoint) -> Bool {
        ppf_arcade_collides(firstKind, Double(a.x), Double(a.y), secondKind, Double(b.x), Double(b.y))
    }

    private func spawn() {
        let flight = mode == .shooter
        let draw = ppf_arcade_spawn(flight, score,
            UInt32.random(in: UInt32.min...UInt32.max), previousNumber)
        previousNumber = draw.number
        let piece = PrimeArcadePiece(number: Int(draw.number), prime: draw.prime,
                                    flight: flight, speed: CGFloat(draw.speed))
        if flight {
            let margin = min(size.width / 2, CGFloat(arcadeRules.ship_margin))
            let x = margin + (size.width - 2 * margin) * CGFloat(draw.lane) / CGFloat(PPF_ARCADE_LANE_MAX)
            piece.position = CGPoint(x: x, y: size.height + CGFloat(arcadeRules.spawn_margin))
        } else {
            piece.position = CGPoint(x: size.width + CGFloat(arcadeRules.spawn_margin),
                                     y: floor + CGFloat(arcadeRules.runner_center))
        }
        spawnIn = Double(draw.delay_ms) / 1000
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
        sinceSave = 0
        highScore = max(highScore, Int(score))
        defaults.set(highScore, forKey: mode.storageKey + "_best_score")
        if mode == .shooter {
            defaults.set(ended ? 0 : Int(score), forKey: mode.storageKey + "_last_score")
            defaults.set(ended ? Int(arcadeRules.lives) : lives, forKey: mode.storageKey + "_remaining_lives")
            defaults.set(ended ? 0 : Int((shieldTime * 1000).rounded(.down)),
                         forKey: mode.storageKey + "_remaining_shield_ms")
        }
    }
}
