# AssetMind — Role-Based Access Control & Admin Guide

This guide covers how to set up, configure, and use the role-based access control (RBAC) system in AssetMind, including the admin dashboard, feature-level permissions, and the bootstrap-admin flow.

---

## Table of Contents

1. [Overview](#overview)
2. [Roles and Feature Permissions](#roles-and-feature-permissions)
3. [Getting Started — Creating Your First Admin](#getting-started--creating-your-first-admin)
4. [Configuration](#configuration)
5. [Admin Dashboard (Frontend)](#admin-dashboard-frontend)
6. [Managing Users via API](#managing-users-via-api)
7. [Feature-Gated Routes (Frontend)](#feature-gated-routes-frontend)
8. [API Reference](#api-reference)
9. [Security Notes](#security-notes)
10. [Troubleshooting](#troubleshooting)

---

## Overview

AssetMind uses JWT-based authentication with two layers of authorization:

| Layer | Purpose |
|-------|---------|
| **Roles** (`ADMIN`, `USER`) | Controls access to admin endpoints and overall privilege level |
| **Feature Flags** | Controls which product features a `USER` can access |

Admins bypass all feature checks — they can access every endpoint.  
Regular users can only access endpoints whose feature flag has been explicitly granted to them.

### Available Feature Flags

| Feature Flag | Protects |
|---|---|
| `ASSETS` | `/api/v1/assets/**` — Asset CRUD, import/export |
| `DEPRECIATION` | `/api/v1/depreciation/**` — Depreciation calculations & AI recommendations |
| `TAX_STRATEGY` | `/api/v1/tax-strategy/**` — Tax optimisation recommendations |
| `CLASSIFICATION` | `/api/v1/classification/**` — AI document classification |
| `BREAKOUT` | `/api/v1/breakout/**` — Component breakout suggestions |

---

## Roles and Feature Permissions

### ADMIN

- Full access to every endpoint (no feature flag checks).
- Can manage all users from the Admin Dashboard.
- Can change user roles, enable/disable accounts, and grant/revoke feature permissions.

### USER

- Must be explicitly granted one or more feature flags to access the corresponding pages/endpoints.
- A newly registered user has **no features** enabled by default. An admin must grant access.
- Attempting to access a feature without permission returns `403 Forbidden` (API) or shows the "Access Denied" page (frontend).

---

## Getting Started — Creating Your First Admin

When you deploy AssetMind for the first time, there are no users. Use the **bootstrap-admin** endpoint to create the initial admin account.

### Prerequisites

1. The backend is running (see the main README for startup instructions).
2. You have the **bootstrap secret key** — this is configured via the `BOOTSTRAP_SECRET` environment variable (default for local dev: `changeme-bootstrap-secret`).

### Step 1 — Bootstrap the Admin

```bash
curl -X POST http://localhost:8080/api/v1/auth/bootstrap-admin \
  -H "Content-Type: application/json" \
  -H "X-Bootstrap-Key: changeme-bootstrap-secret" \
  -d '{
    "username": "admin",
    "password": "YourSecureP@ss123",
    "email": "admin@yourcompany.com"
  }'
```

**Response** (201 Created):

```json
{
  "accessToken": "eyJhbGciOiJI...",
  "refreshToken": "eyJhbGciOiJI...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

The admin account is created with the `ADMIN` role and all feature flags enabled.

> **Important:** This endpoint only works **once**. After an admin exists, subsequent calls return `409 Conflict`. If the `X-Bootstrap-Key` header is missing or wrong, you get `403 Forbidden`.

### Step 2 — Log In via the Frontend

1. Open the app in your browser (default: `http://localhost:5173`).
2. Log in with the admin credentials you just created.
3. You will see all navigation links plus the **Admin** link in the navbar.

### Step 3 — Create Regular Users

Regular users can self-register through the **Register** page or via API:

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "jane",
    "password": "SecureP@ss456",
    "email": "jane@yourcompany.com"
  }'
```

New users start with the `USER` role and **no feature access**. They won't be able to use any features until an admin grants permissions.

### Step 4 — Grant Feature Access

Use the Admin Dashboard (see below) or the API to grant features to the new user.

---

## Configuration

### Environment Variables

| Variable | Default (dev) | Description |
|----------|---------------|-------------|
| `BOOTSTRAP_SECRET` | `changeme-bootstrap-secret` | Secret key required in the `X-Bootstrap-Key` header when calling `/api/v1/auth/bootstrap-admin`. **Change this in production.** |
| `JWT_SECRET` | *(dev-only value)* | Secret key for signing JWT tokens. **Must be changed in production.** |
| `JWT_ACCESS_TOKEN_EXPIRATION` | `3600000` (1 hour) | Access token lifetime in milliseconds. |
| `JWT_REFRESH_TOKEN_EXPIRATION` | `86400000` (24 hours) | Refresh token lifetime in milliseconds. |

### application.yml Snippet

```yaml
jwt:
  secret: ${JWT_SECRET:assetmind-local-jwt-secret-for-hs512-...}
  access-token-expiration: ${JWT_ACCESS_TOKEN_EXPIRATION:3600000}
  refresh-token-expiration: ${JWT_REFRESH_TOKEN_EXPIRATION:86400000}

assetmind:
  bootstrap-secret: ${BOOTSTRAP_SECRET:changeme-bootstrap-secret}
```

---

## Admin Dashboard (Frontend)

Navigate to **Admin** in the nav bar (visible only to admins).

### What You Can Do

| Action | How |
|--------|-----|
| **View all users** | The dashboard loads a grid of user cards automatically. |
| **Change a user's role** | Use the role dropdown on their card (`USER` or `ADMIN`). |
| **Enable / disable an account** | Toggle the **Enabled** checkbox. Disabled users cannot log in. |
| **Grant / revoke features** | Check or uncheck feature boxes (Assets, Depreciation, Tax Strategy, Classification, Breakout). |
| **Save changes** | Click the **Save** button on the user's card. A green success message confirms the update. |

### How It Works

- The frontend decodes the JWT access token to determine the user's role and features.
- Navigation links are only shown for features the current user has access to.
- `AdminRoute` and `FeatureRoute` components guard routes at the React Router level.
- If a user navigates to a URL they don't have access to, they see the **Access Denied** page with instructions to contact an admin.

---

## Managing Users via API

All admin endpoints require a valid JWT with the `ADMIN` role in the `Authorization: Bearer <token>` header.

### List All Users

```bash
curl http://localhost:8080/api/v1/admin/users \
  -H "Authorization: Bearer <admin-access-token>"
```

**Response** (200 OK):

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "username": "jane",
    "email": "jane@yourcompany.com",
    "role": "USER",
    "enabled": true,
    "featureAccess": [],
    "createdAt": "2026-04-05T10:30:00"
  }
]
```

### Update User Access

```bash
curl -X PUT http://localhost:8080/api/v1/admin/users/<userId>/access \
  -H "Authorization: Bearer <admin-access-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "role": "USER",
    "enabled": true,
    "featureAccess": ["ASSETS", "DEPRECIATION", "CLASSIFICATION"]
  }'
```

**Response** (200 OK): Returns the updated user object.

**Validation rules:**
- `role` must be `USER` or `ADMIN`.
- `featureAccess` entries must be from the allowed set: `ASSETS`, `DEPRECIATION`, `TAX_STRATEGY`, `CLASSIFICATION`, `BREAKOUT`. Unknown features are rejected with `400 Bad Request`.
- `enabled` controls whether the user can log in.

---

## Feature-Gated Routes (Frontend)

The React app uses wrapper components to protect pages:

| Component | Purpose |
|-----------|---------|
| `FeatureRoute` | Wraps a page and checks that the user has the required feature flag. Redirects to `/access-denied` if not. |
| `AdminRoute` | Wraps admin-only pages. Redirects to `/access-denied` if the user is not an `ADMIN`. |

### Route-to-Feature Mapping

| Route | Required Feature | Component |
|-------|-----------------|-----------|
| `/assets` | `ASSETS` | Assets |
| `/depreciation` | `DEPRECIATION` | Depreciation |
| `/tax-strategy` | `TAX_STRATEGY` | TaxStrategy |
| `/classification` | `CLASSIFICATION` | Classification |
| `/breakout` | `BREAKOUT` | Breakout |
| `/admin` | `ADMIN` role | AdminDashboard |

---

## API Reference

### Authentication Endpoints (no auth required)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/auth/register` | Create a new user account |
| `POST` | `/api/v1/auth/login` | Authenticate and receive JWT tokens |
| `POST` | `/api/v1/auth/refresh` | Exchange a refresh token for a new access token |
| `POST` | `/api/v1/auth/bootstrap-admin` | Create the first admin (requires `X-Bootstrap-Key` header) |

### Admin Endpoints (ADMIN role required)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/admin/users` | List all users |
| `PUT` | `/api/v1/admin/users/{userId}/access` | Update a user's role, enabled status, and feature access |

### JWT Token Structure

The access token payload includes:

```json
{
  "sub": "user-uuid",
  "username": "jane",
  "role": "USER",
  "featureAccess": ["ASSETS", "DEPRECIATION"],
  "iat": 1743868200,
  "exp": 1743871800
}
```

The backend sets Spring Security authorities from the token:
- `ROLE_USER` or `ROLE_ADMIN` (from `role` claim)
- `FEATURE_ASSETS`, `FEATURE_DEPRECIATION`, etc. (from `featureAccess` claim)

---

## Security Notes

1. **Change the bootstrap secret in production.** Set the `BOOTSTRAP_SECRET` environment variable to a strong random value. The default `changeme-bootstrap-secret` is for local development only.

2. **Change the JWT secret in production.** Set the `JWT_SECRET` environment variable to a cryptographically strong value (at least 64 characters for HS512).

3. **Bootstrap is one-time only.** Once an admin exists, the endpoint returns `409 Conflict`. There is no way to create additional admins via this endpoint — use the Admin Dashboard or API instead.

4. **Disabled users are blocked at login.** Even if a disabled user has a valid JWT from before they were disabled, the token will eventually expire. For immediate revocation, consider shortening token lifetimes.

5. **Feature checks are enforced server-side.** The frontend hides UI elements for convenience, but the actual authorization happens in Spring Security's filter chain. Even direct API calls without a matching feature authority are rejected with `403`.

---

## Troubleshooting

### "Admin already exists" when calling bootstrap-admin

An admin account was already created. Log in with the existing admin credentials, or check the database:

```sql
SELECT id, username, role FROM users WHERE role = 'ADMIN';
```

### User can't see any pages after registering

New users start with no feature access. An admin must:
1. Go to the Admin Dashboard.
2. Find the user's card.
3. Check the desired feature boxes and click **Save**.

### "Access Denied" page appears

The logged-in user doesn't have the required feature flag. Ask an admin to grant it via the Admin Dashboard.

### 403 on bootstrap-admin

Either the `X-Bootstrap-Key` header is missing, or its value doesn't match the configured `BOOTSTRAP_SECRET`.

### JWT token doesn't contain featureAccess

Make sure the user entity has the `feature_access` column populated. The Flyway migration `V4__add_feature_access_to_users.sql` adds this column. Run `flyway migrate` if needed.
