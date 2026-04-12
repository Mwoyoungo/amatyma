# Group Calling Features Implementation Guide

## ✅ Features Implemented

This document describes the screen sharing and raise hand features that have been added to the Amatyma app.

### 1. **Screen Sharing** (via Presenter Mode)
- Full presenter mode support for up to 5 presenters
- Screen sharing toggle button in call interface
- Supports up to 100 total participants (presenters + viewers)
- Only presenters can share their screen

### 2. **Raise Hand Feature**
- Custom raise/lower hand button
- Real-time hand raise notifications via custom messages
- Visual list of users with raised hands
- Timestamp tracking for each raised hand
- Automatic UI updates when hands are raised/lowered

---

## 📁 Files Created

### Layouts
- `activity_group_call.xml` - Main call interface with controls
- `item_raised_hand.xml` - Individual raised hand list item

### Kotlin Classes
- `GroupCallActivity.kt` - Main activity handling calls, screen share, and raise hand
- `RaisedHand.kt` - Data model for raised hand events
- `RaisedHandAdapter.kt` - RecyclerView adapter for raised hands list

### Drawable Icons
- `ic_mic.xml` / `ic_mic_off.xml` - Microphone icons
- `ic_videocam.xml` / `ic_videocam_off.xml` - Video camera icons
- `ic_camera_switch.xml` - Camera switch icon
- `ic_screen_share.xml` - Screen sharing icon
- `ic_hand_raise.xml` - Raise hand icon
- `ic_video_call.xml` - Video call button icon

### Modified Files
- `AndroidManifest.xml` - Added GroupCallActivity registration
- `MessagesActivity.kt` - Added video call button for group chats

---

## 🎯 How It Works

### Starting a Group Call

1. User opens a group chat in `MessagesActivity`
2. Video call icon appears in the header (only for group chats)
3. Clicking the icon launches `GroupCallActivity` with:
   - Group ID
   - Group name
   - Presenter status (default: true)

### Raise Hand Feature

**Sending a Raise Hand:**
```kotlin
// When user clicks "Raise Hand" button:
1. Toggle isHandRaised state
2. Create CustomMessage with type "raise_hand"
3. Include metadata: userId, userName, timestamp
4. Send via CometChat.sendCustomMessage()
5. Update local UI
```

**Receiving Raise Hands:**
```kotlin
// Via CometChat.MessageListener:
1. Listen for CustomMessage with type "raise_hand"
2. Extract user info from message metadata
3. Add to raisedHands list
4. Update RecyclerView adapter
5. Show raised hands card if hidden
```

**Lowering a Hand:**
```kotlin
// Same process but with "lower_hand" type:
1. Send CustomMessage with type "lower_hand"
2. Remove from local raisedHands list
3. Update UI
4. Hide card if no hands remain
```

---

## ⚠️ Important: Completing the Integration

The current implementation has **placeholder code** for actual video calling. To complete the integration, you need to:

### Step 1: Generate Call Tokens

Call tokens must be generated from your **backend server** using CometChat's Auth Key. **Never** expose your Auth Key in the client app.

**Backend Implementation (Node.js example):**
```javascript
const CometChat = require('@cometchat-pro/chat');

// Initialize CometChat on backend
CometChat.init(APP_ID, AUTH_KEY);

// Generate call token endpoint
app.post('/generate-call-token', async (req, res) => {
  const { sessionId, userId } = req.body;

  try {
    const token = await CometChat.generateCallToken(sessionId);
    res.json({ token });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});
```

### Step 2: Update GroupCallActivity

Replace the placeholder in `generateCallToken()` (line ~145):

```kotlin
private fun generateCallToken() {
    // Call your backend API
    val api = RetrofitInstance.api // Your API client
    val sessionId = "session_${System.currentTimeMillis()}"

    api.generateCallToken(sessionId, CometChat.getLoggedInUser()?.uid).enqueue(
        object : Callback<CallTokenResponse> {
            override fun onResponse(call: Call<CallTokenResponse>, response: Response<CallTokenResponse>) {
                if (response.isSuccessful) {
                    this@GroupCallActivity.sessionId = sessionId
                    this@GroupCallActivity.callToken = response.body()?.token
                    startPresenterMode()
                } else {
                    showError("Failed to generate call token")
                }
            }

            override fun onFailure(call: Call<CallTokenResponse>, t: Throwable) {
                showError("Network error: ${t.message}")
            }
        }
    )
}
```

### Step 3: Uncomment Presenter Mode Code

In `startPresenterMode()` (line ~160), uncomment and use the actual implementation:

```kotlin
import com.cometchat.calls.model.PresentationSettings
import com.cometchat.calls.model.CometChatCalls
import com.cometchat.calls.listeners.CometChatCallsEventsListener
import com.cometchat.calls.model.RTCUser

private fun startPresenterMode() {
    val presenterSettings = CometChatCalls.PresentationSettingsBuilder(this)
        .setIsPresenter(isPresenter)
        .setDefaultLayoutEnable(true)
        .setEventListener(object : CometChatCallsEventsListener() {
            override fun onUserJoined(user: RTCUser) {
                Log.d(TAG, "User joined: ${user.uid}")
            }

            override fun onUserLeft(user: RTCUser) {
                Log.d(TAG, "User left: ${user.uid}")
            }

            override fun onError(exception: CometChatException) {
                Log.e(TAG, "Call error: ${exception.message}")
            }

            override fun onCallEnded() {
                Log.d(TAG, "Call ended")
                finish()
            }
        })
        .build()

    CometChatCalls.joinPresentation(
        callToken!!,
        presenterSettings,
        binding.videoContainer,
        object : CometChat.CallbackListener<String>() {
            override fun onSuccess(result: String?) {
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    callStartTime = System.currentTimeMillis()
                    durationHandler.post(durationRunnable)
                }
            }

            override fun onError(exception: CometChatException?) {
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@GroupCallActivity,
                        "Failed to join call: ${exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }
    )
}
```

### Step 4: Implement Control Methods

Replace placeholder methods with actual SDK calls:

```kotlin
private fun toggleMute() {
    isMuted = !isMuted
    CometChatCalls.muteAudio(isMuted) // Actual SDK call
    binding.btnMute.setImageResource(
        if (isMuted) R.drawable.ic_mic_off else R.drawable.ic_mic
    )
}

private fun toggleVideo() {
    isVideoOff = !isVideoOff
    CometChatCalls.pauseVideo(isVideoOff) // Actual SDK call
    binding.btnVideo.setImageResource(
        if (isVideoOff) R.drawable.ic_videocam_off else R.drawable.ic_videocam
    )
}

private fun switchCamera() {
    CometChatCalls.switchCamera() // Actual SDK call
}

private fun toggleScreenShare() {
    if (!isPresenter) {
        Toast.makeText(this, "Only presenters can share screen", Toast.LENGTH_SHORT).show()
        return
    }

    isSharingScreen = !isSharingScreen

    if (isSharingScreen) {
        CometChatCalls.startScreenShare() // Actual SDK call
    } else {
        CometChatCalls.stopScreenShare() // Actual SDK call
    }

    binding.btnScreenShare.text = if (isSharingScreen) "Stop Sharing" else "Share Screen"
}

private fun endCall() {
    CometChatCalls.endSession() // Actual SDK call
    finish()
}
```

---

## 🎨 UI Features

### Call Controls

**Primary Controls:**
- **Mute Button** - Toggle microphone on/off
- **Video Button** - Toggle camera on/off
- **End Call Button** - Terminate the call
- **Switch Camera Button** - Toggle between front/back camera

**Secondary Controls:**
- **Share Screen Button** - Start/stop screen sharing (presenters only)
- **Raise Hand Button** - Raise/lower hand to signal

### Raised Hands Display

- Appears as a card at the top of the screen when hands are raised
- Shows list of users with raised hands
- Displays how long ago each hand was raised
- Auto-hides when all hands are lowered

### Call Information

- Group name displayed at top
- Call duration timer (MM:SS format)
- Loading indicator during initialization

---

## 🔧 Configuration Options

### Presenter vs Viewer Mode

Change the `IS_PRESENTER` parameter when launching GroupCallActivity:

```kotlin
intent.putExtra("IS_PRESENTER", true)  // Can share screen, full controls
intent.putExtra("IS_PRESENTER", false) // View-only, limited controls
```

### Customizing Call Settings

In `startPresenterMode()`, you can customize:

```kotlin
val presenterSettings = CometChatCalls.PresentationSettingsBuilder(this)
    .setIsPresenter(isPresenter)
    .setDefaultLayoutEnable(true)
    .startWithAudioMuted(false)
    .startWithVideoMuted(false)
    .setIsAudioOnly(false)
    .showRecordingButton(true)
    .autoRecordOnCallStart(false)
    .setIdleTimeoutPeriod(180) // seconds
    // ... more settings
    .build()
```

---

## 📊 Custom Message Format

### Raise Hand Message

```json
{
  "receiverId": "GROUP_ID",
  "receiverType": "group",
  "customType": "raise_hand",
  "customData": {
    "userId": "user123",
    "userName": "John Doe",
    "timestamp": 1234567890000
  }
}
```

### Lower Hand Message

```json
{
  "receiverId": "GROUP_ID",
  "receiverType": "group",
  "customType": "lower_hand",
  "customData": {
    "userId": "user123",
    "userName": "John Doe",
    "timestamp": 1234567890000
  }
}
```

---

## 🐛 Troubleshooting

### "Call token is null" Error
- Ensure your backend is properly generating call tokens
- Check that the token is being passed correctly to `GroupCallActivity`

### Screen Sharing Not Working
- Verify you're running on Android API 21+
- Ensure `IS_PRESENTER` is set to `true`
- Check CometChat Calls SDK is properly initialized

### Raise Hand Not Appearing
- Verify CometChat message listener is registered
- Check that both users are in the same group
- Ensure custom message type matches exactly ("raise_hand")

### Video Container Empty
- Confirm call token is valid
- Check that `startPresenterMode()` is being called
- Verify `binding.videoContainer` is a RelativeLayout

---

## 📚 Additional Resources

- [CometChat Android SDK Docs](https://www.cometchat.com/docs/sdk/android/overview)
- [CometChat Presenter Mode](https://www.cometchat.com/docs/sdk/android/presenter-mode)
- [CometChat Direct Calling](https://www.cometchat.com/docs/sdk/android/direct-calling)

---

## ✨ Testing Checklist

- [ ] Backend endpoint generates valid call tokens
- [ ] GroupCallActivity opens when video call icon clicked
- [ ] Presenter mode initializes with video container
- [ ] Mute/unmute buttons work correctly
- [ ] Video on/off buttons work correctly
- [ ] Camera switch button works
- [ ] Screen share button toggles sharing (presenters only)
- [ ] Raise hand sends custom message
- [ ] Raised hands appear in list for other users
- [ ] Lower hand removes from list
- [ ] Call duration timer updates correctly
- [ ] End call button properly terminates session

---

## 🚀 Next Steps

1. **Set up backend token generation** - Critical for security
2. **Test with multiple users** - Verify real-time synchronization
3. **Add error handling** - Handle network failures gracefully
4. **Implement user roles** - Define who can be presenters
5. **Add notifications** - Alert users when hands are raised
6. **Record calls** - Enable call recording if needed

---

## 📝 Notes

- The raise hand feature works immediately without backend changes
- Screen sharing requires proper CometChat Calls SDK setup
- Always test with real devices for camera/microphone features
- Consider adding analytics to track feature usage

---

**Implementation completed by Claude Code**
Date: 2025-10-27
