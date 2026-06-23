import SwiftUI

struct FeedView: View {
    @EnvironmentObject var auth: AuthStore
    @StateObject private var feed = FeedStore()

    var body: some View {
        VStack(spacing: 0) {
            header
            if feed.posts.isEmpty {
                Spacer()
                VStack(spacing: 6) {
                    Text("No posts yet").font(.system(size: 17, weight: .bold)).foregroundColor(.white)
                    Text("Be the first to share with the brotherhood.")
                        .font(.system(size: 13)).foregroundColor(Ama.textSecondary)
                }
                Spacer()
            } else {
                ScrollView {
                    LazyVStack(spacing: 0) {
                        ForEach(feed.posts) { post in
                            PostCardView(post: post)
                        }
                    }
                    .padding(.bottom, 24)
                }
            }
        }
        .background(Ama.black.ignoresSafeArea())
        .onAppear { feed.start() }
        .onDisappear { feed.stop() }
    }

    private var header: some View {
        HStack(spacing: 9) {
            ZStack {
                Circle().fill(Ama.crimson).frame(width: 34, height: 34)
                Text("A").font(.system(size: 17, weight: .heavy)).foregroundColor(.white)
            }
            Text("Amatyma").font(.system(size: 19, weight: .bold)).foregroundColor(.white)
            Spacer()
            Button { auth.signOut() } label: {
                Image(systemName: "rectangle.portrait.and.arrow.right")
                    .foregroundColor(Ama.textSecondary)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
    }
}
