import ReactDOM from "react-dom/client";
import {
  CometChatUIKit,
  UIKitSettingsBuilder,
} from "@cometchat/chat-uikit-react";
import React from "react";
import App from "App";
import { setupLocalization } from "CometChat/utils/utils";
import cometChatLogo from "../src/CometChat/assets/cometchat_logo.svg";
import { CometChatProvider } from "CometChat/context/CometChatContext";

export const COMETCHAT_CONSTANTS = {
  APP_ID: "281421fd397d9bf6",
  REGION: "us",
  AUTH_KEY: "5d7e15509f2034cf002555883a2e732d412d358a",
};

const rootEl = document.getElementById("root") as HTMLElement;
const root = ReactDOM.createRoot(rootEl);

function renderLoading() {
  root.render(
    <div style={{
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      justifyContent: "center",
      height: "100vh",
      width: "100vw",
      gap: "16px",
      fontFamily: "sans-serif",
      background: "#fff",
    }}>
      <img src={cometChatLogo} alt="Amatyma" style={{ width: 64, height: 64 }} />
      <p style={{ fontSize: 15, color: "#888", margin: 0 }}>Initialising…</p>
    </div>
  );
}

function renderError(message: string) {
  root.render(
    <div style={{
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      justifyContent: "center",
      height: "100vh",
      width: "100vw",
      gap: "16px",
      fontFamily: "sans-serif",
      background: "#fff",
      padding: "24px",
      textAlign: "center",
    }}>
      <img src={cometChatLogo} alt="Amatyma" style={{ width: 56, height: 56 }} />
      <p style={{ fontSize: 16, color: "#e53e3e", margin: 0, fontWeight: 600 }}>
        Could not connect
      </p>
      <p style={{ fontSize: 14, color: "#888", margin: 0 }}>{message}</p>
      <button
        onClick={() => window.location.reload()}
        style={{
          marginTop: 8,
          padding: "10px 28px",
          background: "#6851D6",
          color: "#fff",
          border: "none",
          borderRadius: 8,
          fontSize: 14,
          cursor: "pointer",
        }}
      >
        Retry
      </button>
    </div>
  );
}

if (
  COMETCHAT_CONSTANTS.APP_ID &&
  COMETCHAT_CONSTANTS.REGION &&
  COMETCHAT_CONSTANTS.AUTH_KEY
) {
  // Show loading immediately — screen is never blank while init runs
  renderLoading();

  const uiKitSettings = new UIKitSettingsBuilder()
    .setAppId(COMETCHAT_CONSTANTS.APP_ID)
    .setRegion(COMETCHAT_CONSTANTS.REGION)
    .setAuthKey(COMETCHAT_CONSTANTS.AUTH_KEY)
    .subscribePresenceForAllUsers()
    .build();

  // Race the init promise against a 15-second timeout.
  // On iOS Safari, the promise can hang indefinitely — the timeout
  // ensures the user sees an actionable error instead of a blank screen.
  const timeout = new Promise<never>((_, reject) =>
    setTimeout(() => reject(new Error("Connection timed out. Check your network and try again.")), 15000)
  );

  Promise.race([CometChatUIKit.init(uiKitSettings) ?? Promise.resolve(), timeout])
    .then(() => {
      setupLocalization();
      root.render(
        <CometChatProvider>
          <App />
        </CometChatProvider>
      );
    })
    .catch((err: Error) => {
      renderError(err.message ?? "Please check your connection and try again.");
    });
} else {
  root.render(
    <div className="App" style={{ gap: "20px" }}>
      <div className="cometchat-credentials__logo">
        <img src={cometChatLogo} alt="CometChat Logo" />
      </div>
      <div className="cometchat-credentials__header">
        CometChat App credentials are missing. Please add them in{" "}
        <code>index.tsx</code> to continue.
      </div>
    </div>
  );
}
