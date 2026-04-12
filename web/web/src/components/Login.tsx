import { useState, FormEvent } from 'react';
import { signInWithEmailAndPassword, createUserWithEmailAndPassword, sendPasswordResetEmail } from 'firebase/auth';
import { doc, setDoc, getDoc, query, collection, where, getDocs } from 'firebase/firestore';
import { auth, firestore } from '../firebase';
import { loginToCometChat } from '../cometchat';
import './Login.css';

interface LoginProps {
  onLoginSuccess: () => void;
  onProfileSetupNeeded: () => void;
}

export default function Login({ onLoginSuccess, onProfileSetupNeeded }: LoginProps) {
  const [isSignUpMode, setIsSignUpMode] = useState(false);
  const [showForgotPassword, setShowForgotPassword] = useState(false);
  const [email, setEmail] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  const validateEmail = (email: string): boolean => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  };

  const validateUsername = (username: string): boolean => {
    const usernameRegex = /^[a-z0-9_]{3,20}$/;
    return usernameRegex.test(username);
  };

  const handleSignUp = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccessMessage('');

    // Validation
    if (!validateEmail(email)) {
      setError('Please enter a valid email');
      return;
    }

    if (!validateUsername(username)) {
      setError('Username must be 3-20 characters, lowercase letters, numbers, or underscore');
      return;
    }

    if (password.length < 6) {
      setError('Password must be at least 6 characters');
      return;
    }

    setLoading(true);

    try {
      // Check if username is already taken
      const usersRef = collection(firestore, 'users');
      const q = query(usersRef, where('username', '==', username.toLowerCase()));
      const querySnapshot = await getDocs(q);

      if (!querySnapshot.empty) {
        setError('Username already taken');
        setLoading(false);
        return;
      }

      // Create Firebase account
      const userCredential = await createUserWithEmailAndPassword(auth, email, password);
      const user = userCredential.user;

      // Save username to Firestore
      const userData = {
        uid: user.uid,
        email: email,
        username: username.toLowerCase(),
        cometChatSynced: false,
        createdAt: new Date()
      };

      await setDoc(doc(firestore, 'users', user.uid), userData);

      console.log('Account created successfully');

      // Navigate to profile setup
      onProfileSetupNeeded();
    } catch (err: any) {
      console.error('Sign up error:', err);
      setError(err.message || 'Sign up failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleLogin = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccessMessage('');

    if (!email) {
      setError('Please enter your email');
      return;
    }

    if (!password) {
      setError('Please enter your password');
      return;
    }

    setLoading(true);

    try {
      // Login to Firebase
      const userCredential = await signInWithEmailAndPassword(auth, email, password);
      const user = userCredential.user;

      console.log('Firebase login successful:', user.uid);

      // Check if profile is complete
      const userDoc = await getDoc(doc(firestore, 'users', user.uid));

      if (userDoc.exists() && userDoc.data().displayName) {
        // Profile complete, login to CometChat
        await loginToCometChat(user.uid);
        console.log('CometChat login successful');
        onLoginSuccess();
      } else {
        // Profile incomplete, go to setup
        onProfileSetupNeeded();
      }
    } catch (err: any) {
      console.error('Login error:', err);
      setError(err.message || 'Login failed. Please check your credentials.');
    } finally {
      setLoading(false);
    }
  };

  const handleForgotPassword = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccessMessage('');

    if (!validateEmail(email)) {
      setError('Please enter a valid email');
      return;
    }

    setLoading(true);

    try {
      await sendPasswordResetEmail(auth, email);
      setSuccessMessage('Password reset email sent! Check your inbox.');
      setTimeout(() => {
        setShowForgotPassword(false);
        setSuccessMessage('');
      }, 3000);
    } catch (err: any) {
      console.error('Password reset error:', err);
      setError(err.message || 'Failed to send reset email.');
    } finally {
      setLoading(false);
    }
  };

  if (showForgotPassword) {
    return (
      <div className="login-container">
        <div className="login-card">
          <h1>Reset Password</h1>
          <p className="login-subtitle">Enter your email to receive a reset link</p>

          <form onSubmit={handleForgotPassword} className="login-form">
            <div className="form-group">
              <label htmlFor="email">Email</label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="Enter your email"
                required
                disabled={loading}
              />
            </div>

            {error && <div className="error-message">{error}</div>}
            {successMessage && <div className="success-message">{successMessage}</div>}

            <button type="submit" className="login-button" disabled={loading}>
              {loading ? 'Sending...' : 'Send Reset Link'}
            </button>

            <p className="toggle-link" onClick={() => setShowForgotPassword(false)}>
              Back to Login
            </p>
          </form>
        </div>
      </div>
    );
  }

  return (
    <div className="login-container">
      <div className="login-card">
        <h1>Amatyma Chat</h1>
        <p className="login-subtitle">
          {isSignUpMode ? 'Create your account' : 'Sign in to continue'}
        </p>

        <form onSubmit={isSignUpMode ? handleSignUp : handleLogin} className="login-form">
          <div className="form-group">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="Enter your email"
              required
              disabled={loading}
            />
          </div>

          {isSignUpMode && (
            <div className="form-group">
              <label htmlFor="username">Username</label>
              <input
                id="username"
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value.toLowerCase())}
                placeholder="Choose a username"
                required
                disabled={loading}
              />
            </div>
          )}

          <div className="form-group">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Enter your password"
              required
              disabled={loading}
            />
          </div>

          {error && <div className="error-message">{error}</div>}
          {successMessage && <div className="success-message">{successMessage}</div>}

          {!isSignUpMode && (
            <p className="forgot-password" onClick={() => setShowForgotPassword(true)}>
              Forgot Password?
            </p>
          )}

          <button type="submit" className="login-button" disabled={loading}>
            {loading ? (isSignUpMode ? 'Creating Account...' : 'Signing in...') : (isSignUpMode ? 'Sign Up' : 'Login')}
          </button>
        </form>

        <p className="toggle-link" onClick={() => {
          setIsSignUpMode(!isSignUpMode);
          setError('');
          setSuccessMessage('');
        }}>
          {isSignUpMode ? 'Already have an account? Login' : "Don't have an account? Sign Up"}
        </p>
      </div>
    </div>
  );
}
