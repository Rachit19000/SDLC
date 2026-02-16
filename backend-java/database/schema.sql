-- =====================================================
-- PostgreSQL Database Schema for SDLC Application
-- =====================================================

-- Create database (run this separately as superuser)
-- CREATE DATABASE sdlc_db;
-- \c sdlc_db;

-- =====================================================
-- 1. SETTINGS TABLE
-- Stores key-value pairs for application settings
-- =====================================================

CREATE TABLE IF NOT EXISTS settings (
    id SERIAL PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT NOT NULL,
    description VARCHAR(500)
);

-- Create index on setting_key for fast lookups
CREATE INDEX IF NOT EXISTS idx_settings_key ON settings(setting_key);

-- Insert default settings
INSERT INTO settings (setting_key, setting_value, description) VALUES
    ('polling_time', '5000', 'Polling interval in milliseconds (default: 5000ms = 5 seconds)'),
    ('model_selected', 'gpt-4', 'Selected AI model for document processing (e.g., gpt-4, gpt-3.5-turbo, claude-3)')
ON CONFLICT (setting_key) DO NOTHING;

-- =====================================================
-- 2. JOB_MAPPING TABLE
-- Maps JobID to ProjectID and AgentID
-- =====================================================

CREATE TABLE IF NOT EXISTS job_mapping (
    id SERIAL PRIMARY KEY,
    job_id VARCHAR(100) NOT NULL UNIQUE,
    project_id VARCHAR(100) NOT NULL,
    agent_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for common queries
CREATE INDEX IF NOT EXISTS idx_job_mapping_job_id ON job_mapping(job_id);
CREATE INDEX IF NOT EXISTS idx_job_mapping_project_id ON job_mapping(project_id);
CREATE INDEX IF NOT EXISTS idx_job_mapping_agent_id ON job_mapping(agent_id);
CREATE INDEX IF NOT EXISTS idx_job_mapping_project_agent ON job_mapping(project_id, agent_id);

-- =====================================================
-- 3. TRIGGER: Auto-update updated_at timestamp
-- =====================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for job_mapping table
CREATE TRIGGER update_job_mapping_updated_at
    BEFORE UPDATE ON job_mapping
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- 4. USEFUL QUERIES (Examples)
-- =====================================================

-- Get polling time setting
-- SELECT setting_value::INTEGER FROM settings WHERE setting_key = 'polling_time';

-- Get selected model
-- SELECT setting_value FROM settings WHERE setting_key = 'model_selected';

-- Update polling time
-- UPDATE settings SET setting_value = '10000' WHERE setting_key = 'polling_time';

-- Update model
-- UPDATE settings SET setting_value = 'claude-3' WHERE setting_key = 'model_selected';

-- Get job mapping by job_id
-- SELECT * FROM job_mapping WHERE job_id = 'job_123';

-- Get all jobs for a project
-- SELECT * FROM job_mapping WHERE project_id = 'project_456';

-- Get all jobs for an agent
-- SELECT * FROM job_mapping WHERE agent_id = 'agent_789';
