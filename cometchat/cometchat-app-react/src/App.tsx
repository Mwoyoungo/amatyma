import "./App.css";
import React, { useEffect, useState } from "react";
import { CometChat } from "@cometchat/chat-sdk-javascript";
import { CometChatUIKitLoginListener } from "@cometchat/chat-uikit-react";
import { CometChatHome } from "CometChat/components/CometChatHome/CometChatHome";
import CometChatLogin from "CometChat/components/CometChatLogin/CometChatLogin";
import { AppContextProvider } from "CometChat/context/AppContext";
import { useCometChatContext } from "CometChat/context/CometChatContext";
import useSystemColorScheme from "CometChat/customHooks";
import "@cometchat/chat-uikit-react/css-variables.css";
import useThemeStyles from "CometChat/customHook/useThemeStyles";

function App() {
  const [loggedInUser, setLoggedInUser] = useState<CometChat.User | null>(null);

  /**
   * State to store the logged-in user
   * @type {[CometChat.User | null, Function]}
   */
  const { styleFeatures, setStyleFeatures } = useCometChatContext();
  const systemTheme = useSystemColorScheme();
  const getLoggedInUser = CometChatUIKitLoginListener?.getLoggedInUser();
  useThemeStyles(styleFeatures, systemTheme, setStyleFeatures,loggedInUser);

  /**
   * Effect to handle login and logout listeners
   */
  useEffect(() => {
    CometChat.addLoginListener(
      "runnable-sample-app",
      new CometChat.LoginListener({
        loginSuccess: (user: CometChat.User) => {
          console.log("login success");
          setLoggedInUser(user);
        },
        logoutSuccess: () => {
          console.log("logout success");
          setLoggedInUser(null);
        },
      })
    );

    return () => CometChat.removeLoginListener("runnable-sample-app");
  }, []);

  /**
   * Effect to set the logged-in user from CometChat UIKit
   */
  useEffect(() => {
    setLoggedInUser(getLoggedInUser);
  }, [getLoggedInUser]);

   
  return (
    <div className="App">
      <AppContextProvider>
        {loggedInUser ? <CometChatHome /> : <CometChatLogin />}
      </AppContextProvider>
    </div>
  );
}

export default App;
