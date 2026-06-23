import SwiftUI
import FirebaseCore

@main
struct AmatymaApp: App {
    @StateObject private var auth = AuthStore()

    init() {
        FirebaseApp.configure()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(auth)
                .preferredColorScheme(.dark)
        }
    }
}
