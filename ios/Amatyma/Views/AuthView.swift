import SwiftUI

/// Minimal Firebase email/password sign-in so the app can read the live feed.
/// (Google sign-in + onboarding parity with Android comes next.)
struct AuthView: View {
    @EnvironmentObject var auth: AuthStore
    @State private var email = ""
    @State private var password = ""

    var body: some View {
        VStack(spacing: 18) {
            Spacer()

            ZStack {
                Circle().fill(Ama.crimson).frame(width: 64, height: 64)
                Text("A").font(.system(size: 32, weight: .heavy)).foregroundColor(.white)
            }
            Text("Amatyma").font(.system(size: 26, weight: .bold)).foregroundColor(.white)
            Text("Men's Wellness Community").font(.system(size: 14)).foregroundColor(Ama.textSecondary)

            VStack(spacing: 12) {
                field("Email", text: $email, secure: false)
                field("Password", text: $password, secure: true)
            }
            .padding(.top, 12)

            if let error = auth.error {
                Text(error).font(.system(size: 12)).foregroundColor(Ama.crimson)
                    .multilineTextAlignment(.center)
            }

            Button {
                auth.signIn(email: email, password: password)
            } label: {
                ZStack {
                    RoundedRectangle(cornerRadius: 14).fill(Ama.crimson)
                    if auth.working {
                        ProgressView().tint(.white)
                    } else {
                        Text("Sign in").font(.system(size: 16, weight: .semibold)).foregroundColor(.white)
                    }
                }
                .frame(height: 52)
            }
            .disabled(auth.working)

            Spacer()
        }
        .padding(.horizontal, 28)
    }

    @ViewBuilder
    private func field(_ placeholder: String, text: Binding<String>, secure: Bool) -> some View {
        Group {
            if secure {
                SecureField("", text: text, prompt: Text(placeholder).foregroundColor(Ama.textSecondary))
            } else {
                TextField("", text: text, prompt: Text(placeholder).foregroundColor(Ama.textSecondary))
                    .textInputAutocapitalization(.never)
                    .keyboardType(.emailAddress)
            }
        }
        .foregroundColor(.white)
        .padding(.horizontal, 16)
        .frame(height: 50)
        .background(RoundedRectangle(cornerRadius: 12).fill(Color.white.opacity(0.06)))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Ama.hairline, lineWidth: 1))
    }
}
