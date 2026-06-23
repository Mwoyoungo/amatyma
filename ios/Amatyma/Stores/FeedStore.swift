import Foundation
import FirebaseFirestore

/// Live feed of ready posts. Listens to `videoPosts` newest-first and filters to
/// `status == "ready"` client-side (mirrors the Android `VideoFeedViewModel`).
@MainActor
final class FeedStore: ObservableObject {
    @Published var posts: [VideoPost] = []

    private var listener: ListenerRegistration?

    func start() {
        guard listener == nil else { return }
        listener = Firestore.firestore().collection("videoPosts")
            .order(by: "createdAt", descending: true)
            .limit(to: 50)
            .addSnapshotListener { [weak self] snapshot, _ in
                guard let docs = snapshot?.documents else { return }
                self?.posts = docs.compactMap { FeedStore.map($0) }
            }
    }

    func stop() {
        listener?.remove()
        listener = nil
    }

    static func map(_ doc: QueryDocumentSnapshot) -> VideoPost? {
        let x = doc.data()
        guard (x["status"] as? String) == "ready" else { return nil }
        return VideoPost(
            id: doc.documentID,
            authorUid: x["authorUid"] as? String ?? "",
            authorName: x["authorName"] as? String ?? "",
            authorHandle: x["authorHandle"] as? String ?? "",
            authorAvatar: x["authorAvatar"] as? String ?? "",
            caption: x["caption"] as? String ?? "",
            topic: x["topic"] as? String ?? "",
            type: x["type"] as? String ?? "video",
            hlsUrl: x["hlsUrl"] as? String ?? "",
            posterUrl: x["posterUrl"] as? String ?? "",
            imageUrls: x["imageUrls"] as? [String] ?? [],
            bgColor: x["bgColor"] as? String ?? "",
            kudosCount: (x["kudosCount"] as? Int) ?? 0,
            commentCount: (x["commentCount"] as? Int) ?? 0
        )
    }
}
