import React, { useState, useRef } from 'react';
import './RequirementUpload.css';

const RequirementUpload = () => {
  const [uploadMethod, setUploadMethod] = useState('file'); // 'file' or 'paste'
  const [pastedText, setPastedText] = useState('');
  const [fileName, setFileName] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const fileInputRef = useRef(null);
  const dropZoneRef = useRef(null);
  const [isDragging, setIsDragging] = useState(false);

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
        setFileName(file.name);
        setError('');
      } else {
        setError('Please upload a PDF, DOCX, or TXT file');
      }
    }
  };

  const handleUpload = async () => {
    setError('');
    setSuccess('');
    setLoading(true);

    try {
      const token = localStorage.getItem('auth_token');
      
      if (uploadMethod === 'file') {
        const file = fileInputRef.current?.files[0];
        if (!file) {
          throw new Error('Please select a file');
        }

        const formData = new FormData();
        formData.append('file', file);
        formData.append('name', file.name);

        const response = await fetch('http://localhost:3001/api/v1/requirements/upload', {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${token}`,
          },
          body: formData,
        });

        if (!response.ok) {
          const contentType = response.headers.get('content-type');
          if (!contentType || !contentType.includes('application/json')) {
            throw new Error('Backend API is not running. Please start the backend server.');
          }
          const data = await response.json();
          throw new Error(data.error?.message || 'Upload failed');
        }

        const data = await response.json();
        if (data.githubUrl) {
          let message = `File parsed and uploaded to GitHub!\n`;
          message += `Job ID: ${data.jobId}\n`;
          if (data.extractedTextLength) {
            message += `Extracted ${data.extractedTextLength} characters\n`;
          }
          message += `View file: ${data.githubUrl}`;
          setSuccess(message);
        } else {
          setSuccess(`File uploaded successfully! Job ID: ${data.jobId}`);
        }
        setFileName('');
        fileInputRef.current.value = '';
      } else {
        // Paste text method
        if (!pastedText.trim()) {
          throw new Error('Please paste your requirement text');
        }

        const response = await fetch('http://localhost:3001/api/v1/requirements/paste', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
          },
          body: JSON.stringify({
            text: pastedText,
            name: 'Pasted Requirements',
          }),
        });

        if (!response.ok) {
          const contentType = response.headers.get('content-type');
          if (!contentType || !contentType.includes('application/json')) {
            throw new Error('Backend API is not running. Please start the backend server.');
          }
          const data = await response.json();
          throw new Error(data.error?.message || 'Upload failed');
        }

        const data = await response.json();
        if (data.githubUrl) {
          setSuccess(`Text uploaded to GitHub! Job ID: ${data.jobId}\nView file: ${data.githubUrl}`);
        } else {
          setSuccess(`Text uploaded successfully! Job ID: ${data.jobId}`);
        }
        setPastedText('');
      }
    } catch (err) {
      setError(err.message || 'An error occurred during upload');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="upload-container">
      <div className="upload-box">
        <h1 className="upload-title">SDLC Automation Platform</h1>
        <p className="upload-subtitle">Upload Your Requirements</p>

        {/* Method Selection */}
        <div className="method-selection">
          <button
            type="button"
            className={`method-btn ${uploadMethod === 'file' ? 'active' : ''}`}
            onClick={() => setUploadMethod('file')}
          >
            📤 Drag & Drop File
          </button>
          <span className="method-or">OR</span>
          <button
            type="button"
            className={`method-btn ${uploadMethod === 'paste' ? 'active' : ''}`}
            onClick={() => setUploadMethod('paste')}
          >
            📝 Paste Text
          </button>
        </div>

        {/* Error/Success Messages */}
        {error && (
          <div className="error-message">
            {error}
          </div>
        )}

        {success && (
          <div className="success-message">
            {success}
          </div>
        )}

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
            />
          </div>
        )}

        {/* Upload Button */}
        <button
          type="button"
          onClick={handleUpload}
          disabled={loading || (uploadMethod === 'file' && !fileName) || (uploadMethod === 'paste' && !pastedText.trim())}
          className="upload-button"
        >
          {loading ? 'Uploading...' : 'Upload'}
        </button>
      </div>
    </div>
  );
};

export default RequirementUpload;
