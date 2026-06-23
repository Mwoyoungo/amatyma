import SwiftUI

struct RootView: View {
    @EnvironmentObject var auth: AuthStore

    var body: some View {
        ZStack {
            Ama.black.ignoresSafeArea()
            if auth.uid == nil {
                AuthView()
            } else {
                FeedView()
            }
        }
    }
}
