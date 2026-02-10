import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './Dashboard.css';

const Dashboard = () => {
  const navigate = useNavigate();
  const user = JSON.parse(localStorage.getItem('user') || '{}');
  const [projects, setProjects] = useState([]);
  const [showSettings, setShowSettings] = useState(false);
  const [settings, setSettings] = useState({
    model: 'gpt-4',
    pollingInterval: 5000,
    agents: {
      wbs: true,
      userStories: true,
      techSpec: true,
      nfr: true,
      architecture: true,
      sprintPlan: true,
      testScenarios: true,
      performanceTest: true,
      workspace: true
    }
  });
  // Mock projects data - in real app, fetch from API
  useEffect(() => {
    // Simulate loading projects
    const mockProjects = [
      { id: 1, name: 'E-commerce Platform', status: 'active', lastUpdated: '2024-01-15' },
      { id: 2, name: 'Mobile Banking App', status: 'completed', lastUpdated: '2024-01-10' },
      { id: 3, name: 'Healthcare Management', status: 'active', lastUpdated: '2024-01-14' }
    ];
    setProjects(mockProjects);
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('user');
    window.location.href = '/';
  };

  const handleSettingsChange = (key, value) => {
    setSettings(prev => ({
      ...prev,
      [key]: value
    }));
  };

  const handleAgentToggle = (agentName) => {
    setSettings(prev => ({
      ...prev,
      agents: {
        ...prev.agents,
        [agentName]: !prev.agents[agentName]
      }
    }));
  };

  const handleSaveSettings = () => {
    // In real app, save to backend
    localStorage.setItem('sdlc_settings', JSON.stringify(settings));
    alert('Settings saved successfully!');
    setShowSettings(false);
  };

  return (
    <div className="dashboard">
      <div className="dashboard-header">
        <div className="header-left">
          <h2>SDLC Automation Platform</h2>
          <span className="user-info">Welcome, {user.name || user.email || 'User'}</span>
        </div>
        <div className="header-actions">
          <button 
            onClick={() => setShowSettings(true)} 
            className="settings-btn"
            title="Settings"
          >
            ⚙️ Settings
          </button>
          <button onClick={handleLogout} className="logout-btn">
            Logout
          </button>
        </div>
      </div>

      <div className="dashboard-content">
        <div className="projects-section">
          <div className="projects-header">
            <h3>Projects</h3>
            <button 
              onClick={() => navigate('/new-project')} 
              className="create-project-btn"
            >
              + New Project
            </button>
          </div>

          {projects.length === 0 ? (
            <div className="empty-state">
              <p>No projects yet. Create your first project to get started!</p>
            </div>
          ) : (
            <div className="projects-grid">
              {projects.map(project => (
                <div key={project.id} className="project-card">
                  <div className="project-header">
                    <h4>{project.name}</h4>
                    <span className={`status-badge ${project.status}`}>
                      {project.status}
                    </span>
                  </div>
                  <div className="project-info">
                    <span className="last-updated">Last updated: {project.lastUpdated}</span>
                  </div>
                  <div className="project-actions">
                    <button className="view-btn">View</button>
                    <button className="edit-btn">Edit</button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Settings Modal */}
      {showSettings && (
        <div className="modal-overlay" onClick={() => setShowSettings(false)}>
          <div className="modal-content settings-modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Settings</h3>
              <button 
                className="close-btn"
                onClick={() => setShowSettings(false)}
              >
                ×
              </button>
            </div>
            <div className="modal-body">
              {/* Model Selection */}
              <div className="settings-section">
                <h4>AI Model Selection</h4>
                <div className="form-group">
                  <label>Select Model</label>
                  <select
                    value={settings.model}
                    onChange={(e) => handleSettingsChange('model', e.target.value)}
                  >
                    <option value="gpt-4">GPT-4</option>
                    <option value="gpt-4-turbo">GPT-4 Turbo</option>
                    <option value="gpt-3.5-turbo">GPT-3.5 Turbo</option>
                    <option value="claude-3-opus">Claude 3 Opus</option>
                    <option value="claude-3-sonnet">Claude 3 Sonnet</option>
                    <option value="claude-3-haiku">Claude 3 Haiku</option>
                  </select>
                </div>
              </div>

              {/* Polling Interval */}
              <div className="settings-section">
                <h4>Polling Configuration</h4>
                <div className="form-group">
                  <label>Polling Interval (milliseconds)</label>
                  <input
                    type="number"
                    value={settings.pollingInterval}
                    onChange={(e) => handleSettingsChange('pollingInterval', parseInt(e.target.value))}
                    min="1000"
                    step="1000"
                  />
                  <small>Current: {settings.pollingInterval}ms ({settings.pollingInterval / 1000}s)</small>
                </div>
              </div>

              {/* Agent Selection */}
              <div className="settings-section">
                <h4>Agent Configuration</h4>
                <p className="section-description">Select which agents to enable for automation:</p>
                <div className="agents-grid">
                  <div className="agent-item">
                    <label className="checkbox-label">
                      <input
                        type="checkbox"
                        checked={settings.agents.wbs}
                        onChange={() => handleAgentToggle('wbs')}
                      />
                      <span>WBS Agent</span>
                    </label>
                  </div>
                  <div className="agent-item">
                    <label className="checkbox-label">
                      <input
                        type="checkbox"
                        checked={settings.agents.userStories}
                        onChange={() => handleAgentToggle('userStories')}
                      />
                      <span>User Stories Agent</span>
                    </label>
                  </div>
                  <div className="agent-item">
                    <label className="checkbox-label">
                      <input
                        type="checkbox"
                        checked={settings.agents.techSpec}
                        onChange={() => handleAgentToggle('techSpec')}
                      />
                      <span>Tech Spec Agent</span>
                    </label>
                  </div>
                  <div className="agent-item">
                    <label className="checkbox-label">
                      <input
                        type="checkbox"
                        checked={settings.agents.nfr}
                        onChange={() => handleAgentToggle('nfr')}
                      />
                      <span>NFR Agent</span>
                    </label>
                  </div>
                  <div className="agent-item">
                    <label className="checkbox-label">
                      <input
                        type="checkbox"
                        checked={settings.agents.architecture}
                        onChange={() => handleAgentToggle('architecture')}
                      />
                      <span>Architecture Agent</span>
                    </label>
                  </div>
                  <div className="agent-item">
                    <label className="checkbox-label">
                      <input
                        type="checkbox"
                        checked={settings.agents.sprintPlan}
                        onChange={() => handleAgentToggle('sprintPlan')}
                      />
                      <span>Sprint Plan Agent</span>
                    </label>
                  </div>
                  <div className="agent-item">
                    <label className="checkbox-label">
                      <input
                        type="checkbox"
                        checked={settings.agents.testScenarios}
                        onChange={() => handleAgentToggle('testScenarios')}
                      />
                      <span>Test Scenarios Agent</span>
                    </label>
                  </div>
                  <div className="agent-item">
                    <label className="checkbox-label">
                      <input
                        type="checkbox"
                        checked={settings.agents.performanceTest}
                        onChange={() => handleAgentToggle('performanceTest')}
                      />
                      <span>Performance Test Agent</span>
                    </label>
                  </div>
                  <div className="agent-item">
                    <label className="checkbox-label">
                      <input
                        type="checkbox"
                        checked={settings.agents.workspace}
                        onChange={() => handleAgentToggle('workspace')}
                      />
                      <span>Workspace Agent</span>
                    </label>
                  </div>
                </div>
              </div>
            </div>
            <div className="modal-footer">
              <button 
                className="cancel-btn"
                onClick={() => setShowSettings(false)}
              >
                Cancel
              </button>
              <button 
                className="save-btn"
                onClick={handleSaveSettings}
              >
                Save Settings
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Dashboard;
