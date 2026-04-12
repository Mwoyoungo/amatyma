# Amatyma Web Chat - Setup Guide

A lightweight web chat application built with React + Vite that integrates with CometChat and Firebase.

## Features

- **Complete Authentication System**
  - Sign Up with email, username, and password
  - Login with email and password
  - Forgot Password / Password Reset
  - Profile Setup (name + optional photo)
- **Firebase Integration**
  - Email/Password authentication
  - Firestore database for user data
  - Cloud Storage for profile photos
  - Cloud Functions for CometChat user creation
- **CometChat Messaging**
  - Pre-built UI components
  - Real-time conversations
  - Cross-platform support (Android ↔️ Web)
- **Clean, Modern UI**
  - Gradient design matching Android app
  - Responsive layout
  - Loading states & error handling

## Prerequisites

- Node.js 18+ installed
- Existing Amatyma account (created via Android app)

## Setup & Installation

1. **Navigate to the web directory:**
   ```bash
   cd web/web
   ```

2. **Install dependencies:**
   ```bash
   npm install
   ```

3. **Start development server:**
   ```bash
   npm run dev
   ```

4. **Open in browser:**
   - Navigate to: http://localhost:5173
   - Sign in with your Amatyma credentials

## Build for Production

```bash
npm run build
npm run preview
```

## Usage

### For New Users:
1. Open http://localhost:5174 in your browser
2. Click "Don't have an account? Sign Up"
3. Enter your email, choose a username (3-20 chars, lowercase), and password
4. Complete your profile with your name and optional photo
5. Start chatting!

### For Existing Users:
1. Open http://localhost:5174 in your browser
2. Enter your email and password
3. Click "Login"
4. Start chatting!

### Forgot Password:
1. Click "Forgot Password?" on the login screen
2. Enter your email
3. Check your email for the reset link
4. Follow the link to set a new password

## Technology Stack

- **React 19** - UI framework
- **Vite** - Build tool & dev server
- **TypeScript** - Type safety
- **Firebase Authentication** - User authentication
- **CometChat UI Kit 5.x** - Pre-built chat components

## Configuration

All configurations are pre-configured and located in:
- `src/firebase.ts` - Firebase initialization (App ID, Auth domain, etc.)
- `src/cometchat.ts` - CometChat initialization (App ID, Region, Auth Key)

## Project Structure

```
src/
├── components/
│   ├── Login.tsx           # Login component
│   ├── Login.css           # Login styles
│   ├── ChatInterface.tsx   # Main chat interface
│   └── ChatInterface.css   # Chat styles
├── firebase.ts             # Firebase config
├── cometchat.ts            # CometChat config & utilities
├── App.tsx                 # Main app component
├── App.css                 # App styles
├── main.tsx                # Entry point
└── index.css               # Global styles
```

## Key Features

### Authentication Flow

**Sign Up:**
1. User enters email, username, and password
2. System checks if username is available
3. Firebase creates the account
4. User data saved to Firestore
5. User redirected to Profile Setup
6. User enters display name and optional photo
7. Cloud Function creates CometChat user
8. Auto-login to CometChat
9. Redirect to chat interface

**Login:**
1. User enters email and password
2. Firebase authenticates the user
3. System checks if profile is complete
   - If complete: Auto-login to CometChat → Chat interface
   - If incomplete: Redirect to Profile Setup
4. Chat interface loads with all conversations

**Forgot Password:**
1. User clicks "Forgot Password?"
2. Enters email address
3. Firebase sends password reset email
4. User clicks link in email
5. Sets new password
6. Can now login with new password

### Chat Interface
- **Pre-built CometChat component** (`CometChatConversationsWithMessages`)
- Shows conversation list on the left
- Message thread on the right
- Real-time message updates
- User presence indicators
- Typing indicators
- Read receipts
- Voice/video calling support (via CometChat)

### Cross-Platform Support
- Users created on Android can log in on web
- Conversations sync in real-time across all platforms
- Same CometChat backend as Android app

## Notes

- This web app shares the same Firebase & CometChat backend as the Android app
- Only chat functionality is included (no posts, stories, shop, events, etc.)
- All conversations are synced in real-time
- Users maintain the same identity across platforms

## Troubleshooting

### "Failed to initialize application"
- Check that Firebase and CometChat credentials are correct
- Ensure Firebase Authentication is enabled in Firebase Console
- Verify internet connection

### "Login failed"
- Ensure you're using valid credentials from an existing Amatyma account
- Check Firebase Console for authentication errors
- Verify email/password authentication is enabled

### Development server won't start
```bash
# Clear node_modules and reinstall
rm -rf node_modules package-lock.json
npm install
npm run dev
```

## Support

For issues related to:
- **Firebase**: Check Firebase Console logs
- **CometChat**: Check CometChat Dashboard logs
- **Build errors**: Clear cache and reinstall dependencies
