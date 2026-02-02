# Install Maven - Quick Steps

## Step 1: Download Maven
1. Open browser: https://maven.apache.org/download.cgi
2. Download: **apache-maven-3.9.6-bin.zip** (or latest)

## Step 2: Extract
1. Extract ZIP file
2. Move folder to: `C:\Program Files\Apache\maven`
3. You should have: `C:\Program Files\Apache\maven\bin\mvn.cmd`

## Step 3: Add to PATH
1. Press `Win + R` → Type `sysdm.cpl` → Enter
2. Click **"Advanced"** tab → Click **"Environment Variables"**
3. Under **"System variables"**, find **"Path"** → Click **"Edit"**
4. Click **"New"** → Paste: `C:\Program Files\Apache\maven\bin`
5. Click **OK** on all dialogs

## Step 4: Verify
1. **Close and reopen PowerShell** (important!)
2. Run:
   ```powershell
   mvn -version
   ```
3. Should show: `Apache Maven 3.9.x`

## Step 5: Run Project
```powershell
cd backend-java
mvn clean install
mvn spring-boot:run
```

---

## Alternative: Use Node.js Backend (No Maven Needed)

If you want to skip Maven installation:

```powershell
# Set GitHub token
$env:GITHUB_TOKEN="your_github_token_here"

# Start Node.js backend
cd backend
npm install
npm start

# In another terminal, start frontend
cd frontend
npm start
```

Both backends work the same! ✅
