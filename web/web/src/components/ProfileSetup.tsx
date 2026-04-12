import { useState, useRef, FormEvent } from 'react';
import { doc, getDoc, updateDoc } from 'firebase/firestore';
import { ref, uploadBytes, getDownloadURL } from 'firebase/storage';
import { httpsCallable } from 'firebase/functions';
import { auth, firestore, storage, functions } from '../firebase';
import { loginToCometChat } from '../cometchat';
import './ProfileSetup.css';

interface ProfileSetupProps {
  onComplete: () => void;
}

export default function ProfileSetup({ onComplete }: ProfileSetupProps) {
  const [displayName, setDisplayName] = useState('');
  const [selectedImage, setSelectedImage] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleImageSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setSelectedImage(file);
      const url = URL.createObjectURL(file);
      setPreviewUrl(url);
    }
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');

    if (displayName.trim().length < 2) {
      setError('Name must be at least 2 characters');
      return;
    }

    const user = auth.currentUser;
    if (!user) {
      setError('User not logged in');
      return;
    }

    setLoading(true);

    try {
      let photoURL = '';

      // Upload photo if selected
      if (selectedImage) {
        const imageRef = ref(storage, `users/${user.uid}/profile.jpg`);
        await uploadBytes(imageRef, selectedImage);
        photoURL = await getDownloadURL(imageRef);
      }

      // Get existing user data
      const userDoc = await getDoc(doc(firestore, 'users', user.uid));
      const userData = userDoc.data();
      const username = userData?.username || '';
      const email = userData?.email || '';

      // Update Firestore with profile data
      await updateDoc(doc(firestore, 'users', user.uid), {
        displayName: displayName.trim(),
        photoURL: photoURL,
        updatedAt: new Date()
      });

      // Create CometChat user via Cloud Function
      const createCometChatUser = httpsCallable(functions, 'createCometChatUser');
      const result = await createCometChatUser({
        uid: user.uid,
        name: displayName.trim(),
        avatar: photoURL,
        username: username,
        email: email
      });

      const resultData = result.data as { authToken?: string };
      const authToken = resultData.authToken;

      if (authToken) {
        // Login to CometChat
        await loginToCometChat(user.uid);

        // Mark as synced
        await updateDoc(doc(firestore, 'users', user.uid), {
          cometChatSynced: true
        });

        console.log('Profile setup complete');
        onComplete();
      } else {
        setError('Failed to get auth token');
        setLoading(false);
      }
    } catch (err: any) {
      console.error('Profile setup error:', err);
      setError(err.message || 'Failed to set up profile. Please try again.');
      setLoading(false);
    }
  };

  return (
    <div className="profile-setup-container">
      <div className="profile-setup-card">
        <h1>Complete Your Profile</h1>
        <p className="profile-setup-subtitle">Add your details to get started</p>

        <form onSubmit={handleSubmit} className="profile-setup-form">
          <div className="photo-section">
            <div className="photo-preview">
              {previewUrl ? (
                <img src={previewUrl} alt="Profile preview" />
              ) : (
                <div className="photo-placeholder">
                  <svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                    <circle cx="12" cy="7" r="4"></circle>
                  </svg>
                </div>
              )}
            </div>

            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              onChange={handleImageSelect}
              style={{ display: 'none' }}
              disabled={loading}
            />

            <button
              type="button"
              className="change-photo-button"
              onClick={() => fileInputRef.current?.click()}
              disabled={loading}
            >
              {previewUrl ? 'Change Photo' : 'Add Photo'}
            </button>
            <p className="photo-hint">Optional</p>
          </div>

          <div className="form-group">
            <label htmlFor="displayName">Display Name</label>
            <input
              id="displayName"
              type="text"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              placeholder="Enter your name"
              required
              disabled={loading}
            />
          </div>

          {error && <div className="error-message">{error}</div>}

          <button type="submit" className="profile-setup-button" disabled={loading}>
            {loading ? 'Setting up...' : 'Complete Setup'}
          </button>
        </form>
      </div>
    </div>
  );
}
