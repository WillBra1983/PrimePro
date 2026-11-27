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

private struct ArcadePhaseResult {
    let number: Int
    let earned: Int
    let target: Int
    let record: Int?
    let lives: Int?
    let passed: Bool
    let crashed: Bool
}

struct PrimeArcadeView: View {
    let mode: PrimeArcadeMode
    @State private var scene: PrimeArcadeScene
    @State private var phaseResult: ArcadePhaseResult?
    @State private var showPhases = false
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.dismiss) private var dismiss

    init(mode: PrimeArcadeMode) {
        self.mode = mode
        _scene = State(initialValue: PrimeArcadeScene(mode: mode))
    }

    var body: some View {
        ZStack {
            PrimeArcadeSurface(scene: scene)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .clipped()
                .contentShape(Rectangle())
                .accessibilityLabel(mode.title)
                .accessibilityHidden(phaseResult != nil)
            if let phaseResult {
                Color(red: 0.04, green: 0.08, blue: 0.12).opacity(0.82)
                resultPanel(phaseResult)
                    .padding(20)
                    .transition(.opacity)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(red: 0.06, green: 0.11, blue: 0.16).ignoresSafeArea())
        .navigationTitle(mode.title)
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button { dismiss() } label: { Image(systemName: "chevron.left") }
                    .accessibilityLabel("Voltar ao menu")
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    scene.suspend()
                    showPhases = true
                } label: { Image(systemName: "list.number") }
                    .accessibilityLabel("Fases")
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    phaseResult = nil
                    scene.retryPhase()
                } label: { Image(systemName: "arrow.counterclockwise") }
                    .accessibilityLabel("Reiniciar")
            }
        }
        .sheet(isPresented: $showPhases) {
            NavigationStack {
                List(0...scene.unlockedStage, id: \.self) { index in
                    let title = mode == .shooter && index == 50 ? "M51 · Defesa da Terra"
                        : mode == .shooter && index == 51 ? "M52 · Missão bônus" : "Fase \(index + 1)"
                    Button {
                        showPhases = false
                        phaseResult = nil
                        scene.selectPhase(index)
                    } label: {
                        if let best = scene.phaseRecord(index) {
                            LabeledContent(title, value: "Recorde \(best.formatted())")
                        } else {
                            Text(title)
                        }
                    }
                }
                .navigationTitle("Fases")
                .toolbar { Button("Fechar") { showPhases = false } }
            }
        }
        .onAppear {
            let result = $phaseResult
            scene.phaseReady = { value in
                withAnimation(.easeOut(duration: 0.20)) { result.wrappedValue = value }
            }
        }
        .onDisappear {
            scene.phaseReady = nil
            scene.suspend()
        }
        .onChange(of: scenePhase) { phase in
            if phase != .active { scene.suspend() }
        }
    }

    private func resultPanel(_ result: ArcadePhaseResult) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Label(result.passed ? "Fase \(result.number) concluída" : "Fase \(result.number) não superada",
                  systemImage: result.passed ? "flag.checkered" : "flag")
                .font(.title3.bold())
                .foregroundStyle(Color(red: 1, green: 0.86, blue: 0.57))
                .accessibilityAddTraits(.isHeader)
            if result.crashed { Text("Percurso interrompido").foregroundStyle(.secondary) }
            LabeledContent("Pontos na fase", value: result.earned.formatted())
            if result.lives == nil { LabeledContent("Mínimo", value: result.target.formatted()) }
            LabeledContent("Recorde da fase", value: result.record?.formatted() ?? "—")
            if let lives = result.lives { LabeledContent("Vidas", value: lives.formatted()) }
            if result.passed && (result.lives == nil || result.number < Int(PPF_ARCADE_CAMPAIGN_STAGES)) {
                Button {
                    phaseResult = nil
                    scene.advancePhase()
                } label: {
                    Label(result.lives != nil && result.number == 51 ? "Missão bônus" : "Próxima fase",
                          systemImage: "arrow.right")
                        .frame(maxWidth: .infinity, minHeight: 44)
                }
                .buttonStyle(.borderedProminent)
                .tint(Color(red: 0.08, green: 0.44, blue: 0.39))
            }
            Button {
                phaseResult = nil
                scene.retryPhase()
            } label: {
                Label("Rejogar fase", systemImage: "arrow.counterclockwise")
                    .frame(maxWidth: .infinity, minHeight: 44)
            }
            .buttonStyle(.bordered)
        }
        .monospacedDigit()
        .foregroundStyle(.white)
        .padding(20)
        .frame(maxWidth: 420)
        .background(Color(red: 0.09, green: 0.15, blue: 0.19),
                    in: RoundedRectangle(cornerRadius: 8))
    }
}

private struct PrimeArcadeSurface: UIViewRepresentable {
    let scene: PrimeArcadeScene

    func makeUIView(context: Context) -> SKView {
        let view = SKView()
        view.clipsToBounds = true
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
    let travelSpeed: CGFloat
    let role: Int32
    var nextAttack = 0.0
    var escortOffset: CGFloat?
    var loot: Int32 = 0
    var roadLevel: CGFloat = 0

    init(number: Int, prime: Bool, flight: Bool, travelSpeed: CGFloat, role: Int32) {
        self.number = number
        self.isPrime = prime
        self.travelSpeed = travelSpeed
        self.role = role
        super.init()
        let radius: CGFloat = flight ? (prime ? 22 : 20) : 26
        path = CGPath(ellipseIn: CGRect(x: -radius, y: -radius, width: radius * 2, height: radius * 2), transform: nil)
        fillColor = prime ? SKColor(red: 0.11, green: 0.69, blue: 0.49, alpha: 1)
                          : SKColor(red: 0.72, green: 0.20, blue: 0.29, alpha: 1)
        strokeColor = .white.withAlphaComponent(0.6)
        if role == Int32(PPF_ARCADE_ARMED) {
            strokeColor = .systemOrange
            lineWidth = 2
            glowWidth = 5
        } else if role == Int32(PPF_ARCADE_REPAIR) {
            fillColor = .systemBlue
        }
        let label = SKLabelNode(fontNamed: "AvenirNext-DemiBold")
        label.text = role == Int32(PPF_ARCADE_REPAIR) ? "+1" : String(number)
        label.fontSize = 17
        label.verticalAlignmentMode = .center
        addChild(label)
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) is unavailable") }

    func markLoot(_ kind: Int32) {
        loot = kind
        fillColor = kind == Int32(PPF_ARCADE_LOOT_SHIELD) ? .systemTeal : .systemBlue
        strokeColor = .cyan
        if let label = children.first as? SKLabelNode {
            label.text = kind == Int32(PPF_ARCADE_LOOT_LIFE) ? "+1" :
                kind == Int32(PPF_ARCADE_LOOT_PART) ? "PEÇA" :
                kind == Int32(PPF_ARCADE_LOOT_SHIELD) ? "ESCUDO" : "ARMA"
            label.fontSize = kind == Int32(PPF_ARCADE_LOOT_SHIELD) ? 9 : 12
        }
    }
}

private final class PrimeArcadeBolt: SKShapeNode {
    let velocity: CGVector
    let hostile: Bool
    var roadLevel: CGFloat = 0

    init(at center: CGPoint, velocity: CGVector, hostile: Bool, number: Int) {
        self.velocity = velocity
        self.hostile = hostile
        super.init()
        let rect: CGRect
        if hostile {
            let radius: CGFloat = velocity.dx < 0 && velocity.dy == 0 ? 13 : 10
            rect = CGRect(x: -radius, y: -radius, width: radius * 2, height: radius * 2)
        }
        else if velocity.dx != 0 { rect = CGRect(x: -12, y: -3, width: 24, height: 6) }
        else { rect = CGRect(x: -5, y: -10, width: 10, height: 29) }
        path = CGPath(roundedRect: rect, cornerWidth: hostile ? 10 : 3, cornerHeight: hostile ? 10 : 3, transform: nil)
        fillColor = hostile ? .systemOrange : .white
        strokeColor = hostile ? .systemRed : .cyan
        glowWidth = 3
        position = center
        if hostile {
            if velocity.dx < 0 && velocity.dy == 0 {
                let trail = SKShapeNode(rectOf: CGSize(width: 42, height: 5), cornerRadius: 2.5)
                trail.position.x = 24
                trail.fillColor = .systemOrange
                trail.strokeColor = .clear
                trail.zPosition = -1
                addChild(trail)
            }
            let label = SKLabelNode(fontNamed: "AvenirNext-DemiBold")
            label.text = String(number)
            label.fontSize = 12
            label.fontColor = .black
            label.verticalAlignmentMode = .center
            addChild(label)
        }
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) is unavailable") }
}

private final class PrimeArcadeScene: SKScene {
    private let mode: PrimeArcadeMode
    private let defaults = UserDefaults.standard
    private let player = SKShapeNode()
    private let hud = SKLabelNode(fontNamed: "AvenirNext-DemiBold")
    private let goalHUD = SKLabelNode(fontNamed: "AvenirNext-DemiBold")
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
    private let roadTree = PrimeArcadeScene.texture("road-tree", ext: "png")
    private let roadShade = SKSpriteNode(color: SKColor(red: 0.026, green: 0.071, blue: 0.16, alpha: 1), size: .zero)
    private let headlight = SKShapeNode()
    private var roadPhoto: SKSpriteNode?
    private var roadsideTrees: [SKSpriteNode] = []
    private var roadDashes: [[SKShapeNode]] = []
    private var roadDistance: CGFloat = 0
    private var roadTime: TimeInterval = 0
    private var previousRoadPosition = CGPoint.zero
    private var pieces: [PrimeArcadePiece] = []
    private var shots: [PrimeArcadeBolt] = []
    private let bossNode = SKNode()
    private let bossShip = SKSpriteNode()
    private let bossAura = SKShapeNode(ellipseOf: CGSize(width: 114, height: 78))
    private let bossCore = SKShapeNode(circleOfRadius: 12)
    private let bossBar = SKSpriteNode(color: .systemOrange, size: CGSize(width: 86, height: 6))
    private var bossHealth: Int32 = -1
    private var nextBossWave = 0.0
    private var bossStarted = 0.0
    private var bossGuarded: Bool {
        bossHealth > 0 && ppf_arcade_boss_guarded(Int32(stage),
            Int32(clamping: Int(max(0, combatClock - bossStarted) * 1000)),
            pieces.contains { $0.parent != nil && $0.escortOffset != nil })
    }
    private var wave = 0
    private var charge: Int32 = 0
    private var parts = 0
    private var weapon = 0
    private var destroyed = 0
    private var escortPrepared = false
    private var bossScale: CGFloat { CGFloat(ppf_arcade_boss_scale_percent(Int32(stage))) / 100 }
    private var combatClock = 0.0
    private var stars: [SKShapeNode] = []
    private static let zoneNames = ["Órbita azul", "Nebulosa", "Cinturão rochoso", "Gigante anelado", "Terra"]
    private static let zoneAnchors: [CGFloat] = [0.78, 0.82, 0.16, 0.035, 0.50]
    private lazy var zoneTextures = (0..<4).map { Self.texture("flight-zone-\($0)", ext: "jpg") }
    private var flightBackdrops: [SKSpriteNode] = []
    private var zoneBlend: CGFloat {
        min(1, max(0, CGFloat((phaseElapsed / phaseDuration - 0.44) / 0.17)))
    }
    private var planet: SKSpriteNode?
    private let earth = PrimeArcadeScene.texture("earth", ext: "png")
    private static let earthStage = Int(PPF_ARCADE_CAMPAIGN_STAGES) - 2
    private static let bonusStage = Int(PPF_ARCADE_CAMPAIGN_STAGES) - 1
    private var bossData: PPFArcadeBoss { ppf_arcade_boss(Int32(stage)) }
    private var bossName: String { "M\(stage + 1) · \(bossData.prefix)" + (stage >= 4 ? "∞" : "") }
    private var lastFrame: TimeInterval?
    private var spawnIn = Double(arcadeRules.spawn_start_ms) / 1000
    private var previousNumber: Int32 = 0
    private var played = false
    private var sinceSave: TimeInterval = 0
    private var shotDelay = 0.0
    private var shieldTime = 0.0
    private var shieldFlashUntil = 0.0
    private var hitGrace = 0.0
    private var steeringTouches: Set<UITouch> = []
    private var pilotTouch: UITouch?
    private var lastTouchCenter: CGPoint?
    private var firing = false
    private var pendingShot = false
    private var visualTier = -1
    private var stage = 0
    private var phaseBaseScore: Int32 = 0
    private var phaseElapsed: TimeInterval = 0
    private var finishedPhase: ArcadePhaseResult?
    private var exitOrigin = CGPoint.zero
    private var exitStartTime: TimeInterval?
    private var phaseShown = false
    private var autoAdvance = false
    var phaseReady: ((ArcadePhaseResult) -> Void)?
    private let sound = PrimeMatchSoundPlayer()
    private var score: Int32 = 0
    private var lives = Int(arcadeRules.lives)
    private var running = false
    private var ended = false
    private var installed = false
    private var highScore = 0
    private var flight: Bool { mode == .shooter }
    private var phaseDuration: TimeInterval {
        Double(flight ? ppf_arcade_flight_duration_ms(Int32(stage)) : ppf_arcade_phase_duration_ms(false)) / 1000
    }
    private var phasePoints: Int32 { max(0, score - phaseBaseScore) }
    private var pace: Int32 { Int32(min(25, stage) * (flight ? 600 : 40)) }
    var unlockedStage: Int {
        let stored = max(0, defaults.integer(forKey: mode.storageKey + (flight ? "_campaign_unlocked" : "_timed_unlocked")))
        return flight ? min(Self.bonusStage, stored) : stored
    }

    func phaseRecord(_ index: Int) -> Int? {
        defaults.object(forKey: mode.storageKey + "_timed_best_\(index)") as? Int
    }

    init(mode: PrimeArcadeMode) {
        self.mode = mode
        let names = mode == .runner ? ["motorcycle", "car", "car"] : ["airplane", "rocket", "spaceship"]
        vehicleTextures = names.map { Self.texture($0, ext: "png") }
        backdrop = Self.texture(mode == .runner ? "road" : "space", ext: "jpg")
        super.init(size: CGSize(width: 360, height: 420))
        scaleMode = .resizeFill
        highScore = max(0, defaults.integer(forKey: mode.storageKey + "_best_score"))
        if mode == .shooter, defaults.object(forKey: mode.storageKey + "_campaign_boss_hp") != nil {
            score = Int32(clamping: max(0, defaults.integer(forKey: mode.storageKey + "_last_score")))
            stage = min(unlockedStage, max(0, defaults.integer(forKey: mode.storageKey + "_timed_phase_index")))
            let storedBase = defaults.integer(forKey: mode.storageKey + "_timed_phase_base")
            phaseBaseScore = Int32(min(Int(score), max(0, storedBase)))
            phaseElapsed = min(phaseDuration, Double(max(0, defaults.integer(forKey: mode.storageKey + "_timed_elapsed"))) / 1000)
            let savedLives = defaults.object(forKey: mode.storageKey + "_remaining_lives") as? Int
            parts = min(12, max(0, defaults.integer(forKey: mode.storageKey + "_parts")))
            weapon = min(12, max(0, defaults.integer(forKey: mode.storageKey + "_weapon")))
            destroyed = max(0, defaults.integer(forKey: mode.storageKey + "_destroyed"))
            lives = min(Int(ppf_arcade_max_lives(Int32(parts))), max(0, savedLives ?? Int(arcadeRules.lives)))
            charge = Int32(clamping: max(0, min(100, defaults.integer(forKey: mode.storageKey + "_campaign_charge"))))
            let savedHealth = defaults.object(forKey: mode.storageKey + "_campaign_boss_hp") as? Int ?? -1
            bossHealth = max(-1, min(bossData.health, Int32(clamping: savedHealth)))
            nextBossWave = 1.45
            let savedShield = defaults.integer(forKey: mode.storageKey + "_remaining_shield_ms")
            shieldTime = Double(min(Int(PPF_ARCADE_PICKUP_SHIELD_MS), max(0, savedShield))) / 1000
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
        headlight.zPosition = -1
        headlight.fillColor = SKColor(red: 1, green: 0.92, blue: 0.65, alpha: 1)
        headlight.strokeColor = .clear
        headlight.isHidden = mode != .runner
        player.addChild(headlight)
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
        if flight {
            bossNode.zPosition = 2
            bossNode.isHidden = true
            bossAura.fillColor = .systemRed.withAlphaComponent(0.24)
            bossAura.strokeColor = .systemOrange
            bossShip.texture = vehicleTextures[2]
            bossShip.size = CGSize(width: 78, height: 80)
            bossShip.zRotation = .pi
            bossShip.color = .systemRed
            bossShip.colorBlendFactor = 0.35
            let track = SKSpriteNode(color: .systemRed, size: CGSize(width: 86, height: 6))
            track.position.y = -47
            bossBar.anchorPoint = CGPoint(x: 0, y: 0.5)
            bossBar.position = CGPoint(x: -43, y: -47)
            bossNode.addChild(bossAura)
            bossNode.addChild(bossShip)
            bossCore.fillColor = .systemOrange.withAlphaComponent(0.78)
            bossCore.strokeColor = .systemYellow
            bossCore.glowWidth = 7
            bossCore.zPosition = 1
            bossNode.addChild(bossCore)
            bossNode.addChild(track)
            bossNode.addChild(bossBar)
            field.addChild(bossNode)
            positionBoss()
        }
        positionPlayer()
        shieldRing.position = player.position
        updateScenery()

        hud.fontSize = 14
        hud.position = CGPoint(x: size.width / 2, y: size.height - 26)
        hud.zPosition = 5
        addChild(hud)
        goalHUD.fontSize = 12
        goalHUD.fontColor = SKColor(red: 0.65, green: 0.92, blue: 0.83, alpha: 1)
        goalHUD.zPosition = 5
        addChild(goalHUD)
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
        _ = completePhaseIfNeeded(crashed: flight && lives == 0, restored: true)
    }

    override func didChangeSize(_ oldSize: CGSize) {
        super.didChangeSize(oldSize)
        if installed {
            if oldSize.width > 0 && oldSize.height > 0 {
                let sx = size.width / oldSize.width, sy = size.height / oldSize.height
                player.position.x *= sx
                player.position.y *= sy
                for piece in pieces {
                    piece.position.x *= sx
                    piece.position.y *= sy
                    piece.roadLevel *= sy
                }
                for shot in shots {
                    shot.position.x *= sx
                    shot.position.y *= sy
                    shot.roadLevel *= sy
                }
            }
            layoutScene()
        }
    }

    private func layoutScene() {
        hud.position = CGPoint(x: size.width / 2, y: size.height - 25)
        hud.fontSize = size.width < 350 ? 11 : 14
        goalHUD.position = CGPoint(x: size.width / 2, y: size.height - 44)
        message.position = CGPoint(x: size.width / 2, y: size.height / 2)
        pulse.position = CGPoint(x: size.width / 2, y: size.height - 70)
        positionBoss()
        movePlayer(to: player.position)
        previousRoadPosition = player.position
        visualTier = -1
        updateScenery()
    }

    private func updateScenery() {
        let tier = Int(ppf_arcade_vehicle(Int32(stage)))
        if tier == visualTier {
            if mode == .runner { updateRoad() }
            else { moveStars() }
            return
        }
        visualTier = tier
        scenery.removeAllChildren()
        configureVehicle(tier)
        if mode == .runner {
            buildRoad()
            updateRoad()
            return
        }
        backgroundColor = SKColor(red: 0.035, green: 0.06, blue: 0.095, alpha: 1)
        let first = Int(ppf_arcade_flight_zone(Int32(stage), false))
        let second = Int(ppf_arcade_flight_zone(Int32(stage), true))
        flightBackdrops = (first == second ? [first] : [first, second]).enumerated().map { layer, zone in
            let texture = zone < zoneTextures.count ? (zoneTextures[zone] ?? backdrop) : backdrop
            let photo = SKSpriteNode(texture: texture, color: backgroundColor, size: size)
            if let texture {
                let image = texture.size()
                let scale = 1.12 * max(size.width / image.width, size.height / image.height)
                photo.size = CGSize(width: image.width * scale, height: image.height * scale)
            }
            photo.zPosition = CGFloat(-4 + layer)
            scenery.addChild(photo)
            return photo
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
        line.position = CGPoint(x: size.width / 2, y: CGFloat(arcadeRules.control_height))
        line.fillColor = SKColor(red: 0.08, green: 0.45, blue: 0.46, alpha: 1)
        line.strokeColor = .clear
        scenery.addChild(line)
        planet = nil
        if let earth, stage == Self.earthStage {
            let globe = SKSpriteNode(texture: earth)
            let width = size.width * (stage == Self.earthStage ? 1.30 : 0.32)
            globe.size = CGSize(width: width, height: width)
            globe.alpha = stage == Self.earthStage ? 0.84 : 0.44
            globe.zPosition = -1
            scenery.addChild(globe)
            planet = globe
        }
        stars = (0..<34).map { index in
            let dot = SKShapeNode(circleOfRadius: index.isMultiple(of: 5) ? 1.7 : 0.8)
            dot.fillColor = .white.withAlphaComponent(index.isMultiple(of: 5) ? 0.5 : 0.23)
            dot.strokeColor = .clear
            dot.position = CGPoint(x: CGFloat((index * 71) % 353) / 353 * size.width,
                                   y: 82 + CGFloat((index * 47) % 293) / 293 * max(0, size.height - 82))
            scenery.addChild(dot)
            return dot
        }
        moveStars()
    }

    private func moveStars() {
        for (index, photo) in flightBackdrops.enumerated() {
            photo.alpha = index == 0 ? 1 : zoneBlend
            let zone = Int(ppf_arcade_flight_zone(Int32(stage), index != 0))
            let anchor = Self.zoneAnchors[zone]
            let drift = 0.42 * min(anchor, 1 - anchor)
            let overflow = photo.size.width - size.width
            photo.position.x = photo.size.width / 2 - overflow * anchor + CGFloat(sin(combatClock / 18)) * overflow * drift
            photo.position.y = size.height / 2 - CGFloat(sin(combatClock / 23)) * (photo.size.height - size.height) * 0.24
        }
        for (index, star) in stars.enumerated() {
            let shift = CGFloat(index * 127) + CGFloat(combatClock * 1000) / CGFloat(7 + index % 13)
            star.position.y = size.height - shift.truncatingRemainder(dividingBy: max(1, size.height))
        }
        if let planet {
            let finale = stage == Self.earthStage
            let radius = planet.size.width / 2
            let shift = CGFloat(combatClock * 1000 / 57) + size.height * 0.26
            planet.position = CGPoint(x: size.width * (finale ? 0.67 : (stage.isMultiple(of: 2) ? 0.16 : 0.84)),
                                      y: finale ? size.height * 0.42 : size.height + radius -
                                        shift.truncatingRemainder(dividingBy: max(1, size.height + radius * 2)))
        }
    }

    private func laneY(_ lane: Int) -> CGFloat { size.height * (0.41 - 0.12 * CGFloat(lane)) }

    private func roadY(_ x: CGFloat, _ y: CGFloat, inverse: Bool = false) -> CGFloat {
        let along = max(-0.5, min(0.5, x / max(1, size.width) - 0.5))
        let center = size.height * 0.30
        let offset = size.height * 0.028 * along
        let factor = 1 - 0.38 * along
        if inverse { return center + (y - center - offset) / factor }
        return center + (y - center) * factor + offset
    }

    private func roadPolygon(far: CGFloat, near: CGFloat) -> CGPath {
        let path = CGMutablePath()
        path.move(to: CGPoint(x: 0, y: roadY(0, far)))
        path.addLine(to: CGPoint(x: size.width, y: roadY(size.width, far)))
        path.addLine(to: CGPoint(x: size.width, y: roadY(size.width, near)))
        path.addLine(to: CGPoint(x: 0, y: roadY(0, near)))
        path.closeSubpath()
        return path
    }

    private func buildRoad() {
        let horizon = size.height * 0.32
        backgroundColor = SKColor(red: 0.33, green: 0.45, blue: 0.50, alpha: 1)
        roadPhoto = nil
        if let backdrop {
            let sky = SKTexture(rect: CGRect(x: 0, y: 0.21, width: 1, height: 0.79), in: backdrop)
            let clip = SKCropNode()
            let visible = size.height - horizon
            let mask = SKSpriteNode(color: .white, size: CGSize(width: size.width, height: visible))
            mask.position = CGPoint(x: size.width / 2, y: horizon + visible / 2)
            clip.maskNode = mask
            let photo = SKSpriteNode(texture: sky)
            let dimensions = sky.size()
            let scale = max(size.width / dimensions.width, visible / dimensions.height)
            photo.size = CGSize(width: dimensions.width * scale, height: dimensions.height * scale)
            photo.anchorPoint = .zero
            clip.addChild(photo)
            scenery.addChild(clip)
            roadPhoto = photo
        }
        let shoulder = SKShapeNode(path: roadPolygon(far: size.height * 0.53, near: -size.height * 0.06))
        shoulder.fillColor = SKColor(red: 0.34, green: 0.39, blue: 0.27, alpha: 1)
        shoulder.strokeColor = .clear
        shoulder.zPosition = 0.5
        scenery.addChild(shoulder)
        roadsideTrees = []
        if let roadTree {
            roadsideTrees = (0..<18).map { index in
                let tree = SKSpriteNode(texture: roadTree)
                let depth = index / 6
                let width = CGFloat(56 + depth * 37 + (index * 31) % 36)
                tree.size = CGSize(width: width, height: width)
                tree.anchorPoint = .zero
                tree.zPosition = 1
                tree.alpha = CGFloat(155 + 50 * depth) / 255
                scenery.addChild(tree)
                return tree
            }
        }
        let asphalt = SKShapeNode(path: roadPolygon(far: size.height * 0.485, near: size.height * 0.035))
        asphalt.fillColor = SKColor(red: 0.22, green: 0.25, blue: 0.24, alpha: 1)
        asphalt.strokeColor = .clear
        asphalt.zPosition = 2
        scenery.addChild(asphalt)
        for ratio in [CGFloat(0.485), CGFloat(0.035)] {
            let edge = SKShapeNode()
            edge.path = roadLine(from: 0, to: size.width, level: size.height * ratio)
            edge.strokeColor = SKColor(red: 0.89, green: 0.83, blue: 0.64, alpha: 1)
            edge.lineWidth = 2
            edge.zPosition = 3
            scenery.addChild(edge)
        }
        roadDashes = (0..<2).map { lane in
            let stride = CGFloat(60 + lane * 22)
            return (0..<(Int(ceil(size.width / stride)) + 2)).map { _ in
                let dash = SKShapeNode()
                dash.strokeColor = .white.withAlphaComponent(0.68)
                dash.lineWidth = 2
                dash.zPosition = 3
                scenery.addChild(dash)
                return dash
            }
        }
        roadShade.size = size
        roadShade.position = CGPoint(x: size.width / 2, y: size.height / 2)
        roadShade.zPosition = 4
        scenery.addChild(roadShade)
    }

    private func roadLine(from: CGFloat, to: CGFloat, level: CGFloat) -> CGPath {
        let path = CGMutablePath()
        path.move(to: CGPoint(x: from, y: roadY(from, level)))
        path.addLine(to: CGPoint(x: to, y: roadY(to, level)))
        return path
    }

    private func updateRoad() {
        let horizon = size.height * 0.32
        if let photo = roadPhoto {
            let shift = CGFloat(0.50 + 0.42 * sin(Double(roadDistance) / 10500))
            photo.position = CGPoint(x: -(photo.size.width - size.width) * shift, y: horizon)
        }
        let gap = max(138, size.width / 3)
        let cycle = gap * 6
        for (index, tree) in roadsideTrees.enumerated() {
            let depth = CGFloat(index / 6)
            let speed = 0.12 + depth * 0.21
            let raw = (CGFloat(index % 6) * gap + depth * gap / 3 - roadDistance * speed).truncatingRemainder(dividingBy: cycle)
            let x = (raw + cycle).truncatingRemainder(dividingBy: cycle) - gap
            tree.position = CGPoint(x: x, y: roadY(x + tree.size.width / 2, size.height * (0.57 - depth * 0.025)))
        }
        for (lane, dashes) in roadDashes.enumerated() {
            let stride = CGFloat(60 + lane * 22)
            let shift = (roadDistance * (0.75 + CGFloat(lane) * 0.24)).truncatingRemainder(dividingBy: stride)
            let level = (laneY(lane) + laneY(lane + 1)) / 2
            for (index, dash) in dashes.enumerated() {
                let x = CGFloat(index) * stride - shift
                let end = x + stride * (0.48 - 0.14 * x / max(1, size.width))
                dash.path = roadLine(from: x, to: end, level: level)
            }
        }
        let phase = roadTime.truncatingRemainder(dividingBy: 56) / 56
        let night = min(1, max(0, (0.5 - 0.5 * cos(2 * .pi * phase) - 0.20) / 0.60))
        roadShade.alpha = CGFloat(night) * 0.76
        headlight.isHidden = night <= 0
        let beam = CGMutablePath()
        beam.move(to: CGPoint(x: 29, y: 0))
        beam.addLine(to: CGPoint(x: 140, y: -22))
        beam.addLine(to: CGPoint(x: 140, y: 22))
        beam.closeSubpath()
        headlight.path = beam
        headlight.alpha = CGFloat(night) * 0.20
    }

    private func configureVehicle(_ tier: Int) {
        let texture = vehicleTextures[tier]
        vehicleSprite.texture = texture
        vehicleSprite.setScale(flight ? 1 + CGFloat(parts) * 0.012 : 1)
        vehicleSprite.isHidden = texture == nil
        player.fillColor = texture == nil ? .systemTeal : .clear
        player.strokeColor = texture == nil ? .white : .clear
        guard let texture else { return }
        let source = texture.size()
        let scale = min((mode == .runner ? 82 : 72) / source.width,
                        (mode == .runner ? min(48, size.height * 0.106) : 70) / source.height)
        let w = source.width * scale, h = source.height * scale
        vehicleSprite.size = CGSize(width: w, height: h)
        vehicleSprite.position = .zero
        let positions: [(CGFloat, CGFloat, CGFloat)] = [(0.23, 0.81, 0.33), (0.18, 0.77, 0.30), (0.18, 0.77, 0.30)]
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
        let rotation = -(roadDistance / 9).truncatingRemainder(dividingBy: 2 * .pi)
        wheelMarks.forEach { $0.zRotation = rotation }
        for (index, puff) in exhaust.enumerated() {
            let life = ((mode == .runner ? roadTime : time) * 2.2 + Double(index) / 6).truncatingRemainder(dividingBy: 1)
            let p = CGFloat(life)
            puff.isHidden = false
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

    private func playEffect(_ name: String) {
        let enabled = defaults.object(forKey: "prime_match_sound_enabled") as? Bool ?? true
        sound.play(name, enabled: enabled)
    }

    @discardableResult
    private func completePhaseIfNeeded(crashed: Bool = false, restored: Bool = false) -> Bool {
        guard finishedPhase == nil else { return false }
        let elapsed = Int32((phaseElapsed * 1000).rounded(.down))
        let remainingLives = Int32(crashed ? 0 : lives)
        let outcome = flight ? ppf_arcade_flight_outcome(remainingLives, bossHealth)
            : ppf_arcade_phase_outcome(Int32(stage), false, elapsed, phasePoints, crashed)
        guard outcome != Int32(PPF_ARCADE_PHASE_RUNNING) else { return false }
        let passed = outcome == Int32(PPF_ARCADE_PHASE_PASSED)
        let bestKey = mode.storageKey + "_timed_best_\(stage)"
        if flight || !crashed, Int(phasePoints) > (phaseRecord(stage) ?? -1) {
            defaults.set(Int(phasePoints), forKey: bestKey)
        }
        if passed {
            let next = flight ? min(Self.bonusStage, stage + 1) : stage + 1
            defaults.set(max(unlockedStage, next), forKey: mode.storageKey + (flight ? "_campaign_unlocked" : "_timed_unlocked"))
            if flight && next > stage { storeKit(for: next) }
        }
        running = false
        firing = false
        steeringTouches.removeAll()
        rebaseSteering()
        message.text = ""
        exitOrigin = player.position
        exitStartTime = restored ? -1 : nil
        saveProgress()
        finishedPhase = ArcadePhaseResult(number: stage + 1, earned: Int(phasePoints),
                                         target: Int(ppf_arcade_phase_target(Int32(stage), flight)),
                                         record: phaseRecord(stage), lives: flight ? lives : nil,
                                         passed: passed, crashed: crashed)
        if passed && !restored {
            autoAdvance = true
            playEffect("ppf_pm_victory")
            if flight { showPulse("Fase \(stage + 1) concluída") }
        }
        return true
    }

    private func animatePhaseExit(_ time: TimeInterval, result: ArcadePhaseResult) {
        if exitStartTime == nil { exitStartTime = time }
        let start = exitStartTime ?? time
        let t = start < 0 ? 1 : min(1, max(0, (time - start) / 0.85))
        let ease = CGFloat(t * t * (3 - 2 * t))
        if !result.crashed {
            if mode == .runner {
                player.position.x = exitOrigin.x + (size.width + 94 - exitOrigin.x) * ease
            } else {
                player.position.y = exitOrigin.y + (size.height + 120 - exitOrigin.y) * ease
            }
        }
        shieldRing.position = player.position
        if t < 1 {
            animateVehicle(time)
        } else {
            player.isHidden = !result.crashed
            shieldRing.isHidden = true
            exhaust.forEach { $0.isHidden = true }
            field.isPaused = true
            if flight && result.passed && stage < Self.earthStage && autoAdvance {
                if start < 0 || time - start >= 1.45 { advancePhase() }
            } else if !phaseShown, let phaseReady {
                phaseShown = true
                phaseReady(result)
            }
        }
    }

    func advancePhase() {
        guard finishedPhase?.passed == true, !flight || stage < Self.bonusStage else { return }
        preparePhase(stage + 1, base: score)
    }

    func retryPhase() {
        preparePhase(stage, base: phaseBaseScore)
    }

    func selectPhase(_ index: Int) {
        guard (0...unlockedStage).contains(index) else { return }
        preparePhase(index, base: 0, start: false)
    }

    private func showPulse(_ label: String) {
        pulse.removeAllActions()
        pulse.text = label
        pulse.alpha = 1
        pulse.run(.sequence([.wait(forDuration: 0.65), .fadeOut(withDuration: 0.3)]))
    }

    private func beginIfNeeded() {
        guard installed, finishedPhase == nil else { return }
        if !running {
            running = true
            field.isPaused = false
            lastFrame = nil
            message.text = ""
            if flight && phaseElapsed == 0 {
                if stage == 0 { showPulse("Defesa digital da Terra") }
                else if stage == Self.earthStage { showPulse("Terra  ·  Defesa final") }
            }
        }
    }

    private func shoot() {
        if shotDelay <= 0 {
            let offset = CGFloat(arcadeRules.shot_offset)
            let velocity = flight ? CGVector(dx: 0, dy: CGFloat(arcadeRules.shot_speed)) : CGVector(dx: 490, dy: 0)
            let startX = player.position.x + (flight ? 0 : 33)
            let startY = flight ? player.position.y + offset
                : roadY(startX, roadY(player.position.x, player.position.y, inverse: true))
            let start = CGPoint(x: startX, y: startY)
            launch(at: start, velocity: velocity, hostile: false)
            pendingShot = false
            shotDelay = flight ? Double(max(105, Int(arcadeRules.shot_cooldown_ms) - 6 * weapon)) / 1000 : 0.24
        }
    }

    private func launch(at center: CGPoint, velocity: CGVector, hostile: Bool, number: Int = 4) {
        let shot = PrimeArcadeBolt(at: center, velocity: velocity, hostile: hostile, number: number)
        if !flight { shot.roadLevel = roadY(center.x, center.y, inverse: true) }
        field.addChild(shot)
        shots.append(shot)
    }

    private func preparePhase(_ index: Int, base: Int32, start: Bool = true) {
        guard installed else { return }
        suspend()
        score = base
        stage = index
        phaseBaseScore = base
        phaseElapsed = 0
        finishedPhase = nil
        phaseShown = false
        exitStartTime = nil
        player.isHidden = false
        lives = Int(arcadeRules.lives)
        if flight {
            let loadout = kit(for: stage)
            parts = loadout[0]
            weapon = loadout[1]
            lives = loadout[2]
        }
        destroyed = 0
        escortPrepared = false
        ended = false
        pieces.forEach { $0.removeFromParent() }
        shots.forEach { $0.removeFromParent() }
        pieces.removeAll()
        shots.removeAll()
        spawnIn = Double(arcadeRules.spawn_start_ms) / 1000
        previousNumber = 0
        bossHealth = -1
        bossStarted = 0
        bossNode.isHidden = true
        wave = 0
        charge = 0
        combatClock = 0
        visualTier = -1
        roadTime = Double(index % 2) * 28
        roadDistance = 0
        children.filter { $0.name == "roadBurst" }.forEach { $0.removeFromParent() }
        played = false
        sinceSave = 0
        shotDelay = 0
        shieldTime = 0
        shieldFlashUntil = 0
        hitGrace = 0
        shieldRing.isHidden = true
        pulse.removeAllActions()
        pulse.alpha = 0
        exhaust.forEach { $0.isHidden = true }
        positionPlayer()
        updateScenery()
        setPrompt()
        refreshHUD()
        if start { beginIfNeeded() }
        saveProgress()
    }

    func suspend() {
        running = false
        autoAdvance = false
        lastFrame = nil
        field.isPaused = true
        steeringTouches.removeAll()
        pendingShot = false
        lastTouchCenter = nil
        firing = false
        saveProgress()
        if !ended && finishedPhase == nil { setPrompt() }
    }

    private func positionPlayer() {
        player.removeAllActions()
        movePlayer(to: CGPoint(x: mode == .runner ? size.width * 0.30 : size.width / 2,
                               y: mode == .runner ? roadY(size.width * 0.30, laneY(1)) : CGFloat(arcadeRules.ship_y)))
        previousRoadPosition = player.position
    }

    private func setPrompt() {
        message.text = (played || phaseElapsed > 0) ? "Toque para continuar" : "Toque para começar"
    }

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard finishedPhase == nil else { return }
        beginIfNeeded()
        let previousCount = steeringTouches.count
        steeringTouches.formUnion(touches)
        rebaseSteering()
        if flight && previousCount < 2 && firing {
            pendingShot = true
            shoot()
            playEffect("ppf_pm_tap")
        }
    }

    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard running, let center = steeringCenter(), let previous = lastTouchCenter else { return }
        let targetX = player.position.x + center.x - previous.x
        let targetY = flight ? player.position.y + center.y - previous.y
            : roadY(targetX, roadY(player.position.x, player.position.y, inverse: true) + center.y - previous.y)
        movePlayer(to: CGPoint(x: targetX, y: targetY))
        lastTouchCenter = center
    }

    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        endSteering(touches)
    }

    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        pendingShot = false
        endSteering(touches)
    }

    private func endSteering(_ touches: Set<UITouch>) {
        steeringTouches.subtract(touches)
        rebaseSteering()
    }

    private func rebaseSteering() {
        if flight, pilotTouch.map({ steeringTouches.contains($0) }) != true {
            pilotTouch = steeringTouches.min(by: { $0.timestamp < $1.timestamp })
        }
        lastTouchCenter = steeringCenter()
        firing = steeringTouches.count == 2
    }

    private func steeringCenter() -> CGPoint? {
        guard !steeringTouches.isEmpty else { return nil }
        if flight { return pilotTouch?.location(in: self) }
        let total = steeringTouches.reduce(CGPoint.zero) { sum, touch in
            let point = touch.location(in: self)
            return CGPoint(x: sum.x + point.x, y: sum.y + point.y)
        }
        let count = CGFloat(steeringTouches.count)
        return CGPoint(x: total.x / count, y: total.y / count)
    }

    private func movePlayer(to target: CGPoint) {
        let minX: CGFloat
        let maxX: CGFloat
        let minY: CGFloat
        let maxY: CGFloat
        if mode == .runner {
            minX = min(size.width * 0.30, 44)
            maxX = size.width * 0.64
            minY = laneY(2)
            maxY = laneY(0)
        } else {
            minX = min(size.width / 2, CGFloat(arcadeRules.ship_margin))
            maxX = size.width - minX
            let top = min(size.height / 2, 92)
            let bossLimit = bossHealth > 0 ? min(size.height * 0.58, size.height - bossNode.position.y + 55) : top
            maxY = size.height - max(top, bossLimit)
            minY = min(maxY, CGFloat(arcadeRules.control_height) + 36)
        }
        let x = min(maxX, max(minX, target.x))
        let level = flight ? target.y : roadY(target.x, target.y, inverse: true)
        let y = min(maxY, max(minY, level))
        player.position = CGPoint(x: x, y: flight ? y : roadY(x, y))
        shieldRing.position = player.position
    }

    override func update(_ currentTime: TimeInterval) {
        if let finishedPhase {
            animatePhaseExit(currentTime, result: finishedPhase)
            return
        }
        guard running else { return }
        defer { if running { lastFrame = currentTime } }
        guard let lastFrame else { return }
        let frame = min(Double(arcadeRules.max_frame_ms) / 1000, max(0, currentTime - lastFrame))
        let delta = flight && bossHealth >= 0 ? frame : min(max(0, phaseDuration - phaseElapsed), frame)
        phaseElapsed = min(phaseDuration, phaseElapsed + delta)
        combatClock += delta
        played = true
        spawnIn -= delta
        shotDelay = max(0, shotDelay - delta)
        shieldTime = max(0, shieldTime - delta)
        hitGrace = max(0, hitGrace - delta)
        shieldRing.position = player.position
        shieldRing.isHidden = shieldTime <= 0
        let shieldFlashing = combatClock < shieldFlashUntil
        shieldRing.strokeColor = shieldFlashing ? .white : .cyan
        shieldRing.glowWidth = shieldFlashing ? 10 : 5
        if firing || pendingShot { shoot() }
        if spawnIn <= 0 && (!flight || bossHealth < 0) { spawn() }
        animateVehicle(currentTime)

        if mode == .runner {
            updateRunner(delta)
        } else {
            updateFlight(delta)
            if lives > 0 && bossHealth < 0 && phaseElapsed >= phaseDuration - 0.00001 { startBoss() }
        }
        pieces.removeAll { $0.parent == nil }
        shots.removeAll { $0.parent == nil }
        _ = completePhaseIfNeeded()
        updateScenery()
        refreshHUD()
        if mode == .shooter && running {
            sinceSave += delta
            if sinceSave >= 1.8 { saveProgress() }
        }
    }

    private func updateRunner(_ delta: TimeInterval) {
        roadTime += delta
        let distance = CGFloat(delta * ppf_arcade_runner_speed(pace))
        roadDistance += distance
        for shot in shots where shot.parent != nil {
            if !advance(shot, delta: delta) { continue }
            if shot.hostile {
                if near(shot.position, player.position, x: 25, y: 12) {
                    shot.removeFromParent()
                    roadBurst(at: player.position, prime: false)
                    showPulse("Impacto: projétil composto")
                    playEffect("ppf_pm_composite")
                    _ = completePhaseIfNeeded(crashed: true)
                    return
                }
            } else if let hit = pieces.reversed().first(where: { $0.parent != nil && near(shot.position, $0.position, x: 25, y: 15) }) {
                shot.removeFromParent()
                hit.removeFromParent()
                roadBurst(at: hit.position, prime: hit.isPrime)
                if hit.isPrime {
                    showPulse("Primo perdido")
                } else {
                    showPoints("Composto \(hit.number)", ppf_arcade_points(Int32(hit.number), false, false))
                    playEffect("ppf_pm_combo")
                }
            }
        }
        for piece in pieces.reversed() where piece.parent != nil {
            let armed = piece.role == Int32(PPF_ARCADE_ARMED)
            let travel = distance * (armed ? 1.23 : 1)
            piece.position.x -= travel
            if armed {
                let desired = roadY(player.position.x, player.position.y, inverse: true) + CGFloat(sin(combatClock / 0.46)) * 17
                let step = CGFloat(delta) * 44
                piece.roadLevel = min(laneY(0), max(laneY(2), piece.roadLevel + min(step, max(-step, desired - piece.roadLevel))))
                if combatClock >= piece.nextAttack, piece.position.x < size.width - 8,
                   piece.position.x > player.position.x + max(108, size.width * 0.27) {
                    let x = piece.position.x - 23
                    launch(at: CGPoint(x: x, y: roadY(x, piece.roadLevel)),
                           velocity: CGVector(dx: -310, dy: 0), hostile: true, number: piece.number)
                    piece.nextAttack = combatClock + 1.65
                }
            }
            piece.position.y = roadY(piece.position.x, piece.roadLevel)
            if ppf_arcade_road_hit(Double(previousRoadPosition.x - travel),
                                   Double(roadY(previousRoadPosition.x, previousRoadPosition.y, inverse: true)),
                                   Double(player.position.x), Double(roadY(player.position.x, player.position.y, inverse: true)),
                                   Double(piece.position.x), Double(piece.roadLevel), Double(size.height)) {
                roadBurst(at: piece.position, prime: piece.isPrime)
                piece.removeFromParent()
                if piece.isPrime {
                    showPoints("Primo \(piece.number)", ppf_arcade_points(Int32(piece.number), true, true))
                    playEffect("ppf_pm_prime")
                } else {
                    showPulse("Composto \(piece.number)")
                    playEffect("ppf_pm_composite")
                    _ = completePhaseIfNeeded(crashed: true)
                    break
                }
            } else if piece.position.x < -34 {
                if !piece.isPrime {
                    showPoints("Desviou \(piece.number)", ppf_arcade_points(Int32(piece.number), false, false))
                }
                piece.removeFromParent()
            }
        }
        previousRoadPosition = player.position
    }

    private func roadBurst(at center: CGPoint, prime: Bool, scale: CGFloat = 1) {
        let parent = SKNode()
        parent.name = "roadBurst"
        parent.position = center
        parent.setScale(scale)
        parent.zPosition = 4
        addChild(parent)
        let color = prime ? SKColor(red: 1, green: 0.85, blue: 0.51, alpha: 1)
                          : SKColor(red: 1, green: 0.50, blue: 0.40, alpha: 1)
        let ring = SKShapeNode(circleOfRadius: 8)
        ring.strokeColor = color
        ring.lineWidth = 2.5
        parent.addChild(ring)
        ring.run(.group([.scale(to: 7.6, duration: 0.42), .fadeOut(withDuration: 0.42)]))
        for index in 0..<14 {
            let angle = Double(index) * .pi / 7
            let distance = Double(16 + (index * 29) % 53)
            let spark = SKShapeNode(rectOf: CGSize(width: 11, height: 2), cornerRadius: 1)
            spark.fillColor = color
            spark.strokeColor = .clear
            spark.zRotation = CGFloat(angle)
            parent.addChild(spark)
            let motion = SKAction.move(to: CGPoint(x: CGFloat(cos(angle) * distance),
                                                   y: CGFloat(sin(angle) * distance)), duration: 0.42)
            motion.timingMode = .easeOut
            spark.run(.group([motion, .fadeOut(withDuration: 0.42), .scale(to: 0.3, duration: 0.42)]))
        }
        parent.run(.sequence([.wait(forDuration: 0.43), .removeFromParent()]))
    }

    private func updateFlight(_ delta: TimeInterval) {
        let margin = CGFloat(arcadeRules.spawn_margin)
        if bossHealth > 0 { updateBoss() }
        collectBeforeShots(delta)
        for shot in shots.reversed() where shot.parent != nil {
            if !advance(shot, delta: delta) { continue }
            if shot.hostile {
                let dx = Double(shot.position.x - player.position.x)
                let dy = Double(shot.position.y - player.position.y)
                if shieldTime > 0 && ppf_arcade_shield_intercepts(dx, dy) {
                    shot.removeFromParent()
                    shieldFlashUntil = combatClock + 0.2
                    showPulse("Tiro bloqueado")
                    playEffect("ppf_pm_power")
                } else if near(shot.position, player.position, x: 24, y: 26) {
                    shot.removeFromParent()
                    takeFlightHit()
                    if lives <= 0 { return }
                }
                continue
            }
            if let escort = pieces.first(where: {
                $0.parent != nil && $0.escortOffset != nil && near(shot.position, $0.position, x: 25, y: 25)
            }) {
                shot.removeFromParent()
                destroy(escort)
                continue
            }
            if bossHealth > 0 && near(shot.position, bossNode.position, x: 53 * bossScale, y: 34 * bossScale) {
                shot.removeFromParent()
                if bossGuarded || abs(shot.position.x - bossNode.position.x) >= 22 * bossScale { continue }
                let previousHealth = bossHealth
                bossHealth = max(0, bossHealth - ppf_arcade_upgrade_damage(charge, Int32(weapon)))
                dropBossLoot(afterHitFrom: previousHealth)
                if bossHealth == 0 {
                    roadBurst(at: bossNode.position, prime: true, scale: 2.1)
                    playEffect("ppf_pm_composite")
                    bossNode.isHidden = true
                    _ = completePhaseIfNeeded()
                    return
                }
                positionBoss()
                continue
            }
            if let hit = pieces.reversed().first(where: { piece in
                piece.parent != nil && piece.loot == 0 && piece.escortOffset == nil &&
                    collides(Int32(PPF_ARCADE_SHOT), shot.position,
                    with: spaceBox(for: piece), piece.position)
            }) {
                shot.removeFromParent()
                destroy(hit)
            }
        }
        for piece in pieces.reversed() where piece.parent != nil {
            if piece.escortOffset != nil { continue }
            piece.position.y -= CGFloat(delta) * piece.travelSpeed
            if piece.role == Int32(PPF_ARCADE_ARMED) && combatClock >= piece.nextAttack
                && piece.position.y < size.height - 55 && piece.position.y > player.position.y + 84 {
                launch(at: CGPoint(x: piece.position.x, y: piece.position.y - 21),
                       velocity: CGVector(dx: 0, dy: -260), hostile: true, number: piece.number)
                piece.nextAttack = combatClock + 1.6
            }
            if piece.position.y < -margin {
                piece.removeFromParent()
                continue
            }
            guard !piece.isPrime, piece.loot == 0,
                  collides(Int32(PPF_ARCADE_SHIP), player.position, with: spaceBox(for: piece), piece.position) else {
                continue
            }
            piece.removeFromParent()
            takeFlightHit()
            if lives <= 0 { return }
        }
    }

    private func collectBeforeShots(_ delta: TimeInterval) {
        for piece in pieces.reversed() where piece.parent != nil && (piece.isPrime || piece.loot > 0) {
            let next = CGPoint(x: piece.position.x, y: piece.position.y - CGFloat(delta) * piece.travelSpeed)
            guard collides(Int32(PPF_ARCADE_SHIP), player.position, with: spaceBox(for: piece), next) else { continue }
            piece.removeFromParent()
            if piece.loot > 0 {
                collect(piece.loot)
            } else {
                showPoints("Resgate \(piece.number)", ppf_arcade_points(Int32(piece.number), true, true))
                charge = ppf_arcade_add_charge(charge, Int32(piece.number))
                shieldTime = max(shieldTime, Double(arcadeRules.shield_ms) / 1000)
                shieldRing.isHidden = false
                playEffect("ppf_pm_prime")
            }
        }
    }

    private func kit(for level: Int) -> [Int] {
        let stored = defaults.array(forKey: mode.storageKey + "_kit_\(level)") as? [Int] ?? []
        return stored.count == 3 ? stored : [0, 0, Int(arcadeRules.lives)]
    }

    private func storeKit(for level: Int) {
        let previous = kit(for: level)
        let bestParts = max(previous[0], parts)
        let bestLives = defaults.object(forKey: mode.storageKey + "_kit_\(level)") == nil ? lives : max(previous[2], lives)
        defaults.set([bestParts, max(previous[1], weapon), min(Int(ppf_arcade_max_lives(Int32(bestParts))), bestLives)],
                     forKey: mode.storageKey + "_kit_\(level)")
    }

    private func destroy(_ piece: PrimeArcadePiece) {
        piece.removeFromParent()
        if piece.isPrime { showPulse("Primo perdido"); return }
        showPoints("Composto \(piece.number)", ppf_arcade_points(Int32(piece.number), false, false))
        playEffect("ppf_pm_combo")
        roadBurst(at: piece.position, prime: false)
        destroyed += 1
        let kind = ppf_arcade_flight_loot(Int32(stage), Int32(clamping: destroyed))
        guard kind != 0 else { return }
        spawnLoot(kind, at: piece.position, speed: 82)
    }

    private func spawnLoot(_ kind: Int32, at point: CGPoint, speed: CGFloat) {
        let pickup = PrimeArcadePiece(number: 0, prime: false, flight: true, travelSpeed: speed, role: 0)
        pickup.markLoot(kind)
        pickup.position = point
        pieces.append(pickup)
        field.addChild(pickup)
    }

    private func dropBossLoot(afterHitFrom before: Int32) {
        let drops = ppf_arcade_boss_drops(before, bossHealth, bossData.health)
        for mark in 0..<3 where (drops & (1 << mark)) != 0 {
            let kind = mark == 1 ? Int32(PPF_ARCADE_LOOT_LIFE) : Int32(PPF_ARCADE_LOOT_SHIELD)
            let x = bossNode.position.x + CGFloat(mark - 1) * 32
            let point = CGPoint(x: max(22, min(size.width - 22, x)), y: bossNode.position.y - 52 * bossScale)
            spawnLoot(kind, at: point, speed: 112)
        }
    }

    private func collect(_ kind: Int32) {
        if kind == Int32(PPF_ARCADE_LOOT_SHIELD) {
            shieldTime = max(shieldTime, Double(PPF_ARCADE_PICKUP_SHIELD_MS) / 1000)
            shieldRing.isHidden = false
            showPulse("Escudo reforçado")
        } else if kind == Int32(PPF_ARCADE_LOOT_LIFE) {
            lives = min(Int(ppf_arcade_max_lives(Int32(parts))), lives + 1)
            showPulse("Vidas: \(lives)")
        } else if kind == Int32(PPF_ARCADE_LOOT_PART) {
            parts = min(12, parts + 1)
            vehicleSprite.setScale(1 + CGFloat(parts) * 0.012)
            showPulse("Peças: \(parts)")
        } else {
            weapon = min(12, weapon + 1)
            showPulse("Arma: nível \(weapon)")
        }
        playEffect("ppf_pm_power")
        saveProgress()
    }

    private func near(_ a: CGPoint, _ b: CGPoint, x: CGFloat, y: CGFloat) -> Bool {
        abs(a.x - b.x) < x && abs(a.y - b.y) < y
    }

    private func advance(_ shot: PrimeArcadeBolt, delta: TimeInterval) -> Bool {
        shot.position.x += CGFloat(delta) * shot.velocity.dx
        if flight { shot.position.y += CGFloat(delta) * shot.velocity.dy }
        else { shot.position.y = roadY(shot.position.x, shot.roadLevel) }
        if shot.position.x < -36 || shot.position.x > size.width + 36 ||
            shot.position.y < -36 || shot.position.y > size.height + 36 {
            shot.removeFromParent()
            return false
        }
        return true
    }

    private func takeFlightHit() {
        switch ppf_arcade_flight_contact(false, shieldTime > 0, hitGrace > 0) {
        case Int32(PPF_ARCADE_CONTACT_SHIELD):
            showPulse("Escudo ativo")
            playEffect("ppf_pm_power")
        case Int32(PPF_ARCADE_CONTACT_DAMAGE):
            lives -= 1
            hitGrace = Double(arcadeRules.hit_grace_ms) / 1000
            showPulse("Dano")
            playEffect("ppf_pm_composite")
            if lives <= 0 { _ = completePhaseIfNeeded(crashed: true) }
        default: break
        }
    }

    private func positionBoss() {
        guard bossHealth > 0 else { return }
        bossNode.isHidden = false
        bossNode.setScale(bossScale)
        let edge = min(size.width / 2, 57 * bossScale)
        let orbit = size.width * CGFloat(0.50 + 0.27 * sin(combatClock / 1.32))
        bossNode.position = CGPoint(x: min(size.width - edge, max(edge, orbit)),
                                    y: size.height - min(size.height * 0.30, 145))
        bossBar.xScale = CGFloat(bossHealth) / CGFloat(bossData.health)
        let guarded = bossGuarded
        bossCore.isHidden = guarded
        bossBar.color = guarded ? .cyan : .systemOrange
        bossAura.strokeColor = guarded ? .cyan : .systemOrange
        bossAura.fillColor = (guarded ? SKColor.cyan : SKColor.systemRed).withAlphaComponent(0.24)
        bossAura.alpha = guarded || nextBossWave - combatClock < 0.32 ? 1 : 0.45
    }

    private func startBoss() {
        bossHealth = bossData.health
        bossStarted = combatClock
        nextBossWave = combatClock + 1.45
        pieces.forEach { $0.removeFromParent() }
        shots.forEach { $0.removeFromParent() }
        pieces.removeAll()
        shots.removeAll()
        positionBoss()
        movePlayer(to: player.position)
        showPulse(bossName)
        saveProgress()
    }

    private func updateBoss() {
        positionBoss()
        if !escortPrepared {
            escortPrepared = true
            let count = Int(ppf_arcade_boss_escorts(Int32(stage)))
            for i in 0..<count {
                let escort = PrimeArcadePiece(number: 4 + i * 2, prime: false, flight: true,
                                             travelSpeed: 0, role: Int32(PPF_ARCADE_ARMED))
                escort.escortOffset = CGFloat(i) - CGFloat(count - 1) / 2
                if let texture = vehicleTextures[2] {
                    let ship = SKSpriteNode(texture: texture)
                    ship.size = CGSize(width: 66, height: 45)
                    ship.zRotation = .pi
                    ship.zPosition = -1
                    escort.addChild(ship)
                }
                escort.setScale(0.78)
                escort.zPosition = 2
                escort.nextAttack = combatClock + 1.6 + Double(i) * 0.38
                pieces.append(escort)
                field.addChild(escort)
            }
        }
        for escort in pieces where escort.parent != nil {
            guard let offset = escort.escortOffset else { continue }
            escort.position.x = max(19, min(size.width - 19, bossNode.position.x + offset * 54 +
                CGFloat(sin(combatClock * 2.3 + Double(offset))) * 22))
            escort.position.y = bossNode.position.y - 52 * bossScale - 15 - abs(offset) * 8
            if combatClock >= escort.nextAttack {
                let aim = max(-110, min(110, (player.position.x - escort.position.x) * 0.7))
                launch(at: escort.position, velocity: CGVector(dx: aim, dy: -245), hostile: true, number: escort.number)
                escort.nextAttack = combatClock + max(1.15, 2.25 - Double(stage) * 0.018)
            }
        }
        guard combatClock >= nextBossWave else { return }
        let speed = CGFloat(230 + min(stage, 45) * 2)
        let compound = [4, 6, 8, 9, 10, 12][wave % 6]
        if bossData.pattern == 1 {
            for lane in 0..<4 where lane != wave % 4 {
                launch(at: CGPoint(x: size.width * (CGFloat(lane) + 0.5) / 4, y: bossNode.position.y),
                       velocity: CGVector(dx: 0, dy: -speed), hostile: true, number: compound)
            }
        } else {
            let aim = bossData.pattern == 2 || wave % 2 == 1
                ? max(-135, min(135, (player.position.x - bossNode.position.x) * 1.2)) : 0
            for tile in -1...1 {
                launch(at: CGPoint(x: bossNode.position.x, y: bossNode.position.y - 22),
                       velocity: CGVector(dx: aim + CGFloat(tile) * 88, dy: -speed), hostile: true, number: compound)
            }
        }
        wave += 1
        nextBossWave = combatClock + max(0.8, 1.55 - Double(stage) * 0.013)
    }

    private func spaceBox(for piece: PrimeArcadePiece) -> Int32 {
        Int32(piece.isPrime ? PPF_ARCADE_SPACE_PRIME : PPF_ARCADE_SPACE_COMPOSITE)
    }

    private func collides(_ firstKind: Int32, _ a: CGPoint, with secondKind: Int32, _ b: CGPoint) -> Bool {
        ppf_arcade_collides(firstKind, Double(a.x), Double(a.y), secondKind, Double(b.x), Double(b.y))
    }

    private func spawn() {
        let flight = mode == .shooter
        let draw = ppf_arcade_spawn(flight, pace,
            UInt32.random(in: UInt32.min...UInt32.max), previousNumber)
        previousNumber = draw.number
        let role = ppf_arcade_enemy_role(draw.prime, flight, Int32(stage), UInt32.random(in: UInt32.min...UInt32.max))
        let piece = PrimeArcadePiece(number: Int(draw.number), prime: draw.prime,
                                    flight: flight, travelSpeed: CGFloat(draw.speed), role: role)
        piece.nextAttack = combatClock + (flight ? 1.2 : 0.3)
        if flight {
            let margin = min(size.width / 2, CGFloat(arcadeRules.ship_margin))
            let x = margin + (size.width - 2 * margin) * CGFloat(draw.lane) / CGFloat(PPF_ARCADE_LANE_MAX)
            piece.position = CGPoint(x: x, y: size.height + CGFloat(arcadeRules.spawn_margin))
        } else {
            piece.roadLevel = laneY(Int(draw.lane))
            piece.position = CGPoint(x: size.width + 34, y: roadY(size.width + 34, piece.roadLevel))
            let radius = min(21, size.height * 0.049)
            piece.path = CGPath(ellipseIn: CGRect(x: -radius, y: -radius, width: radius * 2, height: radius * 2), transform: nil)
            if role == Int32(PPF_ARCADE_ARMED), let texture = vehicleTextures[2] {
                let car = SKSpriteNode(texture: texture)
                car.size = CGSize(width: 70, height: 34)
                car.xScale = -1
                car.zPosition = -1
                car.color = .systemOrange
                car.colorBlendFactor = 0.35
                piece.addChild(car)
            }
        }
        spawnIn = Double(draw.delay_ms) / 1000
        pieces.append(piece)
        field.addChild(piece)
    }

    private func refreshHUD() {
        let points = Int(phasePoints)
        let target = ppf_arcade_phase_target(Int32(stage), mode == .shooter)
        let remaining = Int(ceil(max(0, phaseDuration - phaseElapsed - 0.00001)))
        goalHUD.text = "\(remaining) s   •   Meta \(target.formatted())"
        if mode == .runner {
            hud.text = "Pontos \(points)   •   Fase \(stage + 1)"
        } else {
            let shield = shieldTime > 0 ? " · Escudo" : ""
            hud.text = "Pontos \(points)   •   Vidas \(lives)   •   Setor \(stage + 1)"
            let zone = Int(ppf_arcade_flight_zone(Int32(stage), zoneBlend >= 0.5))
            let objective = bossHealth >= 0 ? bossName + (bossGuarded ? " · Blindado" : "") : Self.zoneNames[zone]
            goalHUD.text = "\(objective) · Carga \(charge)% · Arma \(weapon)\(shield)"
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
            defaults.set(stage, forKey: mode.storageKey + "_timed_phase_index")
            defaults.set(Int(phaseBaseScore), forKey: mode.storageKey + "_timed_phase_base")
            defaults.set(Int((phaseElapsed * 1000).rounded()), forKey: mode.storageKey + "_timed_elapsed")
            defaults.set(Int(charge), forKey: mode.storageKey + "_campaign_charge")
            defaults.set(Int(bossHealth), forKey: mode.storageKey + "_campaign_boss_hp")
            defaults.set(parts, forKey: mode.storageKey + "_parts")
            defaults.set(weapon, forKey: mode.storageKey + "_weapon")
            defaults.set(destroyed, forKey: mode.storageKey + "_destroyed")
        }
    }
}
