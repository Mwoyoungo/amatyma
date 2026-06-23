import SwiftUI

/// Brand palette mirroring the Android `social.ui.theme` colors.
enum Ama {
    static let black = Color(hex: 0x0A0D14)
    static let crimson = Color(hex: 0xC2185B)
    static let crimsonTint = Color(hex: 0xC2185B).opacity(0.14)
    static let purple = Color(hex: 0xC693F0)
    static let textPrimary = Color.white
    static let textSecondary = Color(hex: 0x9AA3B2)
    static let hairline = Color.white.opacity(0.08)
    static let card = Color(hex: 0x11151D)
    static let actionGray = Color(hex: 0xC8CED9)
}

extension Color {
    init(hex: UInt32) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255.0,
            green: Double((hex >> 8) & 0xFF) / 255.0,
            blue: Double(hex & 0xFF) / 255.0,
            opacity: 1.0
        )
    }

    /// Parse a "#RRGGBB" string (used for text-post backgrounds). Falls back to a dark card.
    init(hexString: String, fallback: Color = Ama.card) {
        let s = hexString.trimmingCharacters(in: CharacterSet(charactersIn: "#"))
        if let v = UInt32(s, radix: 16), s.count == 6 {
            self.init(hex: v)
        } else {
            self = fallback
        }
    }
}
