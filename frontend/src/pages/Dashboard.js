import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './Dashboard.css';

const BACKEND_URL = 'http://localhost:3001/api/v1';

const Dashboard = () => {
  const navigate = useNavigate();
  const user = JSON.parse(localStorage.getItem('user') || '{}');
  const token = localStorage.getItem('auth_token');

  const [repos, setRepos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchTerm, setSearchTerm] = useState('');

  // Create repo modal state
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newRepoName, setNewRepoName] = useState('');
  const [newRepoDescription, setNewRepoDescription] = useState('');
  const [newRepoPrivate, setNewRepoPrivate] = useState(false);
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState('');

  // Settings modal state
  const [showSettings, setShowSettings] = useState(false);
  const [settings, setSettings] = useState(() => {
    const saved = localStorage.getItem('sdlc_settings');
    return saved ? JSON.parse(saved) : {
      model: 'gpt-4',
      pollingInterval: 5000,
      agents: {
        wbs: true, userStories: true, techSpec: true, nfr: true,
        architecture: true, sprintPlan: true, testScenarios: true,
        performanceTest: true, workspace: true
      }
    };
  });

  // Fetch user's GitHub repos on mount
  useEffect(() => {
    fetchRepos();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const fetchRepos = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await fetch(`${BACKEND_URL}/github/repos`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });

      if (response.status === 401) {
        localStorage.removeItem('auth_token');
        localStorage.removeItem('user');
        navigate('/', { replace: true });
        return;
      }

      if (!response.ok) {
        throw new Error('Failed to fetch repositories');
      }

      const data = await response.json();
      setRepos(data.repos || []);
    } catch (err) {
      setError(err.message || 'Failed to load repositories');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateRepo = async () => {
    if (!newRepoName.trim()) {
      setCreateError('Repository name is required');
      return;
    }

    setCreating(true);
    setCreateError('');

    try {
      const response = await fetch(`${BACKEND_URL}/github/repos`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          name: newRepoName.trim(),
          description: newRepoDescription.trim() || 'Created by SDLC Automation Platform',
          isPrivate: newRepoPrivate
        })
      });

      if (response.status === 401) {
        localStorage.removeItem('auth_token');
        localStorage.removeItem('user');
        navigate('/', { replace: true });
        return;
      }

      if (!response.ok) {
        const data = await response.json().catch(() => ({}));
        throw new Error(data.error?.message || 'Failed to create repository');
      }

      const newRepo = await response.json();

      // Close modal and refresh repos
      setShowCreateModal(false);
      setNewRepoName('');
      setNewRepoDescription('');
      setNewRepoPrivate(false);

      // Navigate directly to upload for the new repo
      navigate('/new-project', {
        state: {
          repoOwner: newRepo.owner || user.githubUsername,
          repoName: newRepo.name,
          repoFullName: newRepo.fullName || newRepo.full_name
        }
      });
    } catch (err) {
      setCreateError(err.message);
    } finally {
      setCreating(false);
    }
  };

  const handleSelectRepo = (repo) => {
    navigate('/new-project', {
      state: {
        repoOwner: repo.owner || user.githubUsername,
        repoName: repo.name,
        repoFullName: repo.fullName || repo.full_name
      }
    });
  };

  const handleLogout = () => {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('user');
    window.location.href = '/';
  };

  const handleAgentToggle = (agentName) => {
    setSettings(prev => ({
      ...prev,
      agents: { ...prev.agents, [agentName]: !prev.agents[agentName] }
    }));
  };

  const handleSaveSettings = () => {
    localStorage.setItem('sdlc_settings', JSON.stringify(settings));
    setShowSettings(false);
  };

  // Filter repos by search
  const filteredRepos = repos.filter(repo =>
    repo.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    (repo.description && repo.description.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  const formatDate = (dateStr) => {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now - date;
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
    if (diffDays === 0) return 'today';
    if (diffDays === 1) return 'yesterday';
    if (diffDays < 30) return `${diffDays} days ago`;
    if (diffDays < 365) return `${Math.floor(diffDays / 30)} months ago`;
    return date.toLocaleDateString();
  };

  const languageColors = {
    JavaScript: '#f1e05a', TypeScript: '#3178c6', Python: '#3572A5',
    Java: '#b07219', 'C#': '#178600', Go: '#00ADD8', Rust: '#dea584',
    Ruby: '#701516', PHP: '#4F5D95', HTML: '#e34c26', CSS: '#563d7c',
    Shell: '#89e051', Kotlin: '#A97BFF', Swift: '#F05138', Dart: '#00B4AB'
  };

  return (
    <div className="dashboard">
      {/* Header */}
      <div className="dashboard-header">
        <div className="header-left">
          <div className="header-brand">
            {user.avatarUrl && (
              <img src={user.avatarUrl} alt="avatar" className="user-avatar" />
            )}
            <div>
              <h2>SDLC Automation Platform</h2>
              <span className="user-info">
                {user.githubUsername ? `@${user.githubUsername}` : user.name || user.email || 'User'}
                {user.name && user.githubUsername ? ` · ${user.name}` : ''}
              </span>
            </div>
          </div>
        </div>
        <div className="header-actions">
          <button onClick={() => setShowSettings(true)} className="settings-btn" title="Settings">
            ⚙️ Settings
          </button>
          <button onClick={handleLogout} className="logout-btn">Logout</button>
        </div>
      </div>

      {/* Content */}
      <div className="dashboard-content">
        <div className="projects-section">
          <div className="projects-header">
            <div className="projects-header-left">
              <h3>Your Repositories</h3>
              <span className="repo-count">{repos.length} repos</span>
            </div>
            <div className="projects-header-right">
              <div className="search-box">
                <input
                  type="text"
                  placeholder="Find a repository..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="search-input"
                />
              </div>
              <button onClick={() => setShowCreateModal(true)} className="create-project-btn">
                + New Repository
              </button>
            </div>
          </div>

          {/* Error */}
          {error && (
            <div className="dashboard-error">
              <span>{error}</span>
              <button onClick={fetchRepos} className="retry-btn">Retry</button>
            </div>
          )}

          {/* Loading */}
          {loading && (
            <div className="loading-state">
              <div className="loading-spinner" />
              <p>Loading your repositories from GitHub...</p>
            </div>
          )}

          {/* Empty state */}
          {!loading && !error && filteredRepos.length === 0 && (
            <div className="empty-state">
              {searchTerm ? (
                <p>No repositories match "<strong>{searchTerm}</strong>"</p>
              ) : (
                <>
                  <p>No repositories found on your GitHub account.</p>
                  <p>Create a new repository to get started with SDLC automation!</p>
                  <button onClick={() => setShowCreateModal(true)} className="create-project-btn">
                    + Create Your First Repository
                  </button>
                </>
              )}
            </div>
          )}

          {/* Repos grid */}
          {!loading && filteredRepos.length > 0 && (
            <div className="projects-grid">
              {filteredRepos.map(repo => (
                <div key={repo.id} className="project-card">
                  <div className="project-header">
                    <div className="repo-name-row">
                      <h4>
                        <a href={repo.html_url} target="_blank" rel="noopener noreferrer"
                           className="repo-name-link">
                          {repo.name}
                        </a>
                      </h4>
                      <span className={`visibility-badge ${repo.private ? 'private' : 'public'}`}>
                        {repo.private ? '🔒 Private' : '🌐 Public'}
                      </span>
                    </div>
                  </div>
                  {repo.description && (
                    <p className="repo-description">{repo.description}</p>
                  )}
                  <div className="repo-meta">
                    {repo.language && (
                      <span className="repo-language">
                        <span className="language-dot"
                              style={{ backgroundColor: languageColors[repo.language] || '#ccc' }} />
                        {repo.language}
                      </span>
                    )}
                    {repo.stargazers_count > 0 && (
                      <span className="repo-stars">⭐ {repo.stargazers_count}</span>
                    )}
                    <span className="repo-updated">Updated {formatDate(repo.pushed_at || repo.updated_at)}</span>
                  </div>
                  <div className="project-actions">
                    <button className="select-repo-btn" onClick={() => handleSelectRepo(repo)}>
                      📤 Upload Requirements
                    </button>
                    <a href={repo.html_url} target="_blank" rel="noopener noreferrer" className="view-btn">
                      View on GitHub
                    </a>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Create Repo Modal */}
      {showCreateModal && (
        <div className="modal-overlay" onClick={() => setShowCreateModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Create a New Repository</h3>
              <button className="close-btn" onClick={() => setShowCreateModal(false)}>×</button>
            </div>
            <div className="modal-body">
              {createError && <div className="modal-error">{createError}</div>}
              <div className="form-group">
                <label>Repository name <span className="required">*</span></label>
                <input
                  type="text"
                  value={newRepoName}
                  onChange={(e) => setNewRepoName(e.target.value)}
                  placeholder="e.g. my-sdlc-project"
                  autoFocus
                />
                <small>This will be created under your GitHub account: <strong>@{user.githubUsername}</strong></small>
              </div>
              <div className="form-group">
                <label>Description</label>
                <input
                  type="text"
                  value={newRepoDescription}
                  onChange={(e) => setNewRepoDescription(e.target.value)}
                  placeholder="Short description of your project"
                />
              </div>
              <div className="form-group">
                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={newRepoPrivate}
                    onChange={(e) => setNewRepoPrivate(e.target.checked)}
                  />
                  <span>Private repository</span>
                </label>
              </div>
            </div>
            <div className="modal-footer">
              <button className="cancel-btn" onClick={() => setShowCreateModal(false)}>Cancel</button>
              <button className="create-btn" onClick={handleCreateRepo} disabled={creating || !newRepoName.trim()}>
                {creating ? 'Creating...' : 'Create & Upload Requirements'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Settings Modal */}
      {showSettings && (
        <div className="modal-overlay" onClick={() => setShowSettings(false)}>
          <div className="modal-content settings-modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Settings</h3>
              <button className="close-btn" onClick={() => setShowSettings(false)}>×</button>
            </div>
            <div className="modal-body">
              <div className="settings-section">
                <h4>AI Model Selection</h4>
                <div className="form-group">
                  <label>Select Model</label>
                  <select value={settings.model}
                    onChange={(e) => setSettings(prev => ({ ...prev, model: e.target.value }))}>
                    <option value="gpt-4">GPT-4</option>
                    <option value="gpt-4-turbo">GPT-4 Turbo</option>
                    <option value="gpt-3.5-turbo">GPT-3.5 Turbo</option>
                    <option value="claude-3-opus">Claude 3 Opus</option>
                    <option value="claude-3-sonnet">Claude 3 Sonnet</option>
                    <option value="claude-3-haiku">Claude 3 Haiku</option>
                  </select>
                </div>
              </div>

              <div className="settings-section">
                <h4>Polling Configuration</h4>
                <div className="form-group">
                  <label>Polling Interval (milliseconds)</label>
                  <input type="number" value={settings.pollingInterval}
                    onChange={(e) => setSettings(prev => ({ ...prev, pollingInterval: parseInt(e.target.value) }))}
                    min="1000" step="1000" />
                  <small>Current: {settings.pollingInterval}ms ({settings.pollingInterval / 1000}s)</small>
                </div>
              </div>

              <div className="settings-section">
                <h4>Agent Configuration</h4>
                <p className="section-description">Select which agents to enable for automation:</p>
                <div className="agents-grid">
                  {Object.entries({
                    wbs: 'WBS Agent', userStories: 'User Stories Agent', techSpec: 'Tech Spec Agent',
                    nfr: 'NFR Agent', architecture: 'Architecture Agent', sprintPlan: 'Sprint Plan Agent',
                    testScenarios: 'Test Scenarios Agent', performanceTest: 'Performance Test Agent',
                    workspace: 'Workspace Agent'
                  }).map(([key, label]) => (
                    <div className="agent-item" key={key}>
                      <label className="checkbox-label">
                        <input type="checkbox" checked={settings.agents[key]}
                          onChange={() => handleAgentToggle(key)} />
                        <span>{label}</span>
                      </label>
                    </div>
                  ))}
                </div>
              </div>
            </div>
            <div className="modal-footer">
              <button className="cancel-btn" onClick={() => setShowSettings(false)}>Cancel</button>
              <button className="save-btn" onClick={handleSaveSettings}>Save Settings</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Dashboard;
