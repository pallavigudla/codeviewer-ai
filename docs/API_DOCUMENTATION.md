# REST API Documentation — Code Review Agent

## Authentication & Workspace Endpoints

### 1. Register User (Initiate OTP)
* **URL**: `POST /api/auth/register`
* **Body**:
```json
{
  "fullName": "Pallavi Siri",
  "username": "pallavi_siri",
  "email": "pallavi@codereviewagent.com",
  "password": "password123"
}
```

### 2. Verify Email OTP & Create User
* **URL**: `POST /api/auth/verify-email`
* **Body**:
```json
{
  "email": "pallavi@codereviewagent.com",
  "otpCode": "123456"
}
```

### 3. Login
* **URL**: `POST /api/auth/login`
* **Body**:
```json
{
  "emailOrUsername": "pallavi@codereviewagent.com",
  "password": "password123",
  "rememberMe": true
}
```

### 4. Update Workspace Mode
* **URL**: `PUT /api/auth/workspace-mode?userId={userId}&workspaceMode=INDIVIDUAL`

---

## AI Code Review Endpoints

### 1. Run AI Review on Pasted Code
* **URL**: `POST /api/ai/review`
* **Body**:
```json
{
  "codeContent": "public class User { ... }",
  "language": "Java",
  "title": "User Refactor",
  "authorId": "uuid"
}
```

---

## Team Workspace Endpoints

### 1. Create Workspace
* **URL**: `POST /api/team/create`
* **Body**:
```json
{
  "teamName": "Core Engineering",
  "projectName": "Code Review Agent",
  "memberLimit": 10,
  "leadUserId": "uuid"
}
```

### 2. Join Workspace
* **URL**: `POST /api/team/join`
* **Body**:
```json
{
  "joinCode": "CRA472",
  "userId": "uuid"
}
```
