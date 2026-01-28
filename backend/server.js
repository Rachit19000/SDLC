const express = require('express');
const cors = require('cors');
const app = express();
const PORT = 3001; // Different port from React (3000)

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

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

// Mock Requirement Upload - File
app.post('/api/v1/requirements/upload', (req, res) => {
  // In real app, you'd use multer for file uploads
  // For now, just return a mock response
  
  const jobId = `job_${Date.now()}`;
  
  res.status(202).json({
    jobId: jobId,
    status: 'processing',
    message: 'Document upload initiated. Use jobId to track progress via WebSocket.'
  });
});

// Mock Requirement Upload - Paste Text
app.post('/api/v1/requirements/paste', (req, res) => {
  const { text, name } = req.body;

  if (!text || !text.trim()) {
    return res.status(400).json({
      error: {
        code: 'BAD_REQUEST',
        message: 'Text is required'
      }
    });
  }

  const jobId = `job_${Date.now()}`;

  res.status(202).json({
    jobId: jobId,
    status: 'processing',
    message: 'Text upload initiated. Use jobId to track progress via WebSocket.'
  });
});

// Health check
app.get('/api/v1/health', (req, res) => {
  res.json({ status: 'ok', message: 'Backend API is running' });
});

app.listen(PORT, () => {
  console.log(`🚀 Mock Backend Server running on http://localhost:${PORT}`);
  console.log(`📝 Test login with: rachitjainemail@gmail.com / password123`);
});
