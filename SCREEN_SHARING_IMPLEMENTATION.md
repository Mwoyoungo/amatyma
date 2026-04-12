# Screen Sharing Implementation Guide

## 🎯 Overview

You're correct - **screen sharing is NOT a dashboard setting**. It requires **Presenter Mode** implementation using the CometChat Calls SDK.

---

## 📋 What I've Created

### New Files:
1. **PresentationActivity.kt** - Activity using Presenter Mode for screen sharing
2. **CallButtonHelper.kt** - Helper to override call buttons
3. **activity_presentation.xml** - UI layout (copied from before)

### Existing Useful Files:
- **CallConfigurationHelper.kt** - For raise hand feature
- **RaisedHand.kt** + **RaisedHandAdapter.kt** - Raise hand UI

---

## ⚠️ Critical Requirement: Call Token Generation

**You MUST have a backend** to generate call tokens. This cannot be done from the Android app for security reasons.

### Why Backend is Required:
- Call tokens must be generated using your **Auth Key**
- Auth Key should **NEVER** be in your app code (security risk)
- Tokens must be generated server-side using CometChat API

---

## 🔧 Implementation Steps

### Step 1: Create Backend Endpoint

You need to create a backend API endpoint that generates CometChat call tokens.

#### Option A: Node.js Backend

```javascript
const express = require('express');
const axios = require('axios');
const app = express();

const COMETCHAT_APP_ID = '281421fd397d9bf6';
const COMETCHAT_AUTH_KEY = '5d7e15509f2034cf002555883a2e732d412d358a';
const COMETCHAT_REGION = 'us';

app.post('/api/generate-call-token', async (req, res) => {
    try {
        const { sessionId, userId } = req.body;

        const response = await axios.post(
            `https://api-${COMETCHAT_REGION}.cometchat.io/v3/calls/${sessionId}/token`,
            {},
            {
                headers: {
                    'appId': COMETCHAT_APP_ID,
                    'apiKey': COMETCHAT_AUTH_KEY,
                    'Content-Type': 'application/json'
                }
            }
        );

        res.json({
            token: response.data.data.token,
            sessionId: sessionId
        });
    } catch (error) {
        console.error('Error generating token:', error);
        res.status(500).json({ error: 'Failed to generate call token' });
    }
});

app.listen(3000, () => {
    console.log('Server running on port 3000');
});
```

#### Option B: Firebase Cloud Function

```javascript
const functions = require('firebase-functions');
const axios = require('axios');

exports.generateCallToken = functions.https.onCall(async (data, context) => {
    const sessionId = data.sessionId;
    const userId = context.auth.uid;

    try {
        const response = await axios.post(
            `https://api-us.cometchat.io/v3/calls/${sessionId}/token`,
            {},
            {
                headers: {
                    'appId': '281421fd397d9bf6',
                    'apiKey': '5d7e15509f2034cf002555883a2e732d412d358a',
                    'Content-Type': 'application/json'
                }
            }
        );

        return {
            token: response.data.data.token,
            sessionId: sessionId
        };
    } catch (error) {
        throw new functions.https.HttpsError('internal', 'Failed to generate call token');
    }
});
```

---

### Step 2: Update PresentationActivity

Replace the `generateCallToken()` method in `PresentationActivity.kt`:

```kotlin
private fun generateCallToken(): String {
    // Call your backend API
    val client = OkHttpClient()

    val json = JSONObject().apply {
        put("sessionId", sessionId)
        put("userId", CometChat.getLoggedInUser()?.uid)
    }

    val request = Request.Builder()
        .url("https://your-backend.com/api/generate-call-token")
        .post(json.toString().toRequestBody("application/json".toMediaType()))
        .build()

    val response = client.newCall(request).execute()
    val responseData = JSONObject(response.body?.string() ?: "")

    return responseData.getString("token")
}
```

**OR if using Firebase:**

```kotlin
private fun generateCallToken(callback: (String?) -> Unit) {
    val functions = Firebase.functions

    val data = hashMapOf(
        "sessionId" to sessionId
    )

    functions.getHttpsCallable("generateCallToken")
        .call(data)
        .addOnSuccessListener { result ->
            val token = (result.data as? Map<*, *>)?.get("token") as? String
            callback(token)
        }
        .addOnFailureListener { exception ->
            Log.e(TAG, "Error generating token: ${exception.message}")
            callback(null)
        }
}
```

---

### Step 3: Register PresentationActivity in Manifest

Add to `AndroidManifest.xml`:

```xml
<activity
    android:name=".PresentationActivity"
    android:exported="false"
    android:screenOrientation="portrait"
    android:theme="@style/Theme.Amatyma.Fullscreen" />
```

---

### Step 4: Override Call Buttons (Optional)

If you want group video calls to automatically use Presenter Mode:

Update `MessagesActivity.kt` in the `setupGroupChat` method:

```kotlin
private fun setupGroupChat(groupId: String, preFilledMessage: String?) {
    CometChat.getGroup(groupId, object : CometChat.CallbackListener<Group>() {
        override fun onSuccess(group: Group?) {
            if (group != null) {
                runOnUiThread {
                    addProfileIconToHeader(null)

                    binding.messageHeader.setGroup(group)
                    binding.messageList.setGroup(group)

                    // OVERRIDE CALL BUTTONS FOR SCREEN SHARING
                    CallButtonHelper.configureCallButtonsForScreenSharing(
                        this@MessagesActivity,
                        binding.messageHeader.callButtons,
                        group = group
                    )

                    preFilledMessage?.let {
                        binding.messageComposer.setInitialComposerText(it)
                    }

                    binding.messageComposer.setGroup(group)
                }
            }
        }

        override fun onError(exception: CometChatException?) {
            Log.e("MessagesActivity", "Error fetching group: ${exception?.message}")
            runOnUiThread {
                finish()
            }
        }
    })
}
```

---

## 🎯 What This Gives You

### Features Enabled:
✅ **Screen Sharing** - Presenters can share their screen
✅ **Raise Hand** - Participants can raise hands (custom implementation)
✅ **Presenter Controls** - Mute, video, camera switch
✅ **Up to 5 Presenters** - Multiple people can present
✅ **Up to 100 Participants** - Large group support

### How It Works:
1. User clicks video call button in group chat
2. `CallButtonHelper` intercepts the call
3. Launches `PresentationActivity` instead of regular call
4. Backend generates call token
5. Presenter Mode starts with screen sharing enabled
6. Users can raise hands via custom messaging

---

## 📊 Architecture Comparison

### Regular Calls (Current):
- CometChat UI Kit → Default Call Screen
- No screen sharing
- Standard audio/video

### Presenter Mode (New):
- CometChat UI Kit → **PresentationActivity** → Presenter Mode
- **Screen sharing enabled**
- Raise hand feature
- Custom controls

---

## 🐛 Troubleshooting

### "Call token generation not implemented" Error:
- You need to implement the backend endpoint first
- Update `generateCallToken()` method in `PresentationActivity`

### Import Errors in PresentationActivity:
- Make sure CometChat Calls SDK is imported in your `build.gradle`:
  ```kotlin
  implementation("com.cometchat:calls-sdk-android:4.1.0")
  ```

### Call Doesn't Start:
- Check backend endpoint is running
- Verify Auth Key is correct
- Check logs for token generation errors

---

## 🚀 Quick Start (Without Backend)

If you want to test the UI first before implementing backend:

1. **Comment out** the call token generation in `PresentationActivity`
2. **Add a placeholder** UI that shows controls
3. **Test raise hand** feature (works without tokens)
4. **Implement backend** when ready for actual screen sharing

---

## ✨ Summary

**What You Need:**
1. ✅ Android code (I've created this)
2. ⚠️ Backend for call token generation (you need to create)
3. ✅ UI and layouts (I've created this)
4. ✅ Raise hand system (I've created this)

**Next Steps:**
1. Create backend endpoint for call token generation
2. Update `PresentationActivity.generateCallToken()` method
3. Register `PresentationActivity` in manifest
4. Test with multiple devices

**The screen sharing will work once you have the backend setup!**

---

## 📞 Need Help?

**CometChat Token API Docs**: https://www.cometchat.com/docs/v4/api-reference
**Presenter Mode Docs**: https://www.cometchat.com/docs/sdk/android/presenter-mode

Let me know if you need help setting up the backend!
