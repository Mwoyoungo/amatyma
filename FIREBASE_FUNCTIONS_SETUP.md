# Firebase Cloud Functions Setup

API= f18ee09a2d3f6249d7376a326d3f27e3cd5c1b1b clcla

## Prerequisites
1. Install Firebase CLI: `npm install -g firebase-tools`
2. Login to Firebase: `firebase login`

## Setup Steps

### 1. Initialize Firebase Functions (in project root directory)

```bash
cd C:\amatyma-app
firebase init functions
```

**Select:**
- Use an existing project: `amatyma-e75a8`
- Language: JavaScript
- ESLint: Yes
- Install dependencies: Yes

This creates a `functions/` directory.

### 2. Install Dependencies

```bash
cd functions
npm install axios
```

### 3. Get CometChat API Key

1. Go to https://app.cometchat.com/
2. Select your app (App ID: 281421fd397d9bf6)
3. Go to **API Keys** section
4. Copy the **Full Access API Key** (NOT the Auth-Only key)

### 4. Set Environment Variables

```bash
firebase functions:config:set cometchat.api_key="YOUR_FULL_ACCESS_API_KEY"
firebase functions:config:set cometchat.app_id="281421fd397d9bf6"
firebase functions:config:set cometchat.region="us"
```

### 5. Replace `functions/index.js` with the code below

### 6. Deploy Functions

```bash
firebase deploy --only functions
```

### 7. Enable Cloud Functions API

- Go to Firebase Console
- Functions tab
- May need to upgrade to Blaze plan (pay-as-you-go, but free tier covers most usage)

---

## Security Notes

⚠️ **IMPORTANT:**
- Never commit the Full Access API Key to git
- Use environment variables only
- The API key allows creating/deleting users - keep it secure
- Cloud Functions run server-side, so keys are safe there

---

## Testing

After deployment, test in Android app:
1. Sign up with a new account
2. Complete profile setup
3. Check Firebase Console > Functions for logs
4. Check CometChat Dashboard > Users to verify user was created
