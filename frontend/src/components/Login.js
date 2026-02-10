import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './Login.css';

const Login = () => {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    if (!username.trim() || !password.trim()) {
      setError('Please enter both username/email and password.');
      setLoading(false);
      return;
    }

    // Store user info and token locally, then go straight to dashboard
    const user = {
      name: username,
      email: username.includes('@') ? username : username + '@github.local'
    };
    localStorage.setItem('auth_token', 'session_' + Date.now());
    localStorage.setItem('user', JSON.stringify(user));

    // Navigate to dashboard
    navigate('/dashboard', { replace: true });
  };

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

        <form onSubmit={handleSubmit} className="login-form">
          {error && (
            <div className="error-message">
              {error}
            </div>
          )}

          <div className="form-group">
            <label htmlFor="username" className="form-label">
              GitHub username or email
            </label>
            <input
              id="username"
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="form-input"
              placeholder="Your GitHub username or email address"
              autoFocus
              required
            />
          </div>

          <div className="form-group">
            <div className="password-label-row">
              <label htmlFor="password" className="form-label">
                GitHub Password
              </label>
              <a href="https://github.com/password_reset" target="_blank" rel="noopener noreferrer" className="forgot-password">
                Forgot password?
              </a>
            </div>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="form-input"
              placeholder="Enter your GitHub password"
              required
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="login-button"
          >
            {loading ? 'Signing in...' : 'Sign in'}
          </button>
        </form>
      </div>

      <div className="signup-box">
        New to SDLC Platform? <a href="#" className="signup-link" onClick={(e) => e.preventDefault()}>Create an account.</a>
      </div>
    </div>
  );
};

export default Login;
