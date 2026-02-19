import React, { useState, useRef, useCallback, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import './RequirementUpload.css';

const BACKEND_URL = 'http://localhost:3001/api/v1';

/**
 * Multi-step wizard with human-in-the-loop validation:
 *
 * Step 1 (UPLOAD):             Upload file or paste text
 * Step 2 (REVIEW_PARSED):      Review/edit parsed text → Save to GitHub, then Next
 * Step 3 (REVIEW_REQUIREMENTS): Review/edit structured requirements (FR, NFR, AC) → Save to GitHub
 * Step 4 (REVIEW_STORIES):     Review/edit generated user stories → Save to GitHub
 * Step 5 (REVIEW_TECH_SPECS):  Review/edit generated tech specs → Save to GitHub
 */
const STEPS = {
  UPLOAD: 'upload',
  REVIEW_PARSED: 'review_parsed',
  REVIEW_REQUIREMENTS: 'review_requirements',
  REVIEW_STORIES: 'review_stories',
  REVIEW_TECH_SPECS: 'review_tech_specs',
};

const RequirementUpload = () => {
  const navigate = useNavigate();
  const location = useLocation();

  // Wizard step
  const [currentStep, setCurrentStep] = useState(STEPS.UPLOAD);

  // Repo selection state
  const [repos, setRepos] = useState([]);
  const [loadingRepos, setLoadingRepos] = useState(true);
  const [selectedRepo, setSelectedRepo] = useState(null);

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

  // Review state
  const [parsedText, setParsedText] = useState('');
  const [requirementsText, setRequirementsText] = useState('');
  const [requirementsJson, setRequirementsJson] = useState('');
  const [userStories, setUserStories] = useState('');
  const [techSpecs, setTechSpecs] = useState('');
  const [parsedSaved, setParsedSaved] = useState(false);
  const [requirementsSaved, setRequirementsSaved] = useState(false);
  const [storiesSaved, setStoriesSaved] = useState(false);
  const [techSpecsSaved, setTechSpecsSaved] = useState(false);
  const [parsedSaveUrl, setParsedSaveUrl] = useState('');
  const [requirementsSaveUrl, setRequirementsSaveUrl] = useState('');
  const [storiesSaveUrl, setStoriesSaveUrl] = useState('');
  const [techSpecsSaveUrl, setTechSpecsSaveUrl] = useState('');

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

  // ==========================================
  //  Step 1: Parse file or accept pasted text
  // ==========================================
  const handleUpload = async () => {
    if (!selectedRepo) {
      setError('Please select a repository first');
      return;
    }

    setError('');
    setSuccess('');
    setLoading(true);

    try {
      const token = localStorage.getItem('auth_token');
      if (!token) {
        navigate('/', { replace: true });
        return;
      }

      if (uploadMethod === 'file') {
        // Parse the file via backend
        const file = fileInputRef.current?.files[0];
        if (!file) {
          throw new Error('Please select a file');
        }

        const formData = new FormData();
        formData.append('file', file);

        const response = await fetch(`${BACKEND_URL}/requirements/parse`, {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${token}`,
          },
          body: formData,
        });

        if (response.status === 401) {
          handleAuthError(response);
          return;
        }

        if (!response.ok) {
          let errorMsg = `Server returned ${response.status} ${response.statusText}`;
          try {
            const data = await response.json();
            errorMsg = data.error?.message || data.message || data.error || errorMsg;
          } catch (jsonErr) {
            const text = await response.text();
            errorMsg = text || errorMsg;
          }
          throw new Error(errorMsg);
        }

        let data;
        try {
          data = await response.json();
        } catch (jsonErr) {
          const text = await response.text();
          throw new Error(`Invalid response from server: ${text.substring(0, 200)}`);
        }

        if (!data.parsedText) {
          throw new Error('Server response missing parsedText field');
        }

        setParsedText(data.parsedText);
        setCurrentStep(STEPS.REVIEW_PARSED);
        setParsedSaved(false);
        setParsedSaveUrl('');

      } else {
        // For pasted text, just move to review step
        if (!pastedText.trim()) {
          throw new Error('Please paste your requirement text');
        }
        setParsedText(pastedText.trim());
        setCurrentStep(STEPS.REVIEW_PARSED);
        setParsedSaved(false);
        setParsedSaveUrl('');
      }

    } catch (err) {
      if (err.name === 'TypeError' && err.message.includes('fetch')) {
        setError('Cannot connect to server. Please ensure the backend is running on http://localhost:3001');
      } else {
        setError(err.message || 'An unexpected error occurred');
      }
      console.error('Error in handleUpload:', err);
    } finally {
      setLoading(false);
    }
  };

  // ==========================================
  //  Step 2a: Save parsed text to GitHub
  // ==========================================
  const handleSaveParsed = async () => {
    if (!parsedText.trim()) {
      setError('Parsed text is empty');
      return;
    }

    setError('');
    setSuccess('');
    setLoading(true);

    try {
      const token = localStorage.getItem('auth_token');
      if (!token) {
        navigate('/', { replace: true });
        return;
      }

      const response = await fetch(`${BACKEND_URL}/requirements/save-to-github`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify({
          text: parsedText,
          fileName: fileName || 'Pasted_Requirements',
          repoOwner: selectedRepo.owner,
          repoName: selectedRepo.name,
          fileType: 'parsed',
        }),
      });

      if (response.status === 401) {
        handleAuthError(response);
        return;
      }

      if (!response.ok) {
        const data = await response.json().catch(() => ({}));
        throw new Error(data.error?.message || 'Failed to save to GitHub');
      }

      const data = await response.json();
      setParsedSaved(true);
      setParsedSaveUrl(data.fileUrl || '');
      setSuccess(`Parsed requirements saved to GitHub!${data.fileUrl ? '\nView file: ' + data.fileUrl : ''}`);

    } catch (err) {
      setError(err.message || 'Failed to save to GitHub');
    } finally {
      setLoading(false);
    }
  };

  // ==========================================
  //  Step 2b: Generate requirements (Next)
  // ==========================================
  const handleGenerateRequirements = async () => {
    if (!parsedText.trim()) {
      setError('Parsed text is empty');
      return;
    }

    setError('');
    setSuccess('');
    setLoading(true);

    try {
      const token = localStorage.getItem('auth_token');
      if (!token) {
        navigate('/', { replace: true });
        return;
      }

      const response = await fetch(`${BACKEND_URL}/requirements/generate-requirements`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify({
          text: parsedText,
        }),
      });

      if (response.status === 401) {
        handleAuthError(response);
        return;
      }

      if (!response.ok) {
        let errorMsg = `Server returned ${response.status} ${response.statusText}`;
        try {
          const data = await response.json();
          errorMsg = data.error?.message || data.message || data.error || errorMsg;
        } catch (jsonErr) {
          const text = await response.text();
          errorMsg = text || errorMsg;
        }
        throw new Error(errorMsg);
      }

      let data;
      try {
        data = await response.json();
      } catch (jsonErr) {
        const text = await response.text();
        throw new Error(`Invalid response from server: ${text.substring(0, 200)}`);
      }

      if (!data.requirementsText && !data.requirements) {
        throw new Error('Server response missing requirements data. Response: ' + JSON.stringify(data).substring(0, 200));
      }

      setRequirementsText(data.requirementsText || JSON.stringify(data.requirements, null, 2));
      setRequirementsJson(data.requirementsJson || JSON.stringify(data.requirements));
      setCurrentStep(STEPS.REVIEW_REQUIREMENTS);
      setRequirementsSaved(false);
      setRequirementsSaveUrl('');
      setSuccess('');

    } catch (err) {
      if (err.name === 'TypeError' && err.message.includes('fetch')) {
        setError('Cannot connect to server. Please ensure the backend is running on http://localhost:3001');
      } else {
        setError(err.message || 'Failed to generate requirements');
      }
      console.error('Error generating requirements:', err);
    } finally {
      setLoading(false);
    }
  };

  // ==========================================
  //  Step 3a: Save requirements to GitHub
  // ==========================================
  const handleSaveRequirements = async () => {
    if (!requirementsText.trim()) {
      setError('Requirements text is empty');
      return;
    }

    setError('');
    setSuccess('');
    setLoading(true);

    try {
      const token = localStorage.getItem('auth_token');
      if (!token) {
        navigate('/', { replace: true });
        return;
      }

      const response = await fetch(`${BACKEND_URL}/requirements/save-to-github`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify({
          text: requirementsText,
          fileName: fileName || 'Pasted_Requirements',
          repoOwner: selectedRepo.owner,
          repoName: selectedRepo.name,
          fileType: 'requirements',
        }),
      });

      if (response.status === 401) {
        handleAuthError(response);
        return;
      }

      if (!response.ok) {
        const data = await response.json().catch(() => ({}));
        throw new Error(data.error?.message || 'Failed to save requirements to GitHub');
      }

      const data = await response.json();
      setRequirementsSaved(true);
      setRequirementsSaveUrl(data.fileUrl || '');
      setSuccess(`Requirements saved to GitHub!${data.fileUrl ? '\nView file: ' + data.fileUrl : ''}`);

    } catch (err) {
      setError(err.message || 'Failed to save to GitHub');
    } finally {
      setLoading(false);
    }
  };

  // ==========================================
  //  Step 3b: Generate user stories (Next from Requirements)
  // ==========================================
  const handleGenerateStories = async () => {
    if (!requirementsJson && !parsedText.trim()) {
      setError('No requirements data available');
      return;
    }

    setError('');
    setSuccess('');
    setLoading(true);

    try {
      const token = localStorage.getItem('auth_token');
      if (!token) {
        navigate('/', { replace: true });
        return;
      }

      const body = {};
      if (requirementsJson) {
        body.requirementsJson = requirementsJson;
      } else {
        body.text = parsedText;
      }

      const response = await fetch(`${BACKEND_URL}/requirements/generate-stories`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify(body),
      });

      if (response.status === 401) {
        handleAuthError(response);
        return;
      }

      if (!response.ok) {
        let errorMsg = `Server returned ${response.status} ${response.statusText}`;
        try {
          const data = await response.json();
          errorMsg = data.error?.message || data.message || data.error || errorMsg;
        } catch (jsonErr) {
          const text = await response.text();
          errorMsg = text || errorMsg;
        }
        throw new Error(errorMsg);
      }

      let data;
      try {
        data = await response.json();
      } catch (jsonErr) {
        const text = await response.text();
        throw new Error(`Invalid response from server: ${text.substring(0, 200)}`);
      }

      if (!data.userStories) {
        throw new Error('Server response missing userStories field. Response: ' + JSON.stringify(data).substring(0, 200));
      }

      setUserStories(data.userStories);
      setCurrentStep(STEPS.REVIEW_STORIES);
      setStoriesSaved(false);
      setStoriesSaveUrl('');
      setSuccess('');

    } catch (err) {
      if (err.name === 'TypeError' && err.message.includes('fetch')) {
        setError('Cannot connect to server. Please ensure the backend is running on http://localhost:3001');
      } else {
        setError(err.message || 'Failed to generate user stories');
      }
      console.error('Error generating user stories:', err);
    } finally {
      setLoading(false);
    }
  };

  // ==========================================
  //  Step 4a: Save user stories to GitHub
  // ==========================================
  const handleSaveStories = async () => {
    if (!userStories.trim()) {
      setError('User stories are empty');
      return;
    }

    setError('');
    setSuccess('');
    setLoading(true);

    try {
      const token = localStorage.getItem('auth_token');
      if (!token) {
        navigate('/', { replace: true });
        return;
      }

      const response = await fetch(`${BACKEND_URL}/requirements/save-to-github`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify({
          text: userStories,
          fileName: fileName || 'Pasted_Requirements',
          repoOwner: selectedRepo.owner,
          repoName: selectedRepo.name,
          fileType: 'user_stories',
        }),
      });

      if (response.status === 401) {
        handleAuthError(response);
        return;
      }

      if (!response.ok) {
        const data = await response.json().catch(() => ({}));
        throw new Error(data.error?.message || 'Failed to save user stories to GitHub');
      }

      const data = await response.json();
      setStoriesSaved(true);
      setStoriesSaveUrl(data.fileUrl || '');
      setSuccess(`User stories saved to GitHub!${data.fileUrl ? '\nView file: ' + data.fileUrl : ''}`);

    } catch (err) {
      setError(err.message || 'Failed to save to GitHub');
    } finally {
      setLoading(false);
    }
  };

  // ==========================================
  //  Step 4b: Generate tech specs (Next from Stories)
  // ==========================================
  const handleGenerateTechSpecs = async () => {
    if (!parsedText.trim()) {
      setError('Parsed text is empty');
      return;
    }

    setError('');
    setSuccess('');
    setLoading(true);

    try {
      const token = localStorage.getItem('auth_token');
      if (!token) {
        navigate('/', { replace: true });
        return;
      }

      const response = await fetch(`${BACKEND_URL}/requirements/generate-tech-specs`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify({
          text: parsedText,
          userStories: userStories || '',
        }),
      });

      if (response.status === 401) {
        handleAuthError(response);
        return;
      }

      if (!response.ok) {
        const data = await response.json().catch(() => ({}));
        throw new Error(data.error?.message || 'Failed to generate tech specification');
      }

      const data = await response.json();
      setTechSpecs(data.techSpec);
      setCurrentStep(STEPS.REVIEW_TECH_SPECS);
      setTechSpecsSaved(false);
      setTechSpecsSaveUrl('');
      setSuccess('');

    } catch (err) {
      setError(err.message || 'Failed to generate tech specification');
    } finally {
      setLoading(false);
    }
  };

  // ==========================================
  //  Step 5: Save tech specs to GitHub
  // ==========================================
  const handleSaveTechSpecs = async () => {
    if (!techSpecs.trim()) {
      setError('Tech specification is empty');
      return;
    }

    setError('');
    setSuccess('');
    setLoading(true);

    try {
      const token = localStorage.getItem('auth_token');
      if (!token) {
        navigate('/', { replace: true });
        return;
      }

      const response = await fetch(`${BACKEND_URL}/requirements/save-to-github`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify({
          text: techSpecs,
          fileName: fileName || 'Pasted_Requirements',
          repoOwner: selectedRepo.owner,
          repoName: selectedRepo.name,
          fileType: 'tech_spec',
        }),
      });

      if (response.status === 401) {
        handleAuthError(response);
        return;
      }

      if (!response.ok) {
        const data = await response.json().catch(() => ({}));
        throw new Error(data.error?.message || 'Failed to save tech spec to GitHub');
      }

      const data = await response.json();
      setTechSpecsSaved(true);
      setTechSpecsSaveUrl(data.fileUrl || '');
      setSuccess(`Tech specification saved to GitHub!${data.fileUrl ? '\nView file: ' + data.fileUrl : ''}`);

    } catch (err) {
      setError(err.message || 'Failed to save to GitHub');
    } finally {
      setLoading(false);
    }
  };

  // ==========================================
  //  Helpers
  // ==========================================
  const handleAuthError = async (response) => {
    const data = await response.json().catch(() => ({}));
    const errMsg = data.error?.message || 'Session expired. Please sign in again.';
    setError(errMsg);
    setLoading(false);
    setTimeout(() => {
      localStorage.removeItem('auth_token');
      localStorage.removeItem('user');
      navigate('/', { replace: true });
    }, 3000);
  };

  const handleBackToDashboard = () => {
    navigate('/dashboard');
  };

  const handleStartOver = () => {
    setCurrentStep(STEPS.UPLOAD);
    setParsedText('');
    setRequirementsText('');
    setRequirementsJson('');
    setUserStories('');
    setTechSpecs('');
    setPastedText('');
    setFileName('');
    setError('');
    setSuccess('');
    setParsedSaved(false);
    setRequirementsSaved(false);
    setStoriesSaved(false);
    setTechSpecsSaved(false);
    setParsedSaveUrl('');
    setRequirementsSaveUrl('');
    setStoriesSaveUrl('');
    setTechSpecsSaveUrl('');
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  // Step indicator helper
  const getStepNumber = () => {
    switch (currentStep) {
      case STEPS.UPLOAD: return 1;
      case STEPS.REVIEW_PARSED: return 2;
      case STEPS.REVIEW_REQUIREMENTS: return 3;
      case STEPS.REVIEW_STORIES: return 4;
      case STEPS.REVIEW_TECH_SPECS: return 5;
      default: return 1;
    }
  };

  // ==========================================
  //  Loading / Empty states
  // ==========================================
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

  // ==========================================
  //  Render
  // ==========================================
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

        {/* Step Indicator */}
        <div className="step-indicator">
          <div className={`step-dot ${getStepNumber() >= 1 ? 'active' : ''} ${getStepNumber() > 1 ? 'completed' : ''}`}>
            <span className="step-number">{getStepNumber() > 1 ? '✓' : '1'}</span>
            <span className="step-label">Upload</span>
          </div>
          <div className="step-line"></div>
          <div className={`step-dot ${getStepNumber() >= 2 ? 'active' : ''} ${getStepNumber() > 2 ? 'completed' : ''}`}>
            <span className="step-number">{getStepNumber() > 2 ? '✓' : '2'}</span>
            <span className="step-label">Review Text</span>
          </div>
          <div className="step-line"></div>
          <div className={`step-dot ${getStepNumber() >= 3 ? 'active' : ''} ${getStepNumber() > 3 ? 'completed' : ''}`}>
            <span className="step-number">{getStepNumber() > 3 ? '✓' : '3'}</span>
            <span className="step-label">Requirements</span>
          </div>
          <div className="step-line"></div>
          <div className={`step-dot ${getStepNumber() >= 4 ? 'active' : ''} ${getStepNumber() > 4 ? 'completed' : ''}`}>
            <span className="step-number">{getStepNumber() > 4 ? '✓' : '4'}</span>
            <span className="step-label">User Stories</span>
          </div>
          <div className="step-line"></div>
          <div className={`step-dot ${getStepNumber() >= 5 ? 'active' : ''}`}>
            <span className="step-number">5</span>
            <span className="step-label">Tech Specs</span>
          </div>
        </div>

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
            disabled={loading || currentStep !== STEPS.UPLOAD}
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

        {/* ========================== STEP 1: UPLOAD ========================== */}
        {currentStep === STEPS.UPLOAD && (
          <>
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

            {/* File Upload Section */}
            {uploadMethod === 'file' && (
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
            {uploadMethod === 'paste' && (
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
            <button
              type="button"
              onClick={handleUpload}
              disabled={loading || !selectedRepo || (uploadMethod === 'file' && !fileName) || (uploadMethod === 'paste' && !pastedText.trim())}
              className="upload-button"
            >
              {loading ? (
                <span className="btn-loading">
                  <span className="spinner"></span> Parsing...
                </span>
              ) : (
                'Upload & Parse'
              )}
            </button>
          </>
        )}

        {/* ========================== STEP 2: REVIEW PARSED TEXT ========================== */}
        {currentStep === STEPS.REVIEW_PARSED && (
          <>
            <div className="review-section">
              <div className="review-header">
                <label className="review-label">
                  📝 Review & Edit Parsed Requirements
                </label>
                <span className="review-char-count">
                  {parsedText.length} characters
                </span>
              </div>
              <textarea
                value={parsedText}
                onChange={(e) => {
                  setParsedText(e.target.value);
                  setParsedSaved(false);
                }}
                className="review-textarea"
                rows="14"
                disabled={loading}
                placeholder="Parsed requirements text..."
              />
            </div>

            <div className="action-buttons">
              <button
                type="button"
                onClick={handleSaveParsed}
                disabled={loading || !parsedText.trim()}
                className={`save-btn ${parsedSaved ? 'saved' : ''}`}
              >
                {loading && !parsedSaved ? (
                  <span className="btn-loading">
                    <span className="spinner"></span> Saving...
                  </span>
                ) : parsedSaved ? (
                  '✓ Saved to GitHub'
                ) : (
                  '💾 Save to GitHub'
                )}
              </button>
              <button
                type="button"
                onClick={handleGenerateRequirements}
                disabled={loading || !parsedText.trim()}
                className="next-btn"
              >
                {loading ? (
                  <span className="btn-loading">
                    <span className="spinner"></span> Generating Requirements...
                  </span>
                ) : (
                  'Next → Generate Requirements'
                )}
              </button>
            </div>

            <button
              type="button"
              onClick={handleStartOver}
              className="start-over-btn"
              disabled={loading}
            >
              ← Start Over
            </button>
          </>
        )}

        {/* ========================== STEP 3: REVIEW REQUIREMENTS ========================== */}
        {currentStep === STEPS.REVIEW_REQUIREMENTS && (
          <>
            <div className="review-section">
              <div className="review-header">
                <label className="review-label">
                  📋 Review & Edit Requirements (FR, NFR, AC)
                </label>
                <span className="review-char-count">
                  {requirementsText.length} characters
                </span>
              </div>
              <textarea
                value={requirementsText}
                onChange={(e) => {
                  setRequirementsText(e.target.value);
                  setRequirementsSaved(false);
                }}
                className="review-textarea"
                rows="14"
                disabled={loading}
                placeholder="Generated requirements..."
              />
            </div>

            <div className="action-buttons">
              <button
                type="button"
                onClick={handleSaveRequirements}
                disabled={loading || !requirementsText.trim()}
                className={`save-btn ${requirementsSaved ? 'saved' : ''}`}
              >
                {loading && !requirementsSaved ? (
                  <span className="btn-loading">
                    <span className="spinner"></span> Saving...
                  </span>
                ) : requirementsSaved ? (
                  '✓ Saved to GitHub'
                ) : (
                  '💾 Save Requirements to GitHub'
                )}
              </button>
              <button
                type="button"
                onClick={handleGenerateStories}
                disabled={loading || (!requirementsJson && !parsedText.trim())}
                className="next-btn"
              >
                {loading ? (
                  <span className="btn-loading">
                    <span className="spinner"></span> Generating User Stories...
                  </span>
                ) : (
                  'Next → Generate User Stories'
                )}
              </button>
            </div>

            <button
              type="button"
              onClick={() => {
                setCurrentStep(STEPS.REVIEW_PARSED);
                setError('');
                setSuccess('');
              }}
              className="start-over-btn"
              disabled={loading}
            >
              ← Back to Parsed Text
            </button>
          </>
        )}

        {/* ========================== STEP 4: REVIEW USER STORIES ========================== */}
        {currentStep === STEPS.REVIEW_STORIES && (
          <>
            <div className="review-section">
              <div className="review-header">
                <label className="review-label">
                  📋 Review & Edit User Stories
                </label>
                <span className="review-char-count">
                  {userStories.length} characters
                </span>
              </div>
              <textarea
                value={userStories}
                onChange={(e) => {
                  setUserStories(e.target.value);
                  setStoriesSaved(false);
                }}
                className="review-textarea"
                rows="14"
                disabled={loading}
                placeholder="Generated user stories..."
              />
            </div>

            <div className="action-buttons">
              <button
                type="button"
                onClick={handleSaveStories}
                disabled={loading || !userStories.trim()}
                className={`save-btn ${storiesSaved ? 'saved' : ''}`}
              >
                {loading && !storiesSaved ? (
                  <span className="btn-loading">
                    <span className="spinner"></span> Saving...
                  </span>
                ) : storiesSaved ? (
                  '✓ Saved to GitHub'
                ) : (
                  '💾 Save User Stories to GitHub'
                )}
              </button>
              <button
                type="button"
                onClick={handleGenerateTechSpecs}
                disabled={loading || !parsedText.trim()}
                className="next-btn"
              >
                {loading ? (
                  <span className="btn-loading">
                    <span className="spinner"></span> Generating Tech Specs...
                  </span>
                ) : (
                  'Next → Generate Tech Specs'
                )}
              </button>
            </div>

            <button
              type="button"
              onClick={() => {
                setCurrentStep(STEPS.REVIEW_REQUIREMENTS);
                setError('');
                setSuccess('');
              }}
              className="start-over-btn"
              disabled={loading}
            >
              ← Back to Requirements
            </button>
          </>
        )}

        {/* ========================== STEP 5: REVIEW TECH SPECS ========================== */}
        {currentStep === STEPS.REVIEW_TECH_SPECS && (
          <>
            <div className="review-section">
              <div className="review-header">
                <label className="review-label">
                  🏗️ Review & Edit Technical Specification
                </label>
                <span className="review-char-count">
                  {techSpecs.length} characters
                </span>
              </div>
              <textarea
                value={techSpecs}
                onChange={(e) => {
                  setTechSpecs(e.target.value);
                  setTechSpecsSaved(false);
                }}
                className="review-textarea"
                rows="14"
                disabled={loading}
                placeholder="Generated technical specification..."
              />
            </div>

            <div className="action-buttons">
              <button
                type="button"
                onClick={handleSaveTechSpecs}
                disabled={loading || !techSpecs.trim()}
                className={`save-btn ${techSpecsSaved ? 'saved' : ''}`}
              >
                {loading && !techSpecsSaved ? (
                  <span className="btn-loading">
                    <span className="spinner"></span> Saving...
                  </span>
                ) : techSpecsSaved ? (
                  '✓ Saved to GitHub'
                ) : (
                  '💾 Save Tech Spec to GitHub'
                )}
              </button>
              <button
                type="button"
                onClick={handleStartOver}
                disabled={loading}
                className="next-btn"
              >
                🔄 Start New Requirement
              </button>
            </div>

            <button
              type="button"
              onClick={() => {
                setCurrentStep(STEPS.REVIEW_STORIES);
                setError('');
                setSuccess('');
              }}
              className="start-over-btn"
              disabled={loading}
            >
              ← Back to User Stories
            </button>
          </>
        )}
      </div>
    </div>
  );
};

export default RequirementUpload;
