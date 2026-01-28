import React from 'react';
import RequirementUpload from '../components/RequirementUpload';
import './Dashboard.css';

const Dashboard = () => {
  const user = JSON.parse(localStorage.getItem('user') || '{}');

  const handleLogout = () => {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('user');
    window.location.href = '/';
  };

  return (
    <div className="dashboard">
      <div className="dashboard-header">
        <h2>Welcome, {user.name || user.email || 'User'}</h2>
        <button onClick={handleLogout} className="logout-btn">
          Logout
        </button>
      </div>
      <RequirementUpload />
    </div>
  );
};

export default Dashboard;
