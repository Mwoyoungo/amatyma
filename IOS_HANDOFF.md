# Amatyma — iOS (Swift) Port Handoff

This document maps the app so the SwiftUI/iOS version can be built cleanly, with a
**hard separation** between the **Social** side (Firebase-native) and the **Chat**
side (CometChat). The entire backend is platform-agnostic — iOS reuses it as-is.

---

## 1. The big picture

```
                ┌──────────────────────── Firebase (shared backend) ─────────────────────────┐
                │  Auth · Firestore · Storage · Cloud Functions · FCM/APNs · (FastPix for video)│
                └───────────────▲───────────────────────────────────────────▲──────────────────┘
                                │                                            │
              ┌─────────────────┴──────────────┐          ┌──────────────────┴─────────────────┐
              │   SOCIAL (Firebase-native)      │          │   CHAT (CometChat UIKit + VoIP)     │
              │   feed, reels, stories, posts,  │          │   conversations, messages, calls    │
              │   likes, follows, search, push  │          │   (CometChat owns its own data)     │
              └─────────────────────────────────┘          └─────────────────────────────────────┘
```

**One identity, two product surfaces.** Firebase Auth is the single source of identity.
Social is built directly on Firebase. Chat is CometChat, **bridged** to the same
Firebase user. They share only: (a) the Firebase Auth UID, (b) the `users/{uid}`
Firestore doc (read-only from Social), and (c) the FCM/APNs token array.

> **Golden rule (carried over from Android):** the Social side must NEVER touch
> CometChat objects, its login/session, or its FCM/VoIP handling. CometChat is only
> ever disconnected on an explicit logout.

---

## 2. SOCIAL side — 100% Firebase, no CometChat

### Android source (to be re-implemented in SwiftUI)
All under `app/src/main/java/com/lokaleza/amatyma/social/`:

| File | Role | SwiftUI equivalent |
|------|------|--------------------|
| `SocialShell.kt` | Nav host + bottom bar + story-viewer overlay | Root `TabView` + overlays |
| `FeedScreen.kt` | Feed (For You/Following, topic chips, story tray, post cards: video/image/text) | `FeedView` |
| `ReelsScreen.kt` | Full-screen vertical video pager (video-only) | `ReelsView` (paged `TabView` + `AVPlayer`) |
| `CreateScreen.kt` | New post — Video / Photo(multi) / Text tabs | `CreatePostView` |
| `CreateVideoViewModel.kt`, `VideoUploader.kt`, `GcsResumableUploader.kt` | FastPix video upload | `VideoUploader` (URLSession) |
| `Posts.kt` | Create image/text posts | `PostService` |
| `Stories.kt`, `StoryTray.kt`, `StoryViewer.kt`, `CreateStory.kt` | Stories (text+image, 24h, followed+own) | `StoriesStore`, `StoryTrayView`, `StoryViewerView` |
| `ProfileScreen.kt`, `EditProfile.kt` | Profile + edit (name/bio/avatar) | `ProfileView`, `EditProfileView` |
| `SearchScreen.kt` | People + post search | `SearchView` |
| `Channels.kt`, `Alerts.kt` | Topics, in-app notifications feed | `ChannelsView`, `AlertsView` |
| `CommentsSheet.kt` | Comments + threaded replies | `CommentsSheet` |
| `Likes.kt`, `Following.kt` | App-wide stores (optimistic toggle + Firestore listener) | `ObservableObject` singletons |
| `SocialIdentity.kt` | Projects the signed-in user; overlays `profiles/{uid}` on the read-only `users/{uid}` | `IdentityStore` |
| `VideoFeed.kt`, `VideoPlayer.kt` | Feed VM + player | `FeedStore`, player view |

### Firestore collections (Social)
- `videoPosts/{postId}` — ALL feed posts. Field **`type`** = `video` | `image` | `text` (legacy docs = video). `status` must be `ready` to show. Video = `hlsUrl`/`posterUrl` (server-created via FastPix). Image = `imageUrls[]` + `posterUrl` (client-created). Text = `caption` + optional `bgColor` (client-created).
  - `…/comments/{commentId}` → `…/replies/{replyId}` (threaded)
- `profiles/{uid}` — public stats (`followersCount`, `followingCount`) trigger-maintained, + social overrides (`displayName`, `bio`, `photoURL`) the owner may edit.
- `users/{uid}/following/{targetUid}` — who I follow
- `users/{uid}/followers/{followerUid}` — reverse index (for new-post fan-out)
- `users/{uid}/likes/{postId}` — my likes (single source of truth; trigger keeps `kudosCount`)
- `users/{uid}/notifications/{id}` — in-app Alerts feed (trigger-written)
- `users/{uid}/storiesSeen/{storyId}` — story seen-state
- `socialStories/{storyId}` — 24h ephemeral stories (`type` text/image/video)

### Storage paths (Social — separate from chat/business)
- `social/{uid}/avatar.jpg` — social avatar (NOT the chat `users/{uid}/profile.jpg`)
- `social_stories/{uid}/…` — story images
- `social_posts/{uid}/{postId}/{i}.jpg` — multi-image post media

### Cloud Functions (Social) — `functions/index.js`
- `createFastpixUpload` (callable), `fastpixWebhook` (onRequest) — FastPix video pipeline
- Triggers: `onLikeCreated/Deleted`, `onCommentCreated/Deleted`, `onReplyCreated/Deleted`, `onFollowCreated/Deleted`, `onVideoPostReady` (fan-out new posts to followers)
- Helpers: `writeNotification` (writes Alert **and** calls `sendPush`), `sendPush` (FCM → reads `users/{uid}.fcmTokens`), `getActor`, `pushTextFor`

### FastPix (video) — same REST flow on iOS
1. `createFastpixUpload` → returns a signed upload URL + creates a `videoPosts` doc (status `processing`).
2. Client PUTs the file to the signed URL (resumable; Android uses `GcsResumableUploader`, iOS = `URLSession` upload).
3. `fastpixWebhook` receives `video.upload.media_created` (carries `playbackId`) → sets `hlsUrl = stream.fastpix.io/{playbackId}.m3u8`, `posterUrl = images.fastpix.io/{playbackId}/thumbnail.jpg`, status `ready`.

---

## 3. CHAT side — CometChat (do NOT reimplement; integrate the SDK)

Chat is the existing CometChat UIKit experience. On iOS, use the **CometChat iOS UI Kit**
(there are `cometchat-ios*` skills available) rather than rebuilding it.

### Android source (parent package `com/lokaleza/amatyma/`)
- `MainActivity.kt` — hosts both surfaces; owns the CometChat lifecycle
- `AmatymaApplication.kt` — CometChat init
- `AmatymaFirebaseMessagingService.kt` — single FCM service, routes by `data.type`
- `CometChatVoIPManager.kt`, `CallConnection*.kt`, `OngoingCallActivity.kt` — VoIP/Telecom
- `MessagesActivity.kt`, `Conversations*Fragment.kt`, `Groups*`, `UserDetailsActivity.kt` — chat UI
- `CometChatSyncManager.kt`, `ChatRepository.kt` — local sync

---

## 4. ⭐ How CometChat integrates (the bridge)

This is the critical part for the iOS port — replicate this exactly.

1. **Identity = Firebase Auth.** User signs in with Firebase (email/Google). The Firebase
   **UID** is also the **CometChat UID**.
2. **CometChat login** (in `MainActivity.ensureCometChatLogin` / `loginToCometChat`):
   - Try `CometChatUIKit.login(uid)` directly.
   - On failure → call the **`getCometChatAuthToken`** Cloud Function (mints a server-side
     CometChat auth token) → `CometChatUIKit.login(withAuthToken:)`.
   - User provisioning is handled by **`createCometChatUser`** Cloud Function.
3. **Shared profile doc.** `users/{uid}` (Firestore) holds `displayName`, `username`,
   `photoURL` — written during chat onboarding. **Social reads this doc read-only** and
   overlays its own fields from `profiles/{uid}` (so editing the social profile never
   mutates chat identity).
4. **Push (one channel, routed).** A single FCM/APNs handler routes by `data.type`:
   - `new_message` → CometChat message notification
   - `call` → CometChat VoIP (CallKit/PushKit on iOS)
   - `social` → Social activity channel (likes/comments/follows/replies/new posts)

   The same `users/{uid}.fcmTokens` array feeds both. `sendPush` (Social) is already
   platform-agnostic — Firebase delivers to iOS via APNs automatically.
5. **Logout** clears caches → `CometChat.logout()` → `Auth.signOut()` → back to Auth screen.
   CometChat is disconnected ONLY here.

---

## 5. What iOS reuses verbatim (no changes)
- **All Firestore collections, Storage paths, Cloud Functions, security rules, FastPix pipeline.** The backend is platform-agnostic. iOS just needs the Firebase iOS SDK pointed at the same project.

## 6. iOS port — what to build
- **Social (SwiftUI):** rebuild the screens above using Firebase iOS SDK (Auth/Firestore/Storage/Functions), `AVPlayer` for HLS, `PhotosPicker` for multi-image, `URLSession` for the FastPix upload. The stores (`Likes`/`Following`/`Stories`/`Identity`) become `ObservableObject` singletons with Firestore snapshot listeners (same logic as Kotlin).
- **Chat (CometChat iOS UI Kit):** integrate via the `cometchat-ios` skills. Replicate the bridge in §4 (Firebase Auth → `getCometChatAuthToken` → CometChat login). Use CallKit/PushKit for VoIP.
- **Push:** register the APNs token into `users/{uid}.fcmTokens`; route by `data.type` exactly as Android does.

## 7. iOS setup checklist
- [ ] Add the iOS app in the Firebase console → download **`GoogleService-Info.plist`** (the iOS counterpart of `google-services.json`).
- [ ] Firebase iOS SDK (Auth, Firestore, Storage, Functions, Messaging).
- [ ] CometChat iOS UI Kit pod + App ID/Region/Auth-Key (same CometChat app as Android).
- [ ] APNs key/cert uploaded to Firebase (for FCM→APNs) and to CometChat (for call/message push).
- [ ] FastPix: reuse the same Cloud Functions; no client keys needed (upload URL is server-minted).
- [ ] PhotosPicker / camera usage strings in Info.plist.

> ⚠️ **Security:** rotate the previously-shared creds before launch (FastPix secret, FastPix
> signing key, GitHub PAT). `app/google-services.json` is committed — fine for a private repo;
> avoid making the repo public with it in history.
