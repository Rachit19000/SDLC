# Database Schema Documentation

## Overview
This directory contains the PostgreSQL database schema for the SDLC application.

## Tables

### 1. `settings` Table
Stores application configuration as key-value pairs.

**Columns:**
- `id` (SERIAL, PRIMARY KEY): Auto-incrementing unique identifier
- `setting_key` (VARCHAR(100), UNIQUE, NOT NULL): The setting name (e.g., 'polling_time', 'model_selected')
- `setting_value` (TEXT, NOT NULL): The setting value (e.g., '5000', 'gpt-4')
- `description` (VARCHAR(500)): Optional description of the setting
- `created_at` (TIMESTAMP): When the setting was created
- `updated_at` (TIMESTAMP): When the setting was last updated

**Default Settings:**
- `polling_time`: Default value is '5000' (5 seconds in milliseconds)
- `model_selected`: Default value is 'gpt-4'

### 2. `job_mapping` Table
Maps Job IDs to Project IDs and Agent IDs.

**Columns:**
- `id` (SERIAL, PRIMARY KEY): Auto-incrementing unique identifier
- `job_id` (VARCHAR(100), UNIQUE, NOT NULL): Unique job identifier
- `project_id` (VARCHAR(100), NOT NULL): Associated project identifier
- `agent_id` (VARCHAR(100), NOT NULL): Associated agent identifier
- `created_at` (TIMESTAMP): When the mapping was created
- `updated_at` (TIMESTAMP): When the mapping was last updated

**Indexes:**
- Primary index on `job_id` (unique)
- Index on `project_id` for fast project lookups
- Index on `agent_id` for fast agent lookups
- Composite index on `(project_id, agent_id)` for combined queries

## Setup Instructions

### 1. Create Database
```bash
# Connect to PostgreSQL as superuser
psql -U postgres

# Create database
CREATE DATABASE sdlc_db;

# Connect to the new database
\c sdlc_db;
```

### 2. Run Schema Script
```bash
# From the project root
psql -U postgres -d sdlc_db -f backend-java/database/schema.sql
```

Or execute the SQL file directly:
```bash
psql -U postgres -d sdlc_db < backend-java/database/schema.sql
```

### 3. Verify Tables
```sql
-- List all tables
\dt

-- Check settings table structure
\d settings

-- Check job_mapping table structure
\d job_mapping

-- View default settings
SELECT * FROM settings;
```

## Common Operations

### Settings Operations

**Get Polling Time:**
```sql
SELECT setting_value::INTEGER FROM settings WHERE setting_key = 'polling_time';
```

**Get Selected Model:**
```sql
SELECT setting_value FROM settings WHERE setting_key = 'model_selected';
```

**Update Polling Time:**
```sql
UPDATE settings 
SET setting_value = '10000' 
WHERE setting_key = 'polling_time';
```

**Update Model:**
```sql
UPDATE settings 
SET setting_value = 'claude-3' 
WHERE setting_key = 'model_selected';
```

**Add New Setting:**
```sql
INSERT INTO settings (setting_key, setting_value, description)
VALUES ('new_setting', 'value', 'Description of new setting');
```

### Job Mapping Operations

**Create Job Mapping:**
```sql
INSERT INTO job_mapping (job_id, project_id, agent_id)
VALUES ('job_123', 'project_456', 'agent_789');
```

**Get Job Mapping by Job ID:**
```sql
SELECT * FROM job_mapping WHERE job_id = 'job_123';
```

**Get All Jobs for a Project:**
```sql
SELECT * FROM job_mapping WHERE project_id = 'project_456';
```

**Get All Jobs for an Agent:**
```sql
SELECT * FROM job_mapping WHERE agent_id = 'agent_789';
```

**Update Job Mapping:**
```sql
UPDATE job_mapping 
SET project_id = 'new_project', agent_id = 'new_agent'
WHERE job_id = 'job_123';
```

**Delete Job Mapping:**
```sql
DELETE FROM job_mapping WHERE job_id = 'job_123';
```

## Database Connection Configuration

Add these properties to `application.properties`:

```properties
# PostgreSQL Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/sdlc_db
spring.datasource.username=postgres
spring.datasource.password=your_password
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate Configuration
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true
```

## Notes

- The `updated_at` column is automatically updated via triggers when a row is modified
- All timestamps use PostgreSQL's `CURRENT_TIMESTAMP` function
- The schema uses `SERIAL` for auto-incrementing IDs (PostgreSQL-specific)
- Indexes are created to optimize common query patterns
- The `settings` table uses `TEXT` for values to support various data types (can be cast as needed)
