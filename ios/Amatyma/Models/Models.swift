import Foundation

/// A feed post — video, image (carousel), or text. Mirrors the Android `VideoPost`
/// (the Firestore `videoPosts` collection with a `type` field).
struct VideoPost: Identifiable, Equatable {
    let id: String
    let authorUid: String
    let authorName: String
    let authorHandle: String
    let authorAvatar: String
    let caption: String
    let topic: String
    let type: String        // "video" | "image" | "text"
    let hlsUrl: String
    let posterUrl: String
    let imageUrls: [String]
    let bgColor: String
    let kudosCount: Int
    let commentCount: Int
}

/// The signed-in person, projected for the social UI. Mirrors Android `SocialUser`.
struct SocialUser: Equatable {
    let uid: String
    let name: String
    let handle: String      // "@username" or ""
    let avatarUrl: String
}
