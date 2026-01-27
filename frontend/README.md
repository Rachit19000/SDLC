# SDLC Login Page - React App

Simple React login page with email and password fields.

## Installation

1. Install dependencies:
```bash
npm install
```

2. Start the development server:
```bash
npm start
```

3. Open [http://localhost:3000](http://localhost:3000) in your browser.

## Features

- Email and password input fields
- Form validation
- Error handling
- Loading states
- Clean, modern UI

## API Endpoint

The login form sends a POST request to:
```
http://localhost:3000/api/v1/auth/login
```

Request body:
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

## Project Structure

```
frontend/
├── public/
│   └── index.html
├── src/
│   ├── components/
│   │   ├── Login.js
│   │   └── Login.css
│   ├── App.js
│   ├── App.css
│   ├── index.js
│   └── index.css
└── package.json
```
