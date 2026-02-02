# Install Maven on Windows - Quick Guide

## Method 1: Using Chocolatey (Fastest)

If you have Chocolatey installed:
```powershell
choco install maven
```

## Method 2: Manual Installation (5 minutes)

### Step 1: Download Maven
1. Go to: https://maven.apache.org/download.cgi
2. Download: `apache-maven-3.9.6-bin.zip` (or latest version)

### Step 2: Extract
1. Extract ZIP to: `C:\Program Files\Apache\maven`
2. You should have: `C:\Program Files\Apache\maven\bin\mvn.cmd`

### Step 3: Add to PATH
1. Press `Win + X` → System → Advanced system settings
2. Click "Environment Variables"
3. Under "System variables", find "Path" → Click "Edit"
4. Click "New" → Add: `C:\Program Files\Apache\maven\bin`
5. Click OK on all dialogs

### Step 4: Verify
Open **NEW** PowerShell window:
```powershell
mvn -version
```

Should show: `Apache Maven 3.9.x`

---

## Method 3: Use Maven Wrapper (No Installation Needed)

If you have Java installed, you can download the Maven wrapper JAR:

```powershell
cd backend-java
# Create .mvn/wrapper directory
mkdir .mvn\wrapper -Force

# Download Maven wrapper JAR (if you have curl)
curl -o .mvn\wrapper\maven-wrapper.jar https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar
```

Then use:
```powershell
.\mvnw.cmd clean install
```

---

## Quick Test

After installation, verify:
```powershell
mvn -version
```

Then run:
```powershell
cd backend-java
mvn clean install
```
