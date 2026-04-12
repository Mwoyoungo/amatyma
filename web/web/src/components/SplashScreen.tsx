import { useEffect } from 'react';
import splashImage from '../assets/splash_screen.jpg';
import logoImage from '../assets/logo.jpg';
import './SplashScreen.css';

interface SplashScreenProps {
  onComplete: () => void;
}

export default function SplashScreen({ onComplete }: SplashScreenProps) {
  useEffect(() => {
    // Show splash for 3 seconds then fade out
    const timer = setTimeout(() => {
      onComplete();
    }, 3000);

    return () => clearTimeout(timer);
  }, [onComplete]);

  return (
    <div className="splash-screen">
      <div className="splash-background">
        <img src={splashImage} alt="Amatyma" className="splash-bg-image" />
        <div className="splash-overlay"></div>
      </div>

      <div className="splash-content">
        <div className="splash-logo-container">
          <img src={logoImage} alt="Amatyma Logo" className="splash-logo" />
        </div>
        <h1 className="splash-title">Amatyma</h1>
        <p className="splash-subtitle">Connect. Chat. Communicate.</p>

        <div className="splash-loader">
          <div className="splash-loader-bar"></div>
        </div>
      </div>
    </div>
  );
}
