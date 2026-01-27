# Login Page

## Overview
Login page for the SDLC Automation Platform. Allows users to authenticate and access the system.

## Features
- Email and password authentication
- Form validation
- Error handling
- Loading states
- Remember me functionality
- Forgot password link
- Sign up link

## Usage

### Route
`/login`

### API Endpoint
- **POST** `/api/v1/auth/login`
- Request body:
  ```json
  {
    "email": "user@example.com",
    "password": "password123"
  }
  ```
- Response:
  ```json
  {
    "token": "jwt_token_here",
    "user": {
      "id": "user_123",
      "email": "user@example.com",
      "name": "John Doe"
    }
  }
  ```

## Components
- `page.tsx` - Main login page component
- Uses Tailwind CSS for styling
- Integrates with authentication API client

## Environment Variables
- `NEXT_PUBLIC_API_URL` - Base URL for API (default: http://localhost:3000/api/v1)

## Next Steps
1. Create register page
2. Create forgot password page
3. Add password strength indicator
4. Add social login (optional)
5. Add 2FA (optional)
