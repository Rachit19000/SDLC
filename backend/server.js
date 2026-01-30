const express = require('express');
const cors = require('cors');
const multer = require('multer');
const { createFileInGitHub, getUserRepo } = require('./github-config');
const { parseDocument, isSupportedFileType } = require('./document-parser');
const app = express();
const PORT = 3001; // Different port from React (3000)

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Configure multer for file uploads (memory storage)
const upload = multer({
  storage: multer.memoryStorage(),
  limits: {
    fileSize: 10 * 1024 * 1024 // 10MB limit
  }
});

// Mock users database (in real app, this would be in a database)
const mockUsers = [
  {
    id: 'user_1',
    email: 'rachitjainemail@gmail.com',
    password: 'password123', // In real app, this would be hashed
    name: 'Rachit Jain'
  },
  {
    id: 'user_2',
    email: 'test@example.com',
    password: 'test123',
    name: 'Test User'
  },
  {
    id: 'user_3',
    email: 'admin@example.com',
    password: 'admin123',
    name: 'Admin User'
  }
];

// Mock Authentication - Login
app.post('/api/v1/auth/login', (req, res) => {
  const { email, password } = req.body;

  // Find user
  const user = mockUsers.find(u => u.email === email && u.password === password);

  if (!user) {
    return res.status(401).json({
      error: {
        code: 'UNAUTHORIZED',
        message: 'Invalid email or password'
      }
    });
  }

  // Generate mock token (in real app, use JWT)
  const token = `mock_token_${user.id}_${Date.now()}`;

  res.json({
    token: token,
    user: {
      id: user.id,
      email: user.email,
      name: user.name
    }
  });
});

// Mock Authentication - Register
app.post('/api/v1/auth/register', (req, res) => {
  const { email, password, name } = req.body;

  // Check if user exists
  if (mockUsers.find(u => u.email === email)) {
    return res.status(409).json({
      error: {
        code: 'CONFLICT',
        message: 'User already exists'
      }
    });
  }

  // Create new user
  const newUser = {
    id: `user_${mockUsers.length + 1}`,
    email,
    password, // In real app, hash this
    name
  };

  mockUsers.push(newUser);

  const token = `mock_token_${newUser.id}_${Date.now()}`;

  res.status(201).json({
    token: token,
    user: {
      id: newUser.id,
      email: newUser.email,
      name: newUser.name
    }
  });
});

// Requirement Upload - File (parse and store extracted text in GitHub)
app.post('/api/v1/requirements/upload', upload.single('file'), async (req, res) => {
  try {
    console.log('\n=== File Upload Request Received ===');
    
    // Get user from token (in real app, decode JWT token)
    const token = req.headers.authorization?.replace('Bearer ', '');
    if (!token) {
      console.log('ERROR: No token provided');
      return res.status(401).json({
        error: {
          code: 'UNAUTHORIZED',
          message: 'Authentication token required'
        }
      });
    }

    // Get user email from token (simplified - in real app, decode JWT)
    const user = mockUsers.find(u => token.includes(u.id));
    if (!user) {
      console.log('ERROR: Invalid token - user not found');
      return res.status(401).json({
        error: {
          code: 'UNAUTHORIZED',
          message: 'Invalid token'
        }
      });
    }

    if (!req.file) {
      console.log('ERROR: No file uploaded');
      return res.status(400).json({
        error: {
          code: 'BAD_REQUEST',
          message: 'No file uploaded'
        }
      });
    }

    const fileName = req.file.originalname;
    const mimeType = req.file.mimetype;
    const fileSize = req.file.size;
    
    console.log('File details:');
    console.log('  Name:', fileName);
    console.log('  Type:', mimeType);
    console.log('  Size:', fileSize, 'bytes');
    console.log('  User:', user.email);

    // Check if file type is supported
    if (!isSupportedFileType(mimeType, fileName)) {
      console.log('ERROR: Unsupported file type');
      return res.status(400).json({
        error: {
          code: 'BAD_REQUEST',
          message: `Unsupported file type: ${mimeType}. Supported: PDF, DOCX, TXT`
        }
      });
    }

    // Step 1: Parse document to extract text
    console.log('\n📄 Step 1: Parsing document...');
    const parseResult = await parseDocument(req.file.buffer, mimeType, fileName);
    const extractedText = parseResult.text;
    const metadata = parseResult.metadata;
    
    console.log('✅ Document parsed successfully');
    console.log('  Extracted text length:', extractedText.length, 'characters');
    if (metadata.pages) {
      console.log('  Pages:', metadata.pages);
    }

    // Step 2: Store extracted text in GitHub (not the binary file)
    const jobId = `job_${Date.now()}`;
    const textFileName = `${fileName.replace(/\.[^/.]+$/, '')}_extracted.txt`;
    const filePath = `requirements/${user.id}/${jobId}/${textFileName}`;
    
    console.log('\n📤 Step 2: Uploading extracted text to GitHub...');
    console.log('  File path:', filePath);
    
    // Create commit message with metadata
    let commitMessage = `Add parsed requirement: ${fileName} (Job: ${jobId})`;
    if (metadata.pages) {
      commitMessage += ` - ${metadata.pages} pages`;
    }
    commitMessage += ` - ${extractedText.length} characters extracted`;

    const githubResult = await createFileInGitHub(
      user.email,
      filePath,
      extractedText, // Store extracted text, not binary file
      commitMessage
    );

    console.log('✅ SUCCESS! Extracted text uploaded to GitHub');
    console.log('  GitHub URL:', githubResult.fileUrl);
    console.log('===================================\n');

    res.status(202).json({
      jobId: jobId,
      status: 'stored',
      message: 'File parsed and text uploaded successfully to GitHub',
      githubUrl: githubResult.fileUrl,
      commitUrl: githubResult.commitUrl,
      filePath: filePath,
      fileName: textFileName,
      originalFileName: fileName,
      extractedTextLength: extractedText.length,
      metadata: metadata
    });
  } catch (error) {
    console.error('\n❌ ERROR in file upload:', error.message);
    console.error('Error stack:', error.stack);
    console.error('===================================\n');
    
    res.status(500).json({
      error: {
        code: 'INTERNAL_ERROR',
        message: error.message || 'Failed to process file',
        details: error.stack
      }
    });
  }
});

// Requirement Upload - Paste Text (store in GitHub)
app.post('/api/v1/requirements/paste', async (req, res) => {
  try {
    console.log('\n=== Upload Request Received ===');
    
    // Get user from token
    const token = req.headers.authorization?.replace('Bearer ', '');
    console.log('Token received:', token ? 'Yes' : 'No');
    
    if (!token) {
      console.log('ERROR: No token provided');
      return res.status(401).json({
        error: {
          code: 'UNAUTHORIZED',
          message: 'Authentication token required'
        }
      });
    }

    // Get user email from token (simplified - in real app, decode JWT)
    const user = mockUsers.find(u => token.includes(u.id));
    console.log('User found:', user ? user.email : 'No user found');
    console.log('Token contains user ID:', token);
    
    if (!user) {
      console.log('ERROR: Invalid token - user not found');
      return res.status(401).json({
        error: {
          code: 'UNAUTHORIZED',
          message: 'Invalid token'
        }
      });
    }

    const { text, name } = req.body;
    console.log('Text length:', text ? text.length : 0);
    console.log('Name:', name || 'Not provided');

    if (!text || !text.trim()) {
      console.log('ERROR: No text provided');
      return res.status(400).json({
        error: {
          code: 'BAD_REQUEST',
          message: 'Text is required'
        }
      });
    }

    const jobId = `job_${Date.now()}`;
    const fileName = name ? `${name.replace(/[^a-z0-9]/gi, '_')}.txt` : `requirement_${jobId}.txt`;
    
    // Create file path in GitHub: requirements/{userId}/{jobId}/requirement.txt
    const filePath = `requirements/${user.id}/${jobId}/${fileName}`;
    console.log('File path:', filePath);
    console.log('User email:', user.email);
    
    // Upload to GitHub
    const commitMessage = `Add requirement text: ${fileName} (Job: ${jobId})`;
    console.log('Attempting to upload to GitHub...');
    
    const githubResult = await createFileInGitHub(
      user.email,
      filePath,
      text,
      commitMessage
    );

    console.log('✅ SUCCESS! File uploaded to GitHub');
    console.log('GitHub URL:', githubResult.fileUrl);
    console.log('===================================\n');

    res.status(202).json({
      jobId: jobId,
      status: 'stored',
      message: 'Text uploaded successfully to GitHub',
      githubUrl: githubResult.fileUrl,
      commitUrl: githubResult.commitUrl,
      filePath: filePath,
      fileName: fileName
    });
  } catch (error) {
    console.error('\n❌ ERROR in upload:', error.message);
    console.error('Error stack:', error.stack);
    console.error('===================================\n');
    
    res.status(500).json({
      error: {
        code: 'INTERNAL_ERROR',
        message: 'Failed to upload text to GitHub',
        details: error.message
      }
    });
  }
});

// Health check
app.get('/api/v1/health', (req, res) => {
  res.json({ status: 'ok', message: 'Backend API is running' });
});

app.listen(PORT, () => {
  console.log(`🚀 Mock Backend Server running on http://localhost:${PORT}`);
  console.log(`📝 Test login with: rachitjainemail@gmail.com / password123`);
});
