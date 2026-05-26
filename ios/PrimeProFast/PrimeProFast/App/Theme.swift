import SwiftUI

enum PPFTheme {
    static let primary = Color(red: 0.18, green: 0.49, blue: 0.20)
    static let accent = Color(red: 1.0, green: 0.42, blue: 0.42)
    static let cardBg = Color(red: 0.96, green: 0.97, blue: 0.98)

    static func background(_ dark: Bool) -> Color {
        dark ? Color(red: 0.11, green: 0.13, blue: 0.16) : Color(red: 0.95, green: 0.96, blue: 0.98)
    }

    static func textPrimary(_ dark: Bool) -> Color {
        dark ? Color.white : Color(red: 0.13, green: 0.13, blue: 0.13)
    }
}
