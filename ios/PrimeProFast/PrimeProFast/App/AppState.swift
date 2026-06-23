import Foundation
import SwiftUI

final class AppState: ObservableObject {
    @Published var darkMode = false
    @Published var isPremium = false
    @Published var cancelRequested = false
    @Published var showDocumentPreview = false
    @Published var documentPreviewURL: URL?

    /// Rastreio de temporários (equivalente a `arquivosTemporarios` no Android).
    var temporaryResultFiles: [URL] = []

    let limiarBitsNativo = 8192

    func shouldUseNativeRoute(bits: Int) -> Bool {
        bits > limiarBitsNativo
    }
}
