# Social API

JSON REST API for a social application (posts, likes, profiles, feed). Built with Spring Boot 4 and PostgreSQL, designed to be consumed by a React frontend.

## Tech stack

- Java 21
- Spring Boot 4.1 (Web, Security, Data JPA, Validation)
- PostgreSQL 18
- Liquibase migrations
- JWT access tokens + httpOnly refresh token cookies

## Documentation

- **[API Reference (docs/API.md)](docs/API.md)** — full endpoint documentation with request/response examples

## Features

- Email/password authentication with registration sandbox (admin approval required)
- JWT access token (15 min) + server-managed refresh token (7 days)
- User profiles with stats (total posts, total likes)
- Profile picture upload (multipart)
- Posts: create, like, unlike, soft delete, restore
- Cursor-based feed (infinite scroll, default 20 posts)
- Nightly cleanup of expired soft-deleted posts and refresh tokens
- CORS support for cross-domain SPA deployment

## API overview

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/auth/register` | Public | Register new user (pending approval) |
| `POST` | `/auth/login` | Public | Login, returns access token + refresh cookie |
| `POST` | `/auth/refresh` | Public | Rotate refresh token, get new access token |
| `POST` | `/auth/logout` | User + cookie | Revoke refresh token |
| `GET` | `/profile/me` | User | Get current user profile + stats |
| `PUT` | `/profile/me` | User | Update name and description |
| `POST` | `/profile/me/picture` | User | Upload profile picture |
| `POST` | `/posts` | User | Create post |
| `POST` | `/posts/{id}/like` | User | Like post |
| `DELETE` | `/posts/{id}/like` | User | Unlike post |
| `DELETE` | `/posts/{id}` | User | Soft delete own post |
| `POST` | `/posts/{id}/restore` | User | Restore soft-deleted post (10 days) |
| `GET` | `/feed?cursor=&limit=20` | User | Paginated feed |
| `GET` | `/admin/users/pending` | Admin | List users awaiting approval |
| `PUT` | `/admin/users/{id}/approve` | Admin | Approve user |

## Default admin account

Seeded on first startup if no admin exists:

- **Email:** `admin@social.com`
- **Password:** `Admin123!`

Override via environment variables:

```properties
ADMIN_SEED_EMAIL=admin@social.com
ADMIN_SEED_PASSWORD=Admin123!
```

## Authentication

Protected endpoints require:

```http
Authorization: Bearer <access_token>
```

Login and refresh set an httpOnly `refresh_token` cookie. The React frontend should call auth endpoints with `credentials: 'include'`.

**Typical SPA flow:**

1. `POST /auth/login` → store `accessToken` in memory
2. Call APIs with `Authorization: Bearer ...`
3. On 401 → `POST /auth/refresh` with cookie
4. `POST /auth/logout` → clear token and cookie

## Prerequisites (local development)

- Java 21
- Maven 3.9+
- PostgreSQL 18 (or compatible)

Create the database:

```sql
CREATE DATABASE social_api;
```

Update `src/main/resources/application.properties` if your PostgreSQL connection differs.

## Run locally

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

API: `http://localhost:8080`

## Run with Docker

Build and start API + PostgreSQL:

```bash
docker compose up --build
```

- API: `http://localhost:8080`
- PostgreSQL: `localhost:5433`

Stop:

```bash
docker compose down
```

Build image only:

```bash
docker build -t social-api .
```

Run container manually (requires external PostgreSQL):

```bash
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5433/social_api \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=postgres \
  -e JWT_SECRET=your-secret-key \
  social-api
```

## Tests

```bash
./mvnw test
```

With coverage check (≥90% on core packages):

```bash
./mvnw verify
```

Coverage report: `target/site/jacoco/index.html`

Unit tests follow the naming convention:

```text
conditions_methodUnderTest_expectedResult
```

Example: `duplicateEmail_register_throwsEmailAlreadyExistsException`

## Configuration

Key properties (env vars use Spring relaxed binding, e.g. `JWT_SECRET` → `jwt.secret`):

| Property | Default | Description |
|----------|---------|-------------|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5433/social_api` | Database URL |
| `jwt.secret` | — | JWT signing secret (required in production) |
| `jwt.access-expiration` | `900000` | Access token TTL (ms), 15 min |
| `jwt.refresh-expiration` | `604800000` | Refresh token TTL (ms), 7 days |
| `app.cors.allowed-origins` | `https://app.somedomain.com,http://localhost:3000` | Allowed CORS origins |
| `post.soft-delete.retention-days` | `10` | Soft delete restore window + hard delete cutoff |
| `app.upload.profile-pictures-dir` | `uploads/profile-pictures` | Profile picture storage path |

Production profile:

```bash
java -jar app.jar --spring.profiles.active=prod
```

Activates secure cookies (`SameSite=None`, `Secure=true`) from `application-prod.properties`.

## Project structure

```text
src/main/java/com/waracle/social_api/
├── controller/       REST endpoints
├── service/          Business logic
├── repository/       Spring Data JPA
├── entity/           JPA entities
├── security/         JWT filter, CORS, cookies
├── config/           App configuration
├── scheduler/        Nightly cleanup jobs
└── exception/        Domain exceptions + handler

src/main/resources/db/changelog/   Liquibase migrations
src/test/                          Unit tests (Mockito + JUnit 5)
```

## License

MIT (or update as needed)
