# Enabling Screen Share & Raise Hand in CometChat Calls

Since your CometChat calling is **already working**, we just need to enable these features!

---

## 🖥️ **SCREEN SHARING** - Enable in Dashboard

Screen sharing is **built into CometChat** but needs to be enabled in your dashboard.

### Steps to Enable:

1. **Go to CometChat Dashboard**: https://app.cometchat.com/
2. **Login** with your account
3. **Select your app** (amatyma-e75a8 / App ID: 281421fd397d9bf6)
4. Navigate to: **Calling** → **Settings** or **Features**
5. Look for **"Screen Sharing"** option
6. **Enable** screen sharing toggle
7. **Save** settings

### What This Does:

- Adds a screen share button to the ongoing call interface
- Users will see it automatically in calls (no code changes needed!)
- Works for both presenters and participants (based on your settings)

### If You Don't See the Option:

- **Check your CometChat plan** - Screen sharing may require a specific tier
- **Contact CometChat support** - They can enable it for your app
- **API Key**: `281421fd397d9bf6`

---

##✋ **RAISE HAND** - Custom Implementation

Raise hand is **NOT built into CometChat**, so I've created it for you using custom messages.

### What I Created:

✅ **CallConfigurationHelper.kt** - Helper methods for raise/lower hand
✅ **RaisedHand.kt** - Data model
✅ **RaisedHandAdapter.kt** - UI list adapter
✅ **GroupCallActivity.kt** - Full example implementation

### How Raise Hand Works:

1. **During a call**, user taps "Raise Hand" button
2. Sends a **custom message** to the group
3. **All participants** receive the message
4. UI shows **who raised their hand** and when

---

## 🎯 **Simple Integration Option**

Since your calling already works through CometChat UI Kit, here's the **easiest way** to add raise hand:

### Option 1: Add Raise Hand to Chat (During Call)

Users can send a raise hand message in the chat during the call:

```kotlin
// In your MessagesActivity.kt - add a quick action button
import com.lokaleza.amatyma.CallConfigurationHelper

// Add this method
private fun sendRaiseHand(groupId: String) {
    CallConfigurationHelper.sendRaiseHand(groupId, object : CometChat.CallbackListener<CustomMessage>() {
        override fun onSuccess(message: CustomMessage?) {
            Toast.makeText(this@MessagesActivity, "Hand raised! ✋", Toast.LENGTH_SHORT).show()
        }

        override fun onError(exception: CometChatException?) {
            Toast.makeText(this@MessagesActivity, "Error: ${exception?.message}", Toast.LENGTH_SHORT).show()
        }
    })
}
```

### Option 2: Use the Full GroupCallActivity

The `GroupCallActivity` I created has **everything**:
- ✅ Raise hand button
- ✅ List of raised hands
- ✅ Video controls
- ✅ Screen share button (once enabled in dashboard)

You can:
1. Keep using CometChat's default call screen
2. Use GroupCallActivity for calls that need raise hand
3. Or replace CometChat's screen entirely

---

## 🔍 **Checking What's Enabled**

### Test Screen Sharing:

1. Make a group call
2. Look for a **"Share Screen"** or **monitor icon** button
3. If it's there → Already enabled! ✅
4. If not there → Enable in dashboard

### Test Raise Hand:

The raise hand feature I created works through **custom messages**:
- It's **independent** of CometChat's call UI
- Works via the messaging system
- Can be added as:
  - Button in the chat
  - Overlay on the call screen
  - Separate call activity

---

## 📋 **Quick Start Checklist**

### For Screen Sharing:
- [ ] Login to CometChat Dashboard
- [ ] Navigate to Calling Settings
- [ ] Enable Screen Sharing
- [ ] Test in a call
- [ ] If issues, check plan/contact support

### For Raise Hand:
- [ ] Use `CallConfigurationHelper.sendRaiseHand()` method
- [ ] Add button to MessagesActivity
- [ ] Or use `GroupCallActivity` for full interface
- [ ] Test with multiple users

---

## 🛠️ **Your Options**

### Choice A: Keep CometChat Default UI (Easiest)
- ✅ Enable screen share in dashboard
- ✅ Add raise hand button to chat interface
- ⏱️ 10 minutes setup

### Choice B: Enhanced Call Activity (Full Featured)
- ✅ Use the GroupCallActivity I created
- ✅ Custom UI with all features
- ✅ Raise hand list, screen share, etc.
- ⏱️ 30 minutes to integrate

### Choice C: Hybrid Approach
- ✅ Use CometChat's call UI for video
- ✅ Add raise hand overlay on top
- ⏱️ 20 minutes

---

## 🎥 **Current Status**

### What's Working:
- ✅ Group calling (via CometChat UI Kit)
- ✅ Video/audio controls
- ✅ Call button in message header

### What Needs Setup:
- ⚙️ Screen sharing (enable in dashboard)
- ⚙️ Raise hand (choose integration option above)

---

## 💡 **Recommended Next Steps**

1. **Right now**: Login to CometChat dashboard and enable screen sharing
2. **Test**: Make a call and see if screen share button appears
3. **For raise hand**: Decide which option (A, B, or C) works best for your app
4. **Let me know**: If you need help implementing your chosen option!

---

## 📞 **Need Help?**

**CometChat Support**: https://help.cometchat.com/
**Your App ID**: `281421fd397d9bf6`

Let me know which approach you want to take, and I'll help you implement it!
