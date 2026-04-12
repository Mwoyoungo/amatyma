# Amatyma - Setup Guide

## 🎯 What's Been Implemented

### ✅ Firebase Authentication
- Email/password signup and login
- Username validation (unique, 3-20 chars, lowercase)
- Profile setup with name and photo
- Firebase Storage for profile pictures
- Firestore for user data

### ✅ CometChat Integration
- Automatic user creation via Cloud Functions
- Auth token generation
- Seamless chat login after signup
- Voice/video calling enabled

### ✅ App Flow
```
Launch → Check Auth
  ├─ Not logged in → AuthActivity
  │                    ├─ Sign Up → ProfileSetupActivity → ConversationsActivity
  │                    └─ Login → ConversationsActivity
  └─ Logged in → ConversationsActivity
```

---

## 🚀 Next Steps to Run the App

### 1. **Sync Gradle**
- Open project in Android Studio
- Click "Sync Now" when prompted
- Wait for dependencies to download

### 2. **Enable Firebase Services** (In Firebase Console)

#### A. Authentication
1. Go to: https://console.firebase.google.com/project/amatyma-e75a8
2. Authentication → Sign-in method
3. Enable **Email/Password**

#### B. Firestore Database
1. Firestore Database → Create database
2. Start in **test mode** (we'll secure it later)
3. Location: Choose closest to you

#### C. Storage
1. Storage → Get started
2. Start in **test mode**

### 3. **Setup Cloud Functions**

#### A. Install Firebase CLI
```bash
npm install -g firebase-tools
firebase login
```

#### B. Initialize Functions
```bash
cd C:\amatyma-app
firebase init functions
```
- Select: `amatyma-e75a8`
- Language: JavaScript
- ESLint: Yes
- Install dependencies: Yes

#### C. Get CometChat Full Access API Key
1. Go to: https://app.cometchat.com/
2. Select your app
3. API & Auth Keys → API Keys
4. Copy the **Full Access API Key** (NOT Auth-Only)

#### D. Configure Environment
```bash
firebase functions:config:set cometchat.api_key="YOUR_FULL_ACCESS_API_KEY"
firebase functions:config:set cometchat.app_id="281421fd397d9bf6"
firebase functions:config:set cometchat.region="us"
```

#### E. Copy Functions Code
1. Copy content from `functions_index.js` to `functions/index.js`
2. Install axios:
```bash
cd functions
npm install axios
```

#### F. Deploy Functions
```bash
firebase deploy --only functions
```

**Note:** You may need to upgrade to Blaze plan (pay-as-you-go). Don't worry - it has a generous free tier!

### 4. **Build & Run**
1. Sync Gradle again
2. Build → Rebuild Project
3. Run on device/emulator

---

## 📝 Test Flow

### New User Signup
1. Open app → "Sign Up"
2. Enter:
   - Email: test@example.com
   - Username: testuser
   - Password: password123
3. Click "Sign Up"
4. Add profile photo (optional)
5. Enter display name: "Test User"
6. Click "NEXT"
7. Should navigate to Conversations screen

### Existing User Login
1. Open app → "Already have an account? Login"
2. Enter email and password
3. Click "Login"
4. Should navigate to Conversations screen

### Test Calling
1. Create 2 test users
2. Login with user1 on device 1
3. Login with user2 on device 2
4. Open conversation
5. Click voice/video call button
6. Call should connect!

---

## 🔐 Security Rules (Important!)

### Firestore Rules (Update after testing)
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      // Users can read all profiles but only write their own
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

### Storage Rules
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /users/{userId}/{allPaths=**} {
      // Users can only access their own files
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

---

## 🐛 Troubleshooting

### "Cloud function not found"
- Make sure functions are deployed: `firebase deploy --only functions`
- Check Firebase Console > Functions for any errors

### "Failed to create CometChat user"
- Verify API key is set correctly
- Check CometChat dashboard for API key permissions
- Look at Cloud Functions logs in Firebase Console

### APK too large
- Already configured for APK splits
- Build with: `./gradlew assembleDebug`
- Each architecture APK will be ~45-50MB

### Build errors
- Clean project: Build → Clean Project
- Invalidate caches: File → Invalidate Caches / Restart

---

## 📦 What's Next?

After getting this working, you can add:
1. Fitness tracking features
2. Activity sharing to groups
3. User profiles/settings
4. Notifications
5. Group challenges
6. Bottom navigation bar

---

## 📞 Support

If you encounter issues:
1. Check Firebase Console logs
2. Check Android Logcat
3. Verify all environment variables are set
4. Ensure Firebase services are enabled

---

**Good luck! 🚀**
