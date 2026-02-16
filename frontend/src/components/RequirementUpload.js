import React, { useState, useRef, useCallback, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import './RequirementUpload.css';

const BACKEND_URL = 'http://localhost:3001/api/v1';

const RequirementUpload = () => {
  const navigate = useNavigate();
  const location = useLocation();

  // Repo selection state
  const [repos, setRepos] = useState([]);
  const [loadingRepos, setLoadingRepos] = useState(true);
  const [selectedRepo, setSelectedRepo] = useState(null);
  const [showRepoSelector, setShowRepoSelector] = useState(false);

  // Try to get repo from route state (if navigated from Dashboard)
  const routeRepoOwner = location.state?.repoOwner;
  const routeRepoName = location.state?.repoName;

  const [uploadMethod, setUploadMethod] = useState('file');
  const [pastedText, setPastedText] = useState('');
  const [fileName, setFileName] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const fileInputRef = useRef(null);
  const dropZoneRef = useRef(null);
  const [isDragging, setIsDragging] = useState(false);

  // Progress tracking state
  const [showProgress, setShowProgress] = useState(false);
  const [progress, setProgress] = useState(0);
  const [stage, setStage] = useState('');
  const [jobId, setJobId] = useState('');

  // Fetch user's repos on mount
  useEffect(() => {
    fetchRepos();
  }, []);

  // Set selected repo from route state if available
  useEffect(() => {
    if (routeRepoOwner && routeRepoName) {
      const repo = repos.find(r => r.owner === routeRepoOwner && r.name === routeRepoName);
      if (repo) {
        setSelectedRepo(repo);
      }
    } else if (repos.length > 0 && !selectedRepo) {
      // Auto-select first repo if none selected
      setSelectedRepo(repos[0]);
    }
  }, [repos, routeRepoOwner, routeRepoName]);

  const fetchRepos = async () => {
    setLoadingRepos(true);
    setError('');
    try {
      const token = localStorage.getItem('auth_token');
      if (!token) {
        navigate('/', { replace: true });
        return;
      }

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
      setLoadingRepos(false);
    }
  };

  const handleFileSelect = (e) => {
    const file = e.target.files[0];
    if (file) {
      setFileName(file.name);
      setError('');
    }
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);

    const files = e.dataTransfer.files;
    if (files && files.length > 0) {
      const file = files[0];
      if (file.type === 'application/pdf' ||
          file.type === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' ||
          file.type === 'text/plain') {
        const dataTransfer = new DataTransfer();
        dataTransfer.items.add(file);
        fileInputRef.current.files = dataTransfer.files;
        setFileName(file.name);
        setError('');
      } else {
        setError('Please upload a PDF, DOCX, or TXT file');
      }
    }
  };

  const pollJobStatus = useCallback(async (jobId, token) => {
    try {
      const response = await fetch(`${BACKEND_URL}/jobs/${jobId}`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (response.ok) {
        const data = await response.json();
        setProgress(data.progress);
        setStage(data.stage);

        if (data.status === 'COMPLETED') {
          setLoading(false);
          setShowProgress(false);
          let message = `Requirements processed and user stories generated! Uploaded to ${selectedRepo.fullName}.`;
          if (data.githubFileUrl) {
            message += `\nView file: ${data.githubFileUrl}`;
          }
          setSuccess(message);
          setFileName('');
          setPastedText('');
          if (fileInputRef.current) fileInputRef.current.value = '';
        } else if (data.status === 'FAILED') {
          setLoading(false);
          setShowProgress(false);
          setError(data.error || 'Processing failed');
        } else {
          setTimeout(() => pollJobStatus(jobId, token), 1000);
        }
      }
    } catch (err) {
      console.error('Poll error:', err);
      setLoading(false);
      setShowProgress(false);
      setError('Failed to check job status');
    }
  }, [selectedRepo]);

  const connectSSE = useCallback((jobId, token) => {
    const eventSource = new EventSource(
      `${BACKEND_URL}/jobs/${jobId}/progress`
    );

    eventSource.addEventListener('progress', (event) => {
      try {
        const data = JSON.parse(event.data);
        setProgress(data.progress);
        setStage(data.stage);

        if (data.status === 'COMPLETED') {
          eventSource.close();
          setLoading(false);
          setShowProgress(false);

          let message = `Requirements processed and user stories generated! Uploaded to ${selectedRepo.fullName}.`;
          if (data.githubFileUrl) {
            message += `\nView file: ${data.githubFileUrl}`;
          }
          setSuccess(message);
          setFileName('');
          setPastedText('');
          if (fileInputRef.current) fileInputRef.current.value = '';
        } else if (data.status === 'FAILED') {
          eventSource.close();
          setLoading(false);
          setShowProgress(false);
          setError(data.error || 'Processing failed');
        }
      } catch (err) {
        console.error('SSE parse error:', err);
      }
    });

    eventSource.onerror = () => {
      eventSource.close();
      console.warn('SSE connection lost. Falling back to polling...');
      setTimeout(() => pollJobStatus(jobId, token), 1000);
    };

    return eventSource;
  }, [pollJobStatus, selectedRepo]);

  const handleUpload = async () => {
    if (!selectedRepo) {
      setError('Please select a repository first');
      return;
    }

    setError('');
    setSuccess('');
    setLoading(true);
    setProgress(0);
    setStage('');
    setShowProgress(false);

    try {
      const token = localStorage.getItem('auth_token');

      if (!token) {
        localStorage.removeItem('auth_token');
        localStorage.removeItem('user');
        navigate('/', { replace: true });
        return;
      }

      let response;

      if (uploadMethod === 'file') {
        const file = fileInputRef.current?.files[0];
        if (!file) {
          throw new Error('Please select a file');
        }

        const formData = new FormData();
        formData.append('file', file);
        formData.append('name', file.name);
        formData.append('repoOwner', selectedRepo.owner);
        formData.append('repoName', selectedRepo.name);

        response = await fetch(`${BACKEND_URL}/requirements/upload`, {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${token}`,
          },
          body: formData,
        });
      } else {
        if (!pastedText.trim()) {
          throw new Error('Please paste your requirement text');
        }

        response = await fetch(`${BACKEND_URL}/requirements/paste`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
          },
          body: JSON.stringify({
            text: pastedText,
            name: 'Pasted Requirements',
            repoOwner: selectedRepo.owner,
            repoName: selectedRepo.name,
          }),
        });
      }

      if (response.status === 401) {
        const data = await response.json().catch(() => ({}));
        const errMsg = data.error?.message || 'Session expired. Please sign in again.';
        setError(errMsg);
        setLoading(false);
        setTimeout(() => {
          localStorage.removeItem('auth_token');
          localStorage.removeItem('user');
          navigate('/', { replace: true });
        }, 3000);
        return;
      }

      if (!response.ok) {
        const contentType = response.headers.get('content-type');
        if (!contentType || !contentType.includes('application/json')) {
          throw new Error('Backend API is not running. Please start the backend server.');
        }
        const data = await response.json();
        throw new Error(data.error?.message || data.message || 'Upload failed');
      }

      const data = await response.json();
      const newJobId = data.jobId;
      setJobId(newJobId);
      setShowProgress(true);
      setStage('Starting...');

      connectSSE(newJobId, token);

    } catch (err) {
      setError(err.message || 'An error occurred during upload');
      setLoading(false);
      setShowProgress(false);
    }
  };

  const handleBackToDashboard = () => {
    navigate('/dashboard');
  };

  if (loadingRepos) {
    return (
      <div className="upload-container">
        <div className="upload-box">
          <h1 className="upload-title">Loading Repositories...</h1>
          <p className="upload-subtitle">Fetching your GitHub repositories...</p>
        </div>
      </div>
    );
  }

  if (repos.length === 0) {
    return (
      <div className="upload-container">
        <div className="upload-box">
          <h1 className="upload-title">No Repositories Found</h1>
          <p className="upload-subtitle">You don't have any GitHub repositories yet.</p>
          <button className="upload-button" onClick={handleBackToDashboard}>
            Go to Dashboard to Create One
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="upload-container">
      <div className="upload-box">
        <button
          type="button"
          onClick={handleBackToDashboard}
          className="back-btn"
          title="Back to Dashboard"
        >
          ← Back to Dashboard
        </button>

        <h1 className="upload-title">SDLC Automation Platform</h1>
        <p className="upload-subtitle">Upload Your Requirements</p>

        {/* Repository Selector */}
        <div className="repo-selector-section">
          <label htmlFor="repo-select" className="repo-select-label">
            Select Repository:
          </label>
          <select
            id="repo-select"
            value={selectedRepo ? `${selectedRepo.owner}/${selectedRepo.name}` : ''}
            onChange={(e) => {
              const [owner, name] = e.target.value.split('/');
              const repo = repos.find(r => r.owner === owner && r.name === name);
              setSelectedRepo(repo);
            }}
            className="repo-select"
            disabled={loading}
          >
            {repos.map((repo) => (
              <option key={repo.id} value={`${repo.owner}/${repo.name}`}>
                {repo.fullName} {repo.isPrivate ? '(Private)' : ''}
              </option>
            ))}
          </select>
          {selectedRepo && (
            <a
              href={selectedRepo.url}
              target="_blank"
              rel="noopener noreferrer"
              className="repo-link"
            >
              View on GitHub →
            </a>
          )}
        </div>

        {/* Method Selection */}
        <div className="method-selection">
          <button
            type="button"
            className={`method-btn ${uploadMethod === 'file' ? 'active' : ''}`}
            onClick={() => setUploadMethod('file')}
            disabled={loading}
          >
            📤 Drag & Drop File
          </button>
          <span className="method-or">OR</span>
          <button
            type="button"
            className={`method-btn ${uploadMethod === 'paste' ? 'active' : ''}`}
            onClick={() => setUploadMethod('paste')}
            disabled={loading}
          >
            📝 Paste Text
          </button>
        </div>

        {/* Error Message */}
        {error && (
          <div className="error-message">
            {error}
          </div>
        )}

        {/* Success Message */}
        {success && (
          <div className="success-message">
            {success.split('\n').map((line, i) => (
              <span key={i}>
                {line.startsWith('View file:') ? (
                  <>View file: <a href={line.replace('View file: ', '')} target="_blank" rel="noopener noreferrer" className="github-link">{line.replace('View file: ', '')}</a></>
                ) : (
                  line
                )}
                {i < success.split('\n').length - 1 && <br />}
              </span>
            ))}
          </div>
        )}

        {/* Progress Bar */}
        {showProgress && (
          <div className="progress-section">
            <div className="progress-header">
              <span className="progress-stage">{stage}</span>
              <span className="progress-percent">{progress}%</span>
            </div>
            <div className="progress-bar-track">
              <div
                className="progress-bar-fill"
                style={{ width: `${progress}%` }}
              />
            </div>
            <p className="progress-job-id">Job: {jobId}</p>
          </div>
        )}

        {/* File Upload Section */}
        {uploadMethod === 'file' && !showProgress && (
          <div
            ref={dropZoneRef}
            className={`drop-zone ${isDragging ? 'dragging' : ''}`}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
            onClick={() => fileInputRef.current?.click()}
          >
            <input
              ref={fileInputRef}
              type="file"
              accept=".pdf,.docx,.txt"
              onChange={handleFileSelect}
              className="file-input"
              style={{ display: 'none' }}
            />
            <div className="drop-zone-content">
              <div className="drop-icon">📄</div>
              <p className="drop-text">
                {fileName ? fileName : 'Drag file here or click to browse'}
              </p>
              <p className="drop-hint">Supports PDF, DOCX, TXT files</p>
            </div>
          </div>
        )}

        {/* Paste Text Section */}
        {uploadMethod === 'paste' && !showProgress && (
          <div className="paste-section">
            <label htmlFor="pastedText" className="paste-label">
              Paste your requirement text here:
            </label>
            <textarea
              id="pastedText"
              value={pastedText}
              onChange={(e) => setPastedText(e.target.value)}
              className="paste-textarea"
              placeholder="Paste your requirements here..."
              rows="10"
              disabled={loading}
            />
          </div>
        )}

        {/* Upload Button */}
        {!showProgress && (
          <button
            type="button"
            onClick={handleUpload}
            disabled={loading || !selectedRepo || (uploadMethod === 'file' && !fileName) || (uploadMethod === 'paste' && !pastedText.trim())}
            className="upload-button"
          >
            {loading ? 'Processing...' : 'Upload'}
          </button>
        )}
      </div>
    </div>
  );
};

export default RequirementUpload;
