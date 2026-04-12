<p align="center">
  <img alt="CometChat" src="https://assets.cometchat.io/website/images/logos/banner.png">
</p>

<p>
  <img alt="version" src="https://img.shields.io/badge/version-v1.0.15-blue" />
  <img alt="status" src="https://img.shields.io/badge/status-stable-brightgreen" />
  <img alt="react" src="https://img.shields.io/badge/react-supported-61DAFB?logo=react" />
  <img alt="next.js" src="https://img.shields.io/badge/next.js-supported-black?logo=next.js" />
  <img alt="typescript" src="https://img.shields.io/badge/typescript-supported-blue" />
</p>

# Integration steps for UI Kit Builder

Follow these steps to integrate it into your existing Next.js project:

## 1. Install dependencies in your app

Run the following command to install the required dependencies:

```bash
npm install @cometchat/chat-uikit-react@6.3.2 @cometchat/calls-sdk-javascript
```

## 2. Copy CometChat Folder

Copy the `cometchat-app-react/src/CometChat` folder inside your src/app directory.

## 3. Initialize CometChat UI Kit

Now we will create the CometChatNoSSR.tsx file. Here, we will initialize the CometChat UI Kit.

```
src/app/
│── CometChat/
│── CometChatNoSSR/
│ ├── CometChatNoSSR.tsx

```

```typescript
import React, { useEffect } from "react";
import {
  CometChatUIKit,
  UIKitSettingsBuilder,
} from "@cometchat/chat-uikit-react";
import CometChatApp from "../CometChat/CometChatApp";
import { CometChatProvider } from "../CometChat/context/CometChatContext";
import { setupLocalization } from "../CometChat/utils/utils";

export const COMETCHAT_CONSTANTS = {
  APP_ID: "", // Replace with your App ID
  REGION: "", // Replace with your App Region
  AUTH_KEY: "", // Replace with your Auth Key or leave blank if you are authenticating using Auth Token
};

// Functional Component
const CometChatNoSSR: React.FC = () => {
  useEffect(() => {
    // Initialize UIKit settings
    const UIKitSettings = new UIKitSettingsBuilder()
      .setAppId(COMETCHAT_CONSTANTS.APP_ID)
      .setRegion(COMETCHAT_CONSTANTS.REGION)
      .setAuthKey(COMETCHAT_CONSTANTS.AUTH_KEY)
      .subscribePresenceForAllUsers()
      .build();

    // Initialize CometChat UIKit
    CometChatUIKit.init(UIKitSettings)
      ?.then(() => {
        setupLocalization();
        console.log("Initialization completed successfully");
        CometChatUIKit.getLoggedinUser().then((loggedInUser) => {
          if (!loggedInUser) {
            CometChatUIKit.login("cometchat-uid-1")
              .then((user) => {
                console.log("Login Successful", { user });
              })
              .catch((error) => console.error("Login failed", error));
          } else {
            console.log("Already logged-in", { loggedInUser });
          }
        });
      })
      .catch((error) => console.error("Initialization failed", error));
  }, []);

  return (
    /* The CometChatApp component requires a parent element with an explicit height and width
   to render properly. Ensure the container has defined dimensions, and adjust them as needed  
   based on your layout requirements. */
    <div style={{ width: "100vw", height: "100dvh" }}>
      <CometChatProvider>
        <CometChatApp />
      </CometChatProvider>
    </div>
  );
};

export default CometChatNoSSR;
```

### Note:

Use the `CometChatApp` component to launch a chat interface with a selected user or group.

```typescript
import React, { useEffect, useState } from "react";
import {
  CometChatUIKit,
  UIKitSettingsBuilder,
} from "@cometchat/chat-uikit-react";
import CometChatApp from "../CometChat/CometChatApp";
import { CometChatProvider } from "../CometChat/context/CometChatContext";
import { setupLocalization } from "../CometChat/utils/utils";
import { CometChat } from "@cometchat/chat-sdk-javascript";

export const COMETCHAT_CONSTANTS = {
  APP_ID: "", // Replace with your App ID
  REGION: "", // Replace with your App Region
  AUTH_KEY: "", // Replace with your Auth Key or leave blank if you are authenticating using Auth Token
};

// Functional Component
const CometChatNoSSR: React.FC = () => {
  const [user, setUser] = useState<CometChat.User | undefined>(undefined);

  const [selectedUser, setSelectedUser] = useState<CometChat.User | undefined>(
    undefined
  );
  const [selectedGroup, setSelectedGroup] = useState<
    CometChat.Group | undefined
  >(undefined);

  useEffect(() => {
    const UIKitSettings = new UIKitSettingsBuilder()
      .setAppId(COMETCHAT_CONSTANTS.APP_ID)
      .setRegion(COMETCHAT_CONSTANTS.REGION)
      .setAuthKey(COMETCHAT_CONSTANTS.AUTH_KEY)
      .subscribePresenceForAllUsers()
      .build();
    // Initialize CometChat UIKit
    CometChatUIKit.init(UIKitSettings)
      ?.then(() => {
        setupLocalization();
        console.log("Initialization completed successfully");
        CometChatUIKit.getLoggedinUser().then((loggedInUser) => {
          if (!loggedInUser) {
            CometChatUIKit.login("cometchat-uid-1") // Replace with your logged in user UID
              .then((user) => {
                console.log("Login Successful", { user });
                setUser(user);
              })
              .catch((error) => console.error("Login failed", error));
          } else {
            console.log("Already logged-in", { loggedInUser });
            setUser(loggedInUser);
          }
        });
      })
      .catch((error) => console.error("Initialization failed", error));
  }, []);

  useEffect(() => {
    if (user) {
      // Fetch user or group from CometChat SDK whose chat you want to load.

      /** Fetching User */
      const UID = "cometchat-uid-2"; // Replace with your actual UID
      CometChat.getUser(UID).then(
        (user) => {
          setSelectedUser(user);
        },
        (error) => {
          console.log("User fetching failed with error:", error);
        }
      );

      /** Fetching Group */
      const GUID = "GUID"; // Replace with your actual GUID
      CometChat.getGroup(GUID).then(
        (group) => {
          setSelectedGroup(group);
        },
        (error) => {
          console.log("User fetching failed with error:", error);
        }
      );
    }
  }, [user]);

  return (
    /* The CometChatApp component requires a parent element with an explicit height and width
   to render properly. Ensure the container has defined dimensions, and adjust them as needed  
   based on your layout requirements. */
    <div style={{ width: "100vw", height: "100dvh" }}>
      <CometChatProvider>
        {(selectedUser || selectedGroup) && (
          <CometChatApp user={selectedUser} group={selectedGroup} />
        )}
      </CometChatProvider>
    </div>
  );
};

export default CometChatNoSSR;
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

## 4. User Login

Refer to the [User Login Guide](https://www.cometchat.com/docs/ui-kit/react/next-js-integration#step-4-user-login) to implement user authentication in your app.

## 5. Render CometChatApp Component

In this step, we will be rendering `CometChatApp` component.

Additionally, we will disable Server-Side Rendering (SSR) for `CometChatNoSSR.tsx` while keeping the rest of the application’s SSR functionality intact. This ensures that the CometChat UI Kit Builder components load only on the client-side, preventing SSR-related issues.

Create a wrapper file to include the CometChatApp component with server-side rendering disabled.
In this wrapper, dynamically import the CometChatNoSSR.tsx component with the `{ ssr: false }` option.

```typescript
"use client";
import dynamic from "next/dynamic";

// Dynamically import CometChat component with SSR disabled
const CometChatComponent = dynamic(
  () => import("../app/CometChatNoSSR/CometChatNoSSR"),
  {
    ssr: false,
  }
);

export default function CometChatAppWrapper() {
  return (
    <div>
      <CometChatComponent />
    </div>
  );
}
```

Now, import and use the wrapper component in your project’s main entry file.

```typescript
import CometChatAppWrapper from "./CometChatAppWrapper";

export default function Home() {
  return (
    <>
      {/* Other components or content */}
      <CometChatAppWrapper />
    </>
  );
}
```

**Why disable SSR?**

- The CometChat UI Kit Builder depends on browser APIs (window, document, WebSockets).
- Next.js pre-renders components on the server, which can cause errors with browser-specific features.
- By setting `{ ssr: false }`, we ensure that CometChatNoSSR.tsx only loads on the client.

## 6. Run the App

Finally, start your app using the appropriate command based on your setup:

```bash
npm run dev
```

## Note:

Enable the following features in the Dashboard in your App under Chat > Features to ensure full functionality.

> Mentions, Reactions, Message translation, Polls, Collaborative whiteboard, Collaborative document, Emojis, Stickers, Conversation starter, Conversation summary, Smart reply.

---

For detailed steps, refer to our <a href="https://www.cometchat.com/docs/ui-kit/react/builder-integration-nextjs">UI Kit Builder documentation</a>
