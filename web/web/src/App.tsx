import { useState, useEffect } from 'react'
import { onAuthStateChanged } from 'firebase/auth'
import { doc, getDoc } from 'firebase/firestore'
import { auth, firestore } from './firebase'
import { initCometChat, loginToCometChat } from './cometchat'
import SplashScreen from './components/SplashScreen'
import Login from './components/Login'
import ProfileSetup from './components/ProfileSetup'
import ChatInterface from './components/ChatInterface'
import './App.css'

type AppView = 'splash' | 'login' | 'profileSetup' | 'chat'

function App() {
  const [currentView, setCurrentView] = useState<AppView>('splash')
  const [isInitializing, setIsInitializing] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    // Initialize CometChat
    const initializeApp = async () => {
      try {
        await initCometChat()
        console.log('CometChat initialized')

        // Check Firebase auth state
        const unsubscribe = onAuthStateChanged(auth, async (user) => {
          if (user) {
            console.log('User logged in:', user.uid)

            // Check if profile is complete
            const userDoc = await getDoc(doc(firestore, 'users', user.uid))

            if (userDoc.exists() && userDoc.data().displayName) {
              // Profile complete, login to CometChat
              try {
                await loginToCometChat(user.uid)
                setCurrentView('chat')
              } catch (err) {
                console.error('CometChat login error:', err)
                setError('Failed to connect to chat service')
              }
            } else {
              // Profile incomplete, show setup
              setCurrentView('profileSetup')
            }
          } else {
            console.log('User not logged in')
            setCurrentView('login')
          }
          setIsInitializing(false)
        })

        return unsubscribe
      } catch (err) {
        console.error('Initialization error:', err)
        setError('Failed to initialize application')
        setIsInitializing(false)
      }
    }

    initializeApp()
  }, [])

  const handleLoginSuccess = () => {
    setCurrentView('chat')
  }

  const handleProfileSetupNeeded = () => {
    setCurrentView('profileSetup')
  }

  const handleProfileSetupComplete = () => {
    setCurrentView('chat')
  }

  const handleLogout = () => {
    setCurrentView('login')
  }

  const handleSplashComplete = () => {
    setCurrentView(isInitializing ? 'login' : currentView === 'splash' ? 'login' : currentView)
  }

  if (currentView === 'splash') {
    return <SplashScreen onComplete={handleSplashComplete} />
  }

  if (isInitializing) {
    return (
      <div className="loading-container">
        <div className="loading-spinner"></div>
        <p>Initializing...</p>
      </div>
    )
  }

  if (error) {
    return (
      <div className="error-container">
        <h2>Error</h2>
        <p>{error}</p>
        <button onClick={() => window.location.reload()}>Reload</button>
      </div>
    )
  }

  return (
    <div className="app">
      {currentView === 'login' && (
        <Login
          onLoginSuccess={handleLoginSuccess}
          onProfileSetupNeeded={handleProfileSetupNeeded}
        />
      )}
      {currentView === 'profileSetup' && (
        <ProfileSetup onComplete={handleProfileSetupComplete} />
      )}
      {currentView === 'chat' && (
        <ChatInterface onLogout={handleLogout} />
      )}
    </div>
  )
}

export default App
