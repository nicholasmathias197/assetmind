# JWT Authentication Guide

## Overview

AssetMind now includes comprehensive JWT (JSON Web Token) authentication with role-based access control. All asset endpoints are secured behind JWT tokens.

## Getting Started

### 1. Register a New User

```bash
curl -X POST "http://localhost:8080/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "SecurePassword123",
    "email": "john@example.com"
  }'
```

**Response (201 Created)**:
```json
{
  "message": "User registered successfully",
  "userId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### 2. Login to Get Tokens

```bash
curl -X POST "http://localhost:8080/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "SecurePassword123"
  }'
```

**Response (200 OK)**:
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyLWlkIiwicm9sZSI6IlVTRVIiLCJpYXQiOjE2NDUyNjQ2MDAsImV4cCI6MTY0NTI2ODIwMH0.abc...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyLWlkIiwiaWF0IjoxNjQ1MjY0NjAwLCJleHAiOjE2NDUzNTA2MDB9.xyz...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

### 3. Use Access Token for API Requests

All secured endpoints require the `Authorization` header with the access token:

```bash
curl -X GET "http://localhost:8080/api/v1/assets/page" \
  -H "Authorization: Bearer <accessToken>"
```

Replace `<accessToken>` with the actual token from the login response.

### 4. Refresh Access Token (When Expired)

```bash
curl -X POST "http://localhost:8080/api/v1/auth/refresh" \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "<refreshToken>"
  }'
```

**Response (200 OK)**:
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "<original_refreshToken>",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

## Token Configuration

### Default Expiration Times

- **Access Token**: 1 hour (3600000 ms)
- **Refresh Token**: 24 hours (86400000 ms)

### Custom Configuration

Set environment variables to customize token expiration:

```bash
export JWT_SECRET="your-super-secret-key-min-32-characters-12345"
export JWT_ACCESS_TOKEN_EXPIRATION=7200000
export JWT_REFRESH_TOKEN_EXPIRATION=604800000
```

Or set in `.env` file:
```
JWT_SECRET=your-super-secret-key-min-32-characters-12345
JWT_ACCESS_TOKEN_EXPIRATION=7200000
JWT_REFRESH_TOKEN_EXPIRATION=604800000
```

## Secured Endpoints

The following endpoints require authentication:

| Method | Endpoint | Auth Required |
|--------|----------|---------------|
| POST | `/api/v1/assets` | ✓ |
| GET | `/api/v1/assets` | ✓ |
| GET | `/api/v1/assets/{id}` | ✓ |
| PUT | `/api/v1/assets/{id}` | ✓ |
| GET | `/api/v1/assets/page` | ✓ |
| DELETE | `/api/v1/assets/{id}` | ✓ |
| POST | `/api/v1/depreciation/run` | ✓ |
| POST | `/api/v1/tax-strategy/recommend` | ✓ |
| POST | `/api/v1/classification/suggest` | ✓ |

## Public Endpoints (No Auth Required)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/v1/auth/login` | Login |
| POST | `/api/v1/auth/register` | Register new user |
| POST | `/api/v1/auth/refresh` | Refresh access token |
| GET | `/actuator/**` | Health/metrics |

## Complete Example Workflow

```bash
# 1. Register
curl -X POST "http://localhost:8080/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice",
    "password": "Password123",
    "email": "alice@example.com"
  }'

# 2. Login
TOKEN_RESPONSE=$(curl -X POST "http://localhost:8080/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice",
    "password": "Password123"
  }')

ACCESS_TOKEN=$(echo $TOKEN_RESPONSE | jq -r '.accessToken')

# 3. Create Asset (requires token)
curl -X POST "http://localhost:8080/api/v1/assets" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{
    "id": "asset-1",
    "description": "Office Laptop",
    "assetClass": "COMPUTER_EQUIPMENT",
    "costBasis": 1200.00,
    "inServiceDate": "2026-03-26",
    "usefulLifeYears": 5
  }'

# 4. List Assets (requires token)
curl -X GET "http://localhost:8080/api/v1/assets/page" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

## Error Handling

### Missing Authorization Header
```bash
curl -X GET "http://localhost:8080/api/v1/assets"
```

**Response (401 Unauthorized)**:
```json
{
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "status": 401
}
```

### Invalid Token
```bash
curl -X GET "http://localhost:8080/api/v1/assets" \
  -H "Authorization: Bearer invalid-token"
```

**Response (401 Unauthorized)**:
```json
{
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "status": 401
}
```

### Invalid Credentials
```bash
curl -X POST "http://localhost:8080/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice",
    "password": "WrongPassword"
  }'
```

**Response (401 Unauthorized)**:
```json
{
  "error": "Unauthorized",
  "message": "Invalid credentials",
  "status": 401
}
```

## Security Best Practices

1. **Store tokens securely**: Use secure storage in your client application (HttpOnly cookies, secure storage in mobile apps).
2. **Always use HTTPS**: Don't transmit tokens over unencrypted connections.
3. **Rotate keys**: Change `JWT_SECRET` periodically in production.
4. **Short expiration**: Access tokens expire after 1 hour; users must refresh to get a new one.
5. **Refresh token rotation**: Consider rotating refresh tokens on each use for additional security.

## Role-Based Access Control (Future)

Currently, all authenticated users have "USER" role. Future versions will support:

- `ADMIN`: Full access
- `AUDITOR`: Read-only access
- `USER`: Limited asset management

Example future request:
```bash
curl -X DELETE "http://localhost:8080/api/v1/assets/asset-1" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

## Troubleshooting

### Token Expired
If you get a 401 error, the access token may have expired. Use the refresh endpoint:

```bash
curl -X POST "http://localhost:8080/api/v1/auth/refresh" \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "<refreshToken>"}'
```

### User Already Exists
```bash
curl -X POST "http://localhost:8080/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"username": "existing_user", "password": "...", "email": "..."}'
```

**Response (409 Conflict)**:
```json
{
  "error": "Conflict",
  "message": "Username already exists",
  "status": 409
}
```

## Testing with cURL

Save this script as `test_jwt.sh`:

```bash
#!/bin/bash

BASE_URL="http://localhost:8080"

# Register
echo "Registering user..."
REGISTER=$(curl -s -X POST "$BASE_URL/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "TestPass123",
    "email": "test@example.com"
  }')
echo $REGISTER | jq .

# Login
echo "Logging in..."
LOGIN=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "TestPass123"
  }')
echo $LOGIN | jq .

ACCESS_TOKEN=$(echo $LOGIN | jq -r '.accessToken')

# Get assets with token
echo "Getting assets..."
curl -s -X GET "$BASE_URL/api/v1/assets/page" \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq .
```

Run with: `bash test_jwt.sh`

