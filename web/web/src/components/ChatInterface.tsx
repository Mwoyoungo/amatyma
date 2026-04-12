import { CometChatConversations } from "@cometchat/chat-uikit-react";
import { signOut } from 'firebase/auth';
import { auth } from '../firebase';
import { logoutFromCometChat } from '../cometchat';
import './ChatInterface.css';

interface ChatInterfaceProps {
  onLogout: () => void;
}

export default function ChatInterface({ onLogout }: ChatInterfaceProps) {
  const handleLogout = async () => {
    try {
      // Logout from CometChat
      await logoutFromCometChat();

      // Logout from Firebase
      await signOut(auth);

      console.log('Logout successful');
      onLogout();
    } catch (error) {
      console.error('Logout error:', error);
      // Force logout anyway
      onLogout();
    }
  };

  return (
    <div className="chat-interface">
      <div className="chat-header">
        <h2>Amatyma Chat</h2>
        <button onClick={handleLogout} className="logout-button">
          Logout
        </button>
      </div>

      <div className="chat-container">
        <CometChatConversations />
      </div>
    </div>
  );
}
