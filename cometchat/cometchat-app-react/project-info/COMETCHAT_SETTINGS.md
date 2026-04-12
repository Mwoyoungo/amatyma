# CometChatSettingsInterface Explanation

## Overview

The `CometChatSettingsInterface` defines the configuration settings for a builder. It enables or disables various features within a chat application, including messaging, AI-assisted interactions, group management, and UI customization.

## Structure

The interface is organized into the following categories:

- **Chat Features**
- **Call Features**
- **Layout**
- **Style**
- **No Code** (Optional)
- **Agent** (Optional)

---

## 1. Chat Features (`chatFeatures`)

The `chatFeatures` section consists of multiple subcategories that define various chat functionalities.

### Core Messaging Experience

Controls fundamental chat functionalities:

- `typingIndicator`: Displays when a user is typing.
- `threadConversationAndReplies`: Enables threaded conversations.
- `photosSharing`: Allows sharing of images.
- `videoSharing`: Supports video file sharing.
- `audioSharing`: Enables sharing of audio recordings.
- `fileSharing`: Allows file sharing.
- `editMessage`: Enables editing sent messages.
- `deleteMessage`: Allows users to delete their sent messages.
- `messageDeliveryAndReadReceipts`: Shows message delivery and read receipts.
- `userAndFriendsPresence`: Displays users' online/offline status.

### Deeper User Engagement

Enhances user interaction through additional features:

- `mentions`: Enables @mentions in chat.
- `reactions`: Allows users to react to messages.
- `messageTranslation`: Provides message translation capabilities.
- `polls`: Enables creating and participating in polls.
- `collaborativeWhiteboard`: Allows real-time whiteboard collaboration.
- `collaborativeDocument`: Enables document collaboration.
- `voiceNotes`: Allows sending voice notes.
- `emojis`: Supports emojis in chat.
- `stickers`: Allows sending stickers.
- `userInfo`: Displays user profile information.
- `groupInfo`: Shows group details.

### AI User Copilot

Provides AI-powered assistance:

- `conversationStarter`: Suggests AI-generated conversation starters.
- `conversationSummary`: Summarizes chat conversations.
- `smartReply`: Suggests smart replies based on context.

### Group Management

Manages group-related functionalities:

- `createGroup`: Allows users to create new groups.
- `addMembersToGroups`: Enables adding members to groups.
- `joinLeaveGroup`: Allows users to join or leave groups.
- `deleteGroup`: Enables group deletion by admins.
- `viewGroupMembers`: Displays group member lists.

### Moderator Controls

Provides moderation features for group management:

- `kickUsers`: Allows moderators to remove users from groups.
- `banUsers`: Enables banning users.
- `promoteDemoteMembers`: Allows promoting or demoting group members.

### Private Messaging Within Groups

- `sendPrivateMessageToGroupMembers`: Enables private messaging within groups.

---

## 2. Call Features (`callFeatures`)

Defines voice and video call functionalities.

### Voice and Video Calling

- `oneOnOneVoiceCalling`: Enables one-on-one voice calls.
- `oneOnOneVideoCalling`: Supports one-on-one video calls.
- `groupVideoConference`: Allows group video calls.
- `groupVoiceConference`: Enables group voice-only calls.

---

## 3. Layout (`layout`)

Configures UI layout options:

- `withSideBar`: Determines if the sidebar is visible.
- `tabs`: Defines available UI tabs (e.g., ['chats', 'calls', 'users', 'groups']).
- `chatType`: Specifies chat type (e.g., 'user' or 'group').

---

## 4. Style (`style`)

Handles theme and UI styling preferences.

### Theme and Colors

- `theme`: Defines the theme mode (light, dark, or system).
- `color`:Contains settings for brand colors and text colors in different modes. Enter a hex code for each color.
  - `brandColor`: Primary UI color. Enter a hex code.
  - `primaryTextLight`: Text color in light mode. Enter a hex code.
  - `primaryTextDark`: Text color in dark mode. Enter a hex code.
  - `secondaryTextLight`: Secondary text color in light mode. Enter a hex code.
  - `secondaryTextDark`: Secondary text color in dark mode. Enter a hex code.

### Typography

- `font`: Specifies the UI font family (e.g., 'roboto').
- `size`: Defines the font size preference (e.g., 'default').

---

## 5. No Code (`noCode`) - Optional

Configures no-code deployment and customization options:

- `docked`: Determines if the chat widget is displayed in docked mode.

### Styles

- `buttonBackGround`: Background color for the docked chat button. Enter a hex code.
- `buttonShape`: Shape of the docked button (e.g., 'rounded').
- `openIcon`: URL for the icon displayed when chat is closed.
- `closeIcon`: URL for the icon displayed when chat is open.
- `customJs`: Custom JavaScript code to be executed.
- `customCss`: Custom CSS styles to be applied.
- `dockedAlignment`: Position of the docked button ('left' or 'right').

---

## 6. Agent (`agent`) - Optional

Configures agent-specific features for customer support scenarios:

- `chatHistory`: Enables access to previous chat history.
- `newChat`: Allows starting new chat conversations.
- `agentIcon`: URL or path to the agent's profile icon.
- `showAgentIcon`: Determines if the agent icon should be displayed.

---

## Summary

The `CometChatSettingsInterface` allows full customization of chat, calling, layout, style, no-code deployment, and agent configurations, making it a powerful tool for building dynamic user experiences across various use cases.
