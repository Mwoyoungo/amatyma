# Chat Builder App – Folder Structure Documentation

This document provides an overview of the folder structure of the Chat Builder App, explaining the purpose of each directory and file.

---

## Project Structure Overview

```
cometchat-builder-react/
│── public/
│── src/
│   ├── CometChat/
│      ├── assets/
│      ├── components/
│      ├── context/
│      ├── customHook/
│      ├── locales/
│      ├── styles/
│      ├── utils/
│      ├── CometChatSettings.ts
│      ├── CometChatApp.tsx
│      ├── customHooks.ts
│      ├── decl.d.ts
│      ├── styleConfig.ts
│   ├── App.css
│   ├── App.tsx
│   ├── index.ts
│── project-info/
│   │── LICENSE.md
│   │── SECURITY.md
│   │── SUPPORT.md
│   │── COMETCHAT_SETTINGS.md
│   │── FOLDER_STRUCTURE.md
│── .gitignore
│── README.md
│── NEXT_JS.md
│── CHANGELOG.md
│── package.json
│── tsconfig.json
```

---

## 1. `src/` – Source Code Directory

This folder contains all the source code for the project.

### 1.1 `CometChat/` – CometChat Integration

Handles CometChat UIKIT integration and related functionalities.

#### 1.1.1 `assets/` – Static Assets

Stores static files such as images, icons, and other resources.

#### 1.1.2 `components/` – UI Components

This folder contains reusable UI components, structured into separate directories for modularity. Each subfolder represents a specific UI feature or component:

- **CometChatAddMembers/** → Adds members to a group.
- **CometChatAlertPopup/** → Displays modal popups for warnings or confirmations.
- **CometChatBannedMembers/** → Manages banned members in a group.
- **CometChatCallLog/** → Displays call history logs.
- **CometChatCreateGroup/** → UI for creating new groups.
- **CometChatDetails/** → Displays detailed user information.
- **CometChatThreadedMessages/** → Handles threaded conversations (replies to messages).
- **CometChatHome/** → The main home screen of the chat app.
- **CometChatJoinGroup/** → Allows users to join group chats.
- **CometChatLogin/** → Login component for authentication.
- **CometChatMessages/** → Core chat interface for 1-on-1 and group chats.
- **CometChatSelector/** → Switches between conversations, calls, users, and groups.
- **CometChatTransferOwnership/** → Facilitates transferring group ownership.

Each folder contains corresponding `.tsx` and `.css` files for UI rendering and styling.

#### 1.1.3 `context/` – Global State Management

Manages global state and context API for the application:

- **AppContext.tsx** → Provides a global context.
- **appReducer.ts** → Defines reducer logic for state management.
- **CometChatContext.tsx** → Handles chat builder settings.

#### 1.1.4 `locales/` – Localization

Contains language translation files for multi-language support.

#### 1.1.5 `customHook/` – Custom React Hooks

Contains reusable custom React hooks for the application:

- **useThemeStyles** → A custom React hook that manages dynamic theme styling for the chat application.


#### 1.1.6 `styles/` – Styling Files

Organized CSS styles to ensure modularity:

- **Component-specific styles** (e.g., `CometChatAddMembers.css`)
- **CometChatApp.css** → Global styles for the entire application.

#### 1.1.7 `utils/` – Utility Functions

Contains reusable helper functions and configuration files:

- **utils.ts** → General utility functions.

#### 1.1.8 Key Supporting Files in `CometChat/`

- **CometChatSettings.ts** → Chat builder settings.
- **customHooks.ts** → Custom React hooks.
- **decl.d.ts** → TypeScript declaration file.
- **styleConfig.ts** → Manages style-related configurations.
- **CometChatApp.tsx** → Main component integrating all CometChat functionalities.
  

---

## 2. Root Files

These files are located at the root of the project and handle configurations, metadata, and documentation.

- **App.css** → Global CSS styles.
- **App.tsx** → Main application entry component.
- **index.tsx** → Entry point for rendering the React application.
- **package.json** → Manages project dependencies and metadata.
- **README.md** → Guide for integrating the CometChat Chat Builder into your React project, including setup, installation, and configuration steps.
- **NEXT_JS.md** → Guide for integrating the CometChat Chat Builder into your Next.js project, including setup, installation, and configuration steps.
- **FOLDER_STRUCTURE.md** → Detailed documentation of the Chat Chat Builder App's folder structure, explaining the purpose of each directory and file for better project organization.
- **BUILDER_SETTINGS.md** → Detailed explanation of the CometChatSettingsInterface, outlining chat, call, layout, and style configurations for a customizable chat application.
- **CHANGELOG.md** → Tracks changes across different versions.
- **LICENSE.md** → Contains project licensing information.
- **SECURITY.md** → Security policies and guidelines.
- **SUPPORT.md** → Information on getting project support.
- **.gitignore** → Specifies files to be ignored by Git.
- **tsconfig.json** → TypeScript configuration file.

---

This structured documentation provides a clear and well-organized overview of the CometChat Chat Builder App's folder structure.
