# Social API — REST Documentation

Manual API reference for the Social API backend. Intended for the React frontend at `app.somedomain.com` consuming this API at `api.somedomain.com`.

**Base URL (local):** `http://localhost:8080`  
**Base URL (production):** `https://api.somedomain.com`

All request and response bodies are JSON unless stated otherwise.  
All timestamps use ISO-8601 local date-time format (e.g. `2026-07-30T14:30:00`).

---

## Table of contents

1. [Authentication](#authentication)
2. [Common conventions](#common-conventions)
3. [Error handling](#error-handling)
4. [Auth endpoints](#auth-endpoints)
5. [Profile endpoints](#profile-endpoints)
6. [Post endpoints](#post-endpoints)
7. [Feed endpoint](#feed-endpoint)
8. [Admin endpoints](#admin-endpoints)
9. [Data models](#data-models)
10. [Frontend integration notes](#frontend-integration-notes)

---

## Authentication

The API uses a **JWT access token** plus an **httpOnly refresh token cookie**.

| Token | Where stored | TTL | Purpose |
|-------|--------------|-----|---------|
| Access token | Client memory (e.g. React state) | 15 minutes | Sent on every protected request |
| Refresh token | httpOnly cookie (`refresh_token`) | 7 days | Silent re-authentication |

### Protected requests

Include the access token on every authenticated call:

```http
Authorization: Bearer <access_token>
```

### Refresh token cookie

Set automatically by the server on login and refresh:

| Attribute | Value |
|-----------|-------|
| Name | `refresh_token` |
| Path | `/auth` |
| httpOnly | `true` |
| SameSite | `Lax` (dev) / `None` (prod) |
| Secure | `false` (dev) / `true` (prod) |

The React app **must** call auth endpoints with cookies enabled:

```javascript
fetch('https://api.somedomain.com/auth/login', {
  method: 'POST',
  credentials: 'include',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ email, password }),
});
```

### Typical SPA flow

```
1. POST /auth/login          → receive accessToken + refresh cookie
2. Call APIs with Bearer token
3. On 401 Unauthorized       → POST /auth/refresh (cookie sent automatically)
4. POST /auth/logout         → revoke refresh token, clear cookie
```

### Registration sandbox

New users register with `POST /auth/register` and are created in a **pending** state. They **cannot log in** until an admin approves them via `PUT /admin/users/{id}/approve`. Attempting login before approval returns **403 Forbidden**.

---

## Common conventions

### HTTP status codes

| Code | Meaning |
|------|---------|
| `200` | Success (read, update, action with body) |
| `201` | Resource created |
| `204` | Success, no response body |
| `400` | Invalid request / validation failure |
| `401` | Missing or invalid access token |
| `403` | Authenticated but not allowed (e.g. unapproved account) |
| `404` | Resource not found |
| `409` | Conflict (duplicate like, already deleted, etc.) |
| `410` | Gone (restore window expired) |

### Content types

| Operation | Content-Type |
|-----------|--------------|
| JSON bodies | `application/json` |
| Profile picture upload | `multipart/form-data` |

---

## Error handling

All application errors return a consistent JSON body:

```json
{
  "errorMessage": "Human-readable description of the problem."
}
```

### Error reference

| HTTP | Condition | Example `errorMessage` |
|------|-----------|------------------------|
| `400` | Validation failure | Spring default validation message |
| `400` | User already approved | `"User is already approved."` |
| `400` | Post not deleted | `"Post is not deleted."` |
| `400` | Invalid profile picture | `"Unsupported profile picture type. Allowed: JPEG, PNG, WEBP, GIF."` |
| `401` | No/invalid JWT | Empty body (Spring Security) |
| `401` | Missing refresh token | `"Refresh token missing."` |
| `401` | Expired refresh token | `"Refresh token expired."` |
| `401` | Token reuse detected | `"Refresh token reuse detected. All active sessions have been revoked."` |
| `403` | Unapproved account login | `"Account is not approved yet."` |
| `404` | User not found | `"User not found."` |
| `404` | Post not found | `"Post not found."` |
| `404` | Like not found | `"Post not liked."` |
| `409` | Duplicate email | `"Email already exists."` |
| `409` | Already liked | `"Post already liked."` |
| `409` | Already deleted | `"Post is already deleted."` |
| `410` | Restore window expired | `"Restore window expired. Posts can only be restored within 10 days of deletion."` |

---

## Auth endpoints

### Register

Create a new user account. The account starts **disabled** until admin approval.

```http
POST /auth/register
Content-Type: application/json
```

**Request body**

| Field | Type | Required | Rules |
|-------|------|----------|-------|
| `email` | string | yes | Valid email format |
| `password` | string | yes | Minimum 8 characters |
| `name` | string | yes | Non-blank |

**Example request**

```json
{
  "email": "jane@example.com",
  "password": "SecurePass123",
  "name": "Jane Doe"
}
```

**Response — `201 Created`**

```json
{
  "message": "Registration successful. Waiting approval."
}
```

**Errors:** `409` if email already exists.

---

### Login

Authenticate with email and password. Returns an access token and sets the refresh cookie.

```http
POST /auth/login
Content-Type: application/json
```

**Request body**

| Field | Type | Required |
|-------|------|----------|
| `email` | string | yes |
| `password` | string | yes |

**Example request**

```json
{
  "email": "jane@example.com",
  "password": "SecurePass123"
}
```

**Response — `200 OK`**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Set-Cookie header:** `refresh_token=...; Path=/auth; HttpOnly; Max-Age=604800`

**Errors:** `403` if account not yet approved; `401` if credentials invalid.

---

### Refresh access token

Exchange a valid refresh cookie for a new access token. The refresh token is **rotated** (old one revoked, new one issued).

```http
POST /auth/refresh
Cookie: refresh_token=<token>
```

No request body.

**Response — `200 OK`**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Errors:** `401` if cookie missing, expired, or reuse detected.

---

### Logout

Revokes the current refresh token, blacklists the access token immediately, and clears the cookie. Requires both a valid access token and the refresh token cookie.

```http
POST /auth/logout
Authorization: Bearer <access_token>
Cookie: refresh_token=<token>
```

No request body.

**Response — `200 OK`**

```json
{
  "message": "Logged out successfully."
}
```

**Errors:** `401` if access token or refresh cookie is missing.

---

## Profile endpoints

All profile endpoints require authentication.

### Get current user profile

Returns profile data and aggregate stats for the logged-in user.

```http
GET /profile/me
Authorization: Bearer <access_token>
```

**Response — `200 OK`**

```json
{
  "name": "Jane Doe",
  "description": "Software developer.",
  "profilePicture": "/uploads/profile-pictures/42-a1b2c3d4.jpg",
  "totalPosts": 12,
  "totalLikes": 47
}
```

| Field | Description |
|-------|-------------|
| `totalPosts` | Count of non-deleted posts authored by the user |
| `totalLikes` | Sum of likes on all posts authored by the user |
| `profilePicture` | Public URL path to the uploaded image, or `null` |

Profile pictures are served from `/uploads/profile-pictures/{filename}` (public, no auth).

---

### Update profile

Update name and description. Email and password cannot be changed through this endpoint.

```http
PUT /profile/me
Authorization: Bearer <access_token>
Content-Type: application/json
```

**Request body**

| Field | Type | Required | Rules |
|-------|------|----------|-------|
| `name` | string | yes | Non-blank |
| `description` | string | no | Free text |

**Example request**

```json
{
  "name": "Jane Smith",
  "description": "Full-stack developer."
}
```

**Response — `200 OK`**

Same shape as `GET /profile/me`.

---

### Upload profile picture

Upload or replace the profile picture. Accepts multipart file upload (not a URL).

```http
POST /profile/me/picture
Authorization: Bearer <access_token>
Content-Type: multipart/form-data
```

**Form field**

| Field | Type | Required | Rules |
|-------|------|----------|-------|
| `file` | file | yes | JPEG, PNG, WEBP, or GIF; max 5 MB |

**Example (curl)**

```bash
curl -X POST http://localhost:8080/profile/me/picture \
  -H "Authorization: Bearer <token>" \
  -F "file=@photo.jpg"
```

**Response — `200 OK`**

Same shape as `GET /profile/me`, with updated `profilePicture`.

**Errors:** `400` for missing file, unsupported type, or size exceeded.

---

## Post endpoints

All post endpoints require authentication.

### Create post

```http
POST /posts
Authorization: Bearer <access_token>
Content-Type: application/json
```

**Request body**

| Field | Type | Required | Rules |
|-------|------|----------|-------|
| `content` | string | yes | Non-blank, max 5000 characters |

**Example request**

```json
{
  "content": "Hello, world!"
}
```

**Response — `201 Created`**

```json
{
  "id": 101,
  "content": "Hello, world!",
  "authorName": "Jane Doe",
  "authorEmail": "jane@example.com",
  "createdAt": "2026-07-30T14:30:00",
  "likedBy": []
}
```

> `likedBy` is empty on create. The feed endpoint populates it for listed posts.

---

### Like post

```http
POST /posts/{id}/like
Authorization: Bearer <access_token>
```

**Response — `201 Created`**

```json
{
  "message": "Post liked successfully."
}
```

**Errors:** `404` post not found; `409` already liked.

---

### Unlike post

```http
DELETE /posts/{id}/like
Authorization: Bearer <access_token>
```

**Response — `204 No Content`**

**Errors:** `404` post not found or not liked.

---

### Soft delete post

Marks a post as deleted. Only the **author** can delete their own post. Soft-deleted posts are excluded from the feed.

```http
DELETE /posts/{id}
Authorization: Bearer <access_token>
```

**Response — `204 No Content`**

**Errors:** `404` post not found or not owned by caller; `409` already deleted.

---

### Restore post

Restore a soft-deleted post within the **10-day** retention window.

```http
POST /posts/{id}/restore
Authorization: Bearer <access_token>
```

**Response — `200 OK`**

```json
{
  "message": "Post restored successfully."
}
```

**Errors:** `404` post not found; `400` post not deleted; `410` restore window expired.

> Posts soft-deleted more than 10 days ago are permanently removed by a nightly cleanup job (03:00 server time).

---

## Feed endpoint

Returns a paginated list of published (non-deleted) posts, newest first. Designed for infinite scroll.

```http
GET /feed?cursor={postId}&limit=20
Authorization: Bearer <access_token>
```

**Query parameters**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `cursor` | long | — | Omit for first page. Pass `nextCursor` from previous response to load older posts. |
| `limit` | int | `20` | Page size (max `50`) |

**Example — first page**

```http
GET /feed?limit=20
Authorization: Bearer <access_token>
```

**Example — next page**

```http
GET /feed?cursor=85&limit=20
Authorization: Bearer <access_token>
```

**Response — `200 OK`**

```json
{
  "posts": [
    {
      "id": 101,
      "content": "Hello, world!",
      "authorName": "Jane Doe",
      "authorEmail": "jane@example.com",
      "createdAt": "2026-07-30T14:30:00",
      "likedBy": [
        {
          "id": 2,
          "email": "bob@example.com",
          "name": "Bob Smith",
          "createdAt": "2026-07-28T10:00:00"
        }
      ]
    }
  ],
  "nextCursor": 85,
  "hasMore": true
}
```

| Field | Description |
|-------|-------------|
| `posts` | Array of posts for this page |
| `nextCursor` | Pass as `cursor` to fetch the next page; `null` when no more pages |
| `hasMore` | `true` if additional pages exist |

### Infinite scroll integration

```javascript
// First load
const res = await fetch('/feed?limit=20', { headers: { Authorization: `Bearer ${token}` } });
const { posts, nextCursor, hasMore } = await res.json();

// Load more when user scrolls near bottom
if (hasMore) {
  const next = await fetch(`/feed?cursor=${nextCursor}&limit=20`, {
    headers: { Authorization: `Bearer ${token}` },
  });
}
```

---

## Admin endpoints

Require authentication with **ADMIN** role.

Default seeded admin (created on first startup):

- Email: `admin@social.com`
- Password: `Admin123!`

### List pending users

```http
GET /admin/users/pending
Authorization: Bearer <admin_access_token>
```

**Response — `200 OK`**

```json
[
  {
    "id": 5,
    "email": "jane@example.com",
    "name": "Jane Doe",
    "createdAt": "2026-07-30T12:00:00"
  }
]
```

---

### Approve user

Enable a pending user so they can log in.

```http
PUT /admin/users/{id}/approve
Authorization: Bearer <admin_access_token>
```

**Response — `200 OK`**

```json
{
  "message": "User approved successfully."
}
```

**Errors:** `404` user not found; `400` user already approved or admin account.

---

## Data models

### UserInfoResponse

Returned by profile endpoints.

```json
{
  "name": "string",
  "description": "string | null",
  "profilePicture": "string | null",
  "totalPosts": 0,
  "totalLikes": 0
}
```

### PostResponse

```json
{
  "id": 0,
  "content": "string",
  "authorName": "string",
  "authorEmail": "string",
  "createdAt": "2026-07-30T14:30:00",
  "likedBy": [ /* UserSummaryResponse[] */ ]
}
```

### UserSummaryResponse

Used in `likedBy` lists and admin pending users.

```json
{
  "id": 0,
  "email": "string",
  "name": "string",
  "createdAt": "2026-07-30T14:30:00"
}
```

### FeedResponse

```json
{
  "posts": [ /* PostResponse[] */ ],
  "nextCursor": 0,
  "hasMore": true
}
```

### MessageResponse

```json
{
  "message": "string"
}
```

### LoginResponse

```json
{
  "accessToken": "string"
}
```

### ErrorResponse

```json
{
  "errorMessage": "string"
}
```

---

## Frontend integration notes

### CORS

The API allows requests from configured origins (default):

- `https://app.somedomain.com`
- `http://localhost:3000`

Preflight `OPTIONS` requests are handled automatically.

### Cross-subdomain cookies (production)

When deploying API and frontend on different subdomains, set in production:

```properties
app.auth.cookie-domain=.somedomain.com
app.auth.cookie-secure=true
app.auth.cookie-same-site=None
```

Activate with `--spring.profiles.active=prod`.

### Profile picture URLs

Returned paths are relative (e.g. `/uploads/profile-pictures/42-abc.jpg`). Prepend the API base URL in the frontend:

```javascript
const fullUrl = `${API_BASE}${profile.profilePicture}`;
// → https://api.somedomain.com/uploads/profile-pictures/42-abc.jpg
```

### Endpoint access summary

| Path | Access |
|------|--------|
| `/auth/register`, `/auth/login`, `/auth/refresh` | Public |
| `/auth/logout` | User (Bearer token + refresh cookie) |
| `/uploads/profile-pictures/**` | Public |
| `/profile/**`, `/posts/**`, `/feed/**` | Authenticated user |
| `/admin/**` | Admin role |

---

## Background jobs

| Job | Schedule | Description |
|-----|----------|-------------|
| Post cleanup | Daily 03:00 | Hard-deletes posts soft-deleted 10+ days ago |
| Refresh token cleanup | Daily 03:30 | Removes expired/revoked refresh tokens (30-day retention) |
