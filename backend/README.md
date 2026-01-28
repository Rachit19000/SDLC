# Mock Backend Server

Simple mock backend server for testing the SDLC Automation Platform frontend.

## Installation

```bash
npm install
```

## Run Server

```bash
npm start
```

Server runs on: `http://localhost:3001`

## Test Credentials

- **Email**: rachitjainemail@gmail.com
- **Password**: password123

## API Endpoints

- `POST /api/v1/auth/login` - Login
- `POST /api/v1/auth/register` - Register
- `POST /api/v1/requirements/upload` - Upload file
- `POST /api/v1/requirements/paste` - Paste text
- `GET /api/v1/health` - Health check

## Note

This is a **mock server** for testing only. In production, you'll need:
- Real database (PostgreSQL)
- JWT token generation
- File upload handling (multer)
- Proper authentication
- All the backend services from the architecture
