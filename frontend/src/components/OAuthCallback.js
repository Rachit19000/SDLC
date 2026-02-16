import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

const OAuthCallback = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [error, setError] = useState('');

  useEffect(() => {
    const token = searchParams.get('token');
    const name = searchParams.get('name');
    const email = searchParams.get('email');
    const id = searchParams.get('id');
    const githubUsername = searchParams.get('githubUsername');
    const oauthError = searchParams.get('error');

    if (oauthError) {
      setError(oauthError);
      setTimeout(() => {
        navigate('/?error=' + encodeURIComponent(oauthError), { replace: true });
      }, 3000);
      return;
    }

    if (token && name) {
      // Store auth data in localStorage
      localStorage.setItem('auth_token', token);
      localStorage.setItem('user', JSON.stringify({
        id: id || '',
        name: name,
        email: email || '',
        githubUsername: githubUsername || ''
      }));

      // Redirect to dashboard
      navigate('/dashboard', { replace: true });
    } else {
      setError('Authentication failed: missing token or user data.');
      setTimeout(() => {
        navigate('/', { replace: true });
      }, 3000);
    }
  }, [searchParams, navigate]);

  if (error) {
    return (
      <div style={{
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%)',
        fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif'
      }}>
        <div style={{
          background: '#fff',
          border: '1px solid #d0d7de',
          borderRadius: '8px',
          padding: '32px',
          maxWidth: '420px',
          width: '100%',
          textAlign: 'center'
        }}>
          <div style={{ fontSize: '48px', marginBottom: '16px' }}>⚠️</div>
          <h2 style={{ color: '#d1242f', marginBottom: '12px' }}>Authentication Failed</h2>
          <p style={{ color: '#57606a', fontSize: '14px', lineHeight: '1.5' }}>{error}</p>
          <p style={{ color: '#8b949e', fontSize: '12px', marginTop: '16px' }}>
            Redirecting to login page...
          </p>
        </div>
      </div>
    );
  }

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      background: 'linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%)',
      fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif'
    }}>
      <div style={{
        background: '#fff',
        border: '1px solid #d0d7de',
        borderRadius: '8px',
        padding: '32px',
        maxWidth: '420px',
        width: '100%',
        textAlign: 'center'
      }}>
        <div style={{
          width: '40px',
          height: '40px',
          border: '3px solid #d0d7de',
          borderTopColor: '#0969da',
          borderRadius: '50%',
          animation: 'spin 0.8s linear infinite',
          margin: '0 auto 16px auto'
        }} />
        <h2 style={{ color: '#24292f', marginBottom: '8px' }}>Signing you in...</h2>
        <p style={{ color: '#57606a', fontSize: '14px' }}>
          Completing GitHub authentication
        </p>
        <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
      </div>
    </div>
  );
};

export default OAuthCallback;
