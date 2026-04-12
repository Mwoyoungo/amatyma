import { CometChat } from "@cometchat/chat-sdk-javascript";
import { CometChatUIKit, UIKitSettingsBuilder } from "@cometchat/chat-uikit-react";

const COMETCHAT_CONSTANTS = {
  APP_ID: "281421fd397d9bf6",
  REGION: "us",
  AUTH_KEY: "5d7e15509f2034cf002555883a2e732d412d358a",
};

export const initCometChat = async (): Promise<void> => {
  const UIKitSettings = new UIKitSettingsBuilder()
    .setAppId(COMETCHAT_CONSTANTS.APP_ID)
    .setRegion(COMETCHAT_CONSTANTS.REGION)
    .setAuthKey(COMETCHAT_CONSTANTS.AUTH_KEY)
    .subscribePresenceForAllUsers()
    .build();

  return CometChatUIKit.init(UIKitSettings)
    .then(() => {
      console.log("CometChat initialization completed successfully");
    })
    .catch((error) => {
      console.error("CometChat initialization failed:", error);
      throw error;
    });
};

export const loginToCometChat = async (uid: string): Promise<CometChat.User> => {
  return CometChatUIKit.login(uid)
    .then((user) => {
      console.log("CometChat login successful:", user.getName());
      return user;
    })
    .catch((error) => {
      console.error("CometChat login failed:", error);
      throw error;
    });
};

export const logoutFromCometChat = async (): Promise<void> => {
  return CometChat.logout()
    .then(() => {
      console.log("CometChat logout successful");
    })
    .catch((error) => {
      console.error("CometChat logout failed:", error);
      throw error;
    });
};
