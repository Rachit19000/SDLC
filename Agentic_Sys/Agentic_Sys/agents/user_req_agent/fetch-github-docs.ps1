# PowerShell script to fetch and open GitHub documents in Cursor
# Usage: .\fetch-github-docs.ps1 user-stories
#        .\fetch-github-docs.ps1 requirements
#        .\fetch-github-docs.ps1 tech-specs
#        .\fetch-github-docs.ps1 all
#        .\fetch-github-docs.ps1 list

param(
    [Parameter(Mandatory=$true)]
    [string]$Command
)

$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
$pythonScript = Join-Path $scriptPath "fetch_github_docs.py"

if (-not (Test-Path $pythonScript)) {
    Write-Host "Error: fetch_github_docs.py not found at $pythonScript" -ForegroundColor Red
    exit 1
}

# Run the Python script
python $pythonScript $Command

if ($LASTEXITCODE -ne 0) {
    Write-Host "`nError occurred. Make sure you have:" -ForegroundColor Yellow
    Write-Host "  1. Python installed and in PATH" -ForegroundColor Yellow
    Write-Host "  2. Required packages installed (pip install -r requirements.txt)" -ForegroundColor Yellow
    Write-Host "  3. .env file configured with GitHub credentials" -ForegroundColor Yellow
    exit $LASTEXITCODE
}
