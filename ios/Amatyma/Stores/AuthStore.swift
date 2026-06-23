import Foundation
import FirebaseAuth

/// Firebase Auth gate. Same identity that powers chat (CometChat) and social.
@MainActor
final class AuthStore: ObservableObject {
    @Published var uid: String?
    @Published var working = false
    @Published var error: String?

    private var handle: AuthStateDidChangeListenerHandle?

    init() {
        uid = Auth.auth().currentUser?.uid
        handle = Auth.auth().addStateDidChangeListener { [weak self] _, user in
            Task { @MainActor in self?.uid = user?.uid }
        }
    }

    func signIn(email: String, password: String) {
        guard !email.isEmpty, !password.isEmpty else { return }
        working = true
        error = nil
        Auth.auth().signIn(withEmail: email, password: password) { [weak self] _, err in
            Task { @MainActor in
                self?.working = false
                self?.error = err?.localizedDescription
            }
        }
    }

    func signOut() {
        try? Auth.auth().signOut()
    }

    deinit {
        if let handle { Auth.auth().removeStateDidChangeListener(handle) }
    }
}
