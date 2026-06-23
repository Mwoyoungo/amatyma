import SwiftUI

/// One feed post, rendered by type (video poster / image carousel / text card).
/// Video playback (AVPlayer) + like/comment wiring land in the next pass — this
/// already pulls and renders real posts from the shared backend.
struct PostCardView: View {
    let post: VideoPost

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            authorRow

            if post.type == "text" {
                textBody
            } else {
                if !post.caption.isEmpty {
                    Text(post.caption)
                        .font(.system(size: 15.5))
                        .foregroundColor(Color(hex: 0xE3E6EC))
                        .padding(.horizontal, 16).padding(.vertical, 12)
                } else {
                    Spacer().frame(height: 12)
                }
                if post.type == "image" {
                    ImageCarousel(urls: post.imageUrls.isEmpty ? [post.posterUrl] : post.imageUrls)
                } else {
                    VideoPoster(posterUrl: post.posterUrl)
                }
            }

            actionRow
        }
        .padding(.top, 22).padding(.bottom, 6)
    }

    private var authorRow: some View {
        HStack(spacing: 11) {
            Avatar(url: post.authorAvatar, size: 46)
            VStack(alignment: .leading, spacing: 1) {
                Text(post.authorName.isEmpty ? "Amatyma" : post.authorName)
                    .font(.system(size: 16, weight: .semibold)).foregroundColor(.white)
                Text(post.authorHandle.isEmpty ? post.topic : post.authorHandle)
                    .font(.system(size: 13, weight: .medium)).foregroundColor(Ama.purple)
            }
            Spacer()
        }
        .padding(.horizontal, 16)
    }

    private var textBody: some View {
        Group {
            if post.bgColor.isEmpty {
                Text(post.caption)
                    .font(.system(size: 17, weight: .medium)).foregroundColor(.white)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 16).padding(.top, 6).padding(.bottom, 14)
            } else {
                Text(post.caption)
                    .font(.system(size: 20, weight: .bold)).foregroundColor(.white)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
                    .padding(28)
                    .background(RoundedRectangle(cornerRadius: 16).fill(Color(hexString: post.bgColor)))
                    .padding(.horizontal, 16).padding(.vertical, 12)
            }
        }
    }

    private var actionRow: some View {
        HStack(spacing: 24) {
            Label("\(post.kudosCount)", systemImage: "heart")
            Label("\(post.commentCount)", systemImage: "bubble.right")
            Spacer()
            Image(systemName: "paperplane")
        }
        .font(.system(size: 14, weight: .semibold))
        .foregroundColor(Ama.actionGray)
        .padding(.horizontal, 16).padding(.top, 14)
    }
}

private struct VideoPoster: View {
    let posterUrl: String
    var body: some View {
        ZStack {
            Color(hex: 0x0B0E15)
            AsyncImage(url: URL(string: posterUrl)) { img in
                img.resizable().scaledToFill()
            } placeholder: { Color(hex: 0x0B0E15) }
            Circle().fill(Color.black.opacity(0.42)).frame(width: 64, height: 64)
                .overlay(Image(systemName: "play.fill").font(.system(size: 26)).foregroundColor(.white))
        }
        .frame(maxWidth: .infinity)
        .aspectRatio(9.0/16.0, contentMode: .fit)
        .clipped()
    }
}

private struct ImageCarousel: View {
    let urls: [String]
    @State private var page = 0
    var body: some View {
        ZStack(alignment: .topTrailing) {
            TabView(selection: $page) {
                ForEach(Array(urls.enumerated()), id: \.offset) { idx, url in
                    AsyncImage(url: URL(string: url)) { img in
                        img.resizable().scaledToFill()
                    } placeholder: { Color(hex: 0x0B0E15) }
                    .tag(idx)
                    .clipped()
                }
            }
            .tabViewStyle(.page(indexDisplayMode: urls.count > 1 ? .automatic : .never))
            .aspectRatio(1, contentMode: .fit)

            if urls.count > 1 {
                Text("\(page + 1)/\(urls.count)")
                    .font(.system(size: 11, weight: .semibold)).foregroundColor(.white)
                    .padding(.horizontal, 10).padding(.vertical, 4)
                    .background(Capsule().fill(Color.black.opacity(0.5)))
                    .padding(12)
            }
        }
    }
}

private struct Avatar: View {
    let url: String
    let size: CGFloat
    var body: some View {
        Circle().fill(Ama.crimson)
            .frame(width: size, height: size)
            .overlay(
                AsyncImage(url: URL(string: url)) { img in
                    img.resizable().scaledToFill()
                } placeholder: { Ama.card }
                .clipShape(Circle())
                .padding(1.8)
            )
    }
}
