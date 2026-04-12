<p align="center">
  <img alt="CometChat" src="https://assets.cometchat.io/website/images/logos/banner.png">
</p>

<p>
  <img alt="version" src="https://img.shields.io/badge/version-v1.0.15-blue" />
  <img alt="status" src="https://img.shields.io/badge/status-stable-brightgreen" />
  <img alt="react" src="https://img.shields.io/badge/react-supported-61DAFB?logo=react" />
  <img alt="vite" src="https://img.shields.io/badge/vite-supported-646CFF?logo=vite" />
  <img alt="typescript" src="https://img.shields.io/badge/typescript-supported-blue" />
</p>

# Integration steps for UI Kit Builder

Follow these steps to integrate it into your existing React project:

For Next.js integration, please refer to our <a href="https://www.cometchat.com/docs/ui-kit/react/builder-integration-nextjs" target="_blank">step-by-step guide</a>.

## 1. Install dependencies in your app

Run the following command to install the required dependencies:

```bash
npm install @cometchat/chat-uikit-react@6.3.2 @cometchat/calls-sdk-javascript
```

## 2. Copy CometChat Folder

Copy the `cometchat-app-react/src/CometChat` folder into your project’s `src` directory.

## 3. Initialize CometChat UI Kit

The initialization process varies depending on your setup. Select your framework:

<details>
  <summary>CRA</summary>

Open the file `src/index.tsx` and update it to include the required imports and initialization logic.

```typescript
import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import {
  UIKitSettingsBuilder,
  CometChatUIKit,
} from "@cometchat/chat-uikit-react";
import { setupLocalization } from "./CometChat/utils/utils";
import { CometChatProvider } from "./CometChat/context/CometChatContext";

export const COMETCHAT_CONSTANTS = {
  APP_ID: "", // Replace with your App ID
  REGION: "", // Replace with your App Region
  AUTH_KEY: "", // Replace with your Auth Key or leave blank if you are authenticating using Auth Token
};

const uiKitSettings = new UIKitSettingsBuilder()
  .setAppId(COMETCHAT_CONSTANTS.APP_ID)
  .setRegion(COMETCHAT_CONSTANTS.REGION)
  .setAuthKey(COMETCHAT_CONSTANTS.AUTH_KEY)
  .subscribePresenceForAllUsers()
  .build();

CometChatUIKit.init(uiKitSettings)?.then(() => {
  setupLocalization();
  ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
    <CometChatProvider>
      <App />
    </CometChatProvider>
  );
});
```

</details>

<details>
  <summary>Vite</summary>

Open the file `src/main.tsx` and update it to include the required imports and initialization logic.

```typescript
import { createRoot } from "react-dom/client";
import "./index.css";
import App from "./App.tsx";
import {
  UIKitSettingsBuilder,
  CometChatUIKit,
} from "@cometchat/chat-uikit-react";
import { setupLocalization } from "./CometChat/utils/utils.ts";
import { CometChatProvider } from "./CometChat/context/CometChatContext.tsx";

export const COMETCHAT_CONSTANTS = {
  APP_ID: "", // Replace with your App ID
  REGION: "", // Replace with your App Region
  AUTH_KEY: "", // Replace with your Auth Key or leave blank if you are authenticating using Auth Token
};

const uiKitSettings = new UIKitSettingsBuilder()
  .setAppId(COMETCHAT_CONSTANTS.APP_ID)
  .setRegion(COMETCHAT_CONSTANTS.REGION)
  .setAuthKey(COMETCHAT_CONSTANTS.AUTH_KEY)
  .subscribePresenceForAllUsers()
  .build();

CometChatUIKit.init(uiKitSettings)?.then(() => {
  setupLocalization();
  createRoot(document.getElementById("root")!).render(
    <CometChatProvider>
      <App />
    </CometChatProvider>
  );
});
```

</details>

## 4. User Login

Refer to the [User Login Guide](https://www.cometchat.com/docs/ui-kit/react/react-js-integration#step-4-user-login) to implement user authentication in your app.

## 5. Render CometChatApp Component

Add the `CometChatApp` component to your app:

```typescript
import CometChatApp from "./CometChat/CometChatApp";

const App = () => {
  return (
    /* The CometChatApp component requires a parent element with an explicit height and width  
   to render properly. Ensure the container has defined dimensions, and adjust them as needed  
   based on your layout requirements. */
    <div style={{ width: "100vw", height: "100dvh" }}>
      <CometChatApp />
    </div>
  );
};

export default App;
```

### Note:

Use the `CometChatApp` component to launch a chat interface with a selected user or group.

```typescript
import { useEffect, useState } from "react";
import { CometChat } from "@cometchat/chat-sdk-javascript";
import CometChatApp from "./CometChat/CometChatApp";

const App = () => {
  const [selectedUser, setSelectedUser] = useState<CometChat.User | undefined>(
    undefined
  );
  const [selectedGroup, setSelectedGroup] = useState<
    CometChat.Group | undefined
  >(undefined);

  useEffect(() => {
    const UID = "UID"; // Replace with your User ID
    CometChat.getUser(UID).then(setSelectedUser).catch(console.error);

    const GUID = "GUID"; // Replace with your Group ID
    CometChat.getGroup(GUID).then(setSelectedGroup).catch(console.error);
  }, []);

  return (
    /* CometChatApp requires a parent with explicit height & width to render correctly.
      Adjust the height and width as needed.
     */
    <div style={{ width: "100vw", height: "100vh" }}>
      <CometChatApp user={selectedUser} group={selectedGroup} />
    </div>
  );
};

export default App;
```

This implementation applies when you choose the **Without Sidebar** option for Sidebar.

- If `chatType` is `user` (default), only User chats will be displayed (either the selected user or the default user).
- If `chatType` is `group`, only Group chats will be displayed (either the selected group or the default group).

### Group Action Messages
Control whether group action messages (like member joins, leaves, kicked, banned) are displayed by using the `showGroupActionMessages` prop:

```typescript
<CometChatApp 
  showGroupActionMessages={true}
/>
```

- If `showGroupActionMessages` is `true` (default): Group action messages are visible.
- If `showGroupActionMessages` is `false`: Group action messages are hidden.

💡 Dashboard Setting:
To enable or disable this feature from the dashboard, go to **Chat > Settings** and toggle **Include Group Actions** on or off.

This allows you to customize the chat experience by showing or hiding system-generated group notifications.

## 6. Run the App

Finally, start your app using the appropriate command based on your setup:

- Vite

```bash
npm run dev
```

- Create React App (CRA):

```bash
npm start
```

## Note:

Enable the following features in the Dashboard in your App under Chat > Features to ensure full functionality.

> Mentions, Reactions, Message translation, Polls, Collaborative whiteboard, Collaborative document, Emojis, Stickers, Conversation starter, Conversation summary, Smart reply.

---

If you face any issues while integrating the builder in your app project, please check if you have the following configurations added to your `tsConfig.json`:

```json
{
  "compilerOptions": {
    "jsx": "react-jsx",
    "resolveJsonModule": true
  }
}
```

If your development server is running, restart it to ensure the new TypeScript configuration is picked up.

## Run the UI Kit Builder App Independently (optional)

> &nbsp;
>
> 1. Open the `cometchat-app-react` folder.
> 2. Add credentials for your app in `src/index.tsx` (`src/main.tsx` incase for Vite):
>
> ```typescript
> export const COMETCHAT_CONSTANTS = {
>   APP_ID: "", // Replace with your App ID
>   REGION: "", // Replace with your App Region
>   AUTH_KEY: "", // Replace with your Auth Key or leave blank if you are authenticating using Auth Token
> };
> ```
>
> 3. Install dependencies:
>
> ```bash
> npm i
> ```
>
> 4. Run the app:
>
> ```bash
> npm start
> ```

For detailed steps, refer to our <a href="https://www.cometchat.com/docs/ui-kit/react/builder-integration" target="_blank">UI Kit Builder documentation</a>
