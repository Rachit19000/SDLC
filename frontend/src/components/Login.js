import React, { useState } from 'react';
import './Login.css';

const BACKEND_URL = 'http://localhost:3001/api/v1';

const Login = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleGitHubSignIn = () => {
    setLoading(true);
    setError('');
    // Redirect the browser to the backend's GitHub OAuth initiation endpoint.
    // The backend will redirect to GitHub, which authenticates the user,
    // then GitHub redirects back to the backend callback, which redirects
    // to the frontend /oauth/callback with token + user info.
    window.location.href = `${BACKEND_URL}/auth/github`;
  };

  // Check if there's an error passed via URL (e.g., from a failed OAuth attempt)
  React.useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const oauthError = params.get('error');
    if (oauthError) {
      setError(oauthError);
      // Clean up URL
      window.history.replaceState({}, '', '/');
    }
  }, []);

  return (
    <div className="login-container">
      <div className="login-box">
        {/* GitHub Logo */}
        <div className="github-logo">
          <svg height="48" viewBox="0 0 16 16" width="48" fill="currentColor">
            <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"></path>
          </svg>
        </div>
        
        <h1 className="login-title">SDLC Automation Platform</h1>
        <p className="login-subtitle">Sign in with your GitHub account</p>

        {error && (
          <div className="error-message">
            {error}
          </div>
        )}

        <button
          onClick={handleGitHubSignIn}
          disabled={loading}
          className="login-button github-oauth-btn"
        >
          <svg height="20" viewBox="0 0 16 16" width="20" fill="currentColor" style={{ marginRight: '8px' }}>
            <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"></path>
          </svg>
          {loading ? 'Redirecting to GitHub...' : 'Sign in with GitHub'}
        </button>

        <p className="oauth-info">
          You will be redirected to GitHub to authorize this application.
          We never see or store your GitHub password.
        </p>
      </div>

      <div className="signup-box">
        New to GitHub? <a href="https://github.com/join" target="_blank" rel="noopener noreferrer" className="signup-link">Create an account.</a>
      </div>
    </div>
  );
};

export default Login;
