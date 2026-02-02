# Quick Maven Setup Script for Windows

Write-Host "Checking if Maven is installed..." -ForegroundColor Yellow

# Check if Maven is already installed
try {
    $mvnVersion = mvn -version 2>&1
    Write-Host "Maven is already installed!" -ForegroundColor Green
    Write-Host $mvnVersion
    exit 0
} catch {
    Write-Host "Maven not found. Installing..." -ForegroundColor Yellow
}

# Check if Chocolatey is available
try {
    $choco = choco --version
    Write-Host "Chocolatey found. Installing Maven via Chocolatey..." -ForegroundColor Green
    choco install maven -y
    Write-Host "Maven installed! Please restart PowerShell and run: mvn -version" -ForegroundColor Green
} catch {
    Write-Host "Chocolatey not found. Manual installation required." -ForegroundColor Red
    Write-Host ""
    Write-Host "Please follow these steps:" -ForegroundColor Yellow
    Write-Host "1. Download Maven from: https://maven.apache.org/download.cgi" -ForegroundColor Cyan
    Write-Host "2. Extract to: C:\Program Files\Apache\maven" -ForegroundColor Cyan
    Write-Host "3. Add to PATH: C:\Program Files\Apache\maven\bin" -ForegroundColor Cyan
    Write-Host "4. Restart PowerShell" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Or see: backend-java/INSTALL_MAVEN_WINDOWS.md" -ForegroundColor Cyan
}
