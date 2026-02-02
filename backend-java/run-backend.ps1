# PowerShell script to run Java Spring Boot backend
# This script sets up environment variables and runs the backend

Write-Host "Setting up environment variables..." -ForegroundColor Cyan

# Set Java Home
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25"
if (-not (Test-Path $env:JAVA_HOME)) {
    Write-Host "ERROR: Java not found at $env:JAVA_HOME" -ForegroundColor Red
    exit 1
}

# Set Maven Home
$env:M2_HOME = "C:\Program Files\apache-maven-3.9.12-bin\apache-maven-3.9.12"
if (-not (Test-Path $env:M2_HOME)) {
    Write-Host "ERROR: Maven not found at $env:M2_HOME" -ForegroundColor Red
    exit 1
}

# Add Maven to PATH
$env:PATH = "$env:M2_HOME\bin;$env:PATH"

Write-Host "Java Home: $env:JAVA_HOME" -ForegroundColor Green
Write-Host "Maven Home: $env:M2_HOME" -ForegroundColor Green
Write-Host ""

# Verify Maven
Write-Host "Verifying Maven installation..." -ForegroundColor Cyan
mvn -version
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Maven verification failed" -ForegroundColor Red
    exit 1
}

# Load GitHub Token from .env file if it exists
$envFile = "..\backend\.env"
if (Test-Path $envFile) {
    $envContent = Get-Content $envFile
    foreach ($line in $envContent) {
        if ($line -match "^GITHUB_TOKEN=(.+)$") {
            $env:GITHUB_TOKEN = $matches[1].Trim()
            Write-Host "GitHub token loaded from .env file" -ForegroundColor Green
            break
        }
    }
}

# Check if GitHub token is set
if (-not $env:GITHUB_TOKEN) {
    Write-Host "WARNING: GITHUB_TOKEN not set!" -ForegroundColor Yellow
    Write-Host "Please set it as an environment variable or in backend/.env file" -ForegroundColor Yellow
    Write-Host "Example: `$env:GITHUB_TOKEN = 'your_token_here'" -ForegroundColor Yellow
    Write-Host ""
}

Write-Host ""
Write-Host "Starting Spring Boot backend..." -ForegroundColor Cyan
Write-Host "Backend will run on: http://localhost:3001" -ForegroundColor Yellow
Write-Host "Press Ctrl+C to stop the server" -ForegroundColor Yellow
Write-Host ""

# Run Spring Boot
mvn spring-boot:run
