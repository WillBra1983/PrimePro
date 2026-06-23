import SwiftUI

@main
struct PrimeProFastApp: App {
    @StateObject private var appState = AppState()

    init() {
        PPFResultFiles.purgeOldTemporaryResults()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(appState)
        }
    }
}
