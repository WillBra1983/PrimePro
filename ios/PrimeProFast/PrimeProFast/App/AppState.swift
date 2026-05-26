import Foundation
import SwiftUI

final class AppState: ObservableObject {
    @Published var darkMode = false
    @Published var isPremium = false
    @Published var cancelRequested = false

    let limiarBitsNativo = 8192

    func shouldUseNativeRoute(bits: Int) -> Bool {
        bits > limiarBitsNativo
    }
}
