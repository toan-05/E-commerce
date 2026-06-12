# Manual Auth, Dynamic RBAC, And IP Rate Limit Plan

This document defines the phase 1.5 backend authentication foundation.

OAuth2/OpenID Connect is out of scope for this phase.

Token persistence is also out of scope: do not create token entities, token repositories, token migrations, or token tables.

## References

- Spring Security Password Storage: https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html
- Spring Security Authentication Architecture: https://docs.spring.io/spring-security/reference/servlet/authentication/architecture.html
- Spring Security Authorization Architecture: https://docs.spring.io/spring-security/reference/servlet/authorization/architecture.html
- OWASP Password Storage Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html
- OWASP JSON Web Token for Java Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html
- Token Bucket algorithm: https://en.wikipedia.org/wiki/Token_bucket

## Goals

- Support backend-managed register/login with email and password.
- Store passwords using strong one-way hashing.
- Issue short-lived stateless JWT access tokens.
- Support dynamic RBAC: roles and permissions are data in the database, not Java enums.
- Rate limit auth endpoints by client IP.
- Return auth and rate-limit errors through the existing `ApiErrorResponse` shape.
- Keep Spring Security stateless for API requests.
- Keep DTOs grouped by module under `dto/request/auth` and `dto/response/auth`.

## Non-Goals

- Do not implement OAuth2 or OpenID Connect in this phase.
- Do not support Keycloak tokens in this phase.
- Do not create any database table related to tokens.
- Do not persist JWT IDs, refresh tokens, access tokens, token families, or token revocation state.
- Do not implement server-side logout or token revocation in this phase.
- Do not hard-code role or permission catalogs as enums.
- Do not add Redis/distributed rate limiting yet.
- Do not store plaintext passwords.
- Do not use long-lived access tokens.

## Architecture

```text
Register
  Client -> POST /api/v1/auth/register
         -> validate request
         -> hash password
         -> create UserAccount with default role code USER
         -> issue stateless access token

Login
  Client -> POST /api/v1/auth/login
         -> verify password
         -> load roles and permissions from DB
         -> issue stateless access token

Authenticated request
  Client -> Authorization: Bearer <access-token>
         -> JwtAuthenticationFilter validates JWT signature and expiry
         -> SecurityContext contains current user id, email, roles, permissions

Rate limit
  Client -> auth endpoint
         -> RateLimitFilter resolves client IP
         -> filter checks endpoint-specific token bucket
         -> request continues or returns 429
```

Client-side logout means the client deletes its token. Because the backend does not store tokens, the backend cannot revoke a previously issued JWT before its expiry.

## Package Plan

Continue using the current layer-based structure. DTOs stay module-grouped.

```text
controller/
  AuthController.java
  MeController.java

service/
  AuthService.java
  UserAccountService.java

service/impl/
  AuthServiceImpl.java
  UserAccountServiceImpl.java

entity/
  UserAccount.java
  Role.java
  Permission.java

entity/enums/
  UserStatus.java

repository/
  UserAccountRepository.java
  RoleRepository.java

config/
  AppProperties.java
  SecurityConfig.java

security/
  CurrentUser.java
  CurrentUserResolver.java
  JwtTokenProvider.java
  JwtAuthenticationFilter.java
  JwtAuthenticationEntryPoint.java
  JwtAccessDeniedHandler.java
  ClientIpResolver.java
  RateLimitFilter.java
  RateLimitPolicy.java

dto/request/auth/
  RegisterRequest.java
  LoginRequest.java

dto/response/auth/
  AuthTokenResponse.java
  AuthUserResponse.java
  MeResponse.java
```

Do not add:

```text
token persistence entity/repository/service/dto
server-side logout dto/controller flow
role enum catalog
permission enum catalog
```

## Data Model

### user_accounts

```text
id BIGINT PK
email VARCHAR(255) UNIQUE NOT NULL
password_hash VARCHAR(255) NOT NULL
full_name VARCHAR(255) NULL
phone VARCHAR(50) NULL
user_status VARCHAR(50) NOT NULL
email_verified BOOLEAN NOT NULL DEFAULT FALSE
created_at TIMESTAMP(6) NOT NULL
updated_at TIMESTAMP(6) NOT NULL
status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE'
```

Rules:

- Normalize email before saving: lowercase and trim.
- Never return `password_hash` from API responses.
- Use existing `BaseEntity` conventions.

### roles

```text
id BIGINT PK
code VARCHAR(100) UNIQUE NOT NULL
name VARCHAR(150) NOT NULL
description VARCHAR(500) NULL
created_at TIMESTAMP(6) NOT NULL
updated_at TIMESTAMP(6) NOT NULL
status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE'
```

Initial seed roles:

```text
USER
ADMIN
STAFF
WAREHOUSE
```

Rules:

- `code` is a stable machine-readable key, normalized to uppercase snake case.
- `name` is display text.
- New roles can be created later from an admin API without changing Java code or DB schema.

### permissions

```text
id BIGINT PK
code VARCHAR(120) UNIQUE NOT NULL
name VARCHAR(150) NOT NULL
description VARCHAR(500) NULL
created_at TIMESTAMP(6) NOT NULL
updated_at TIMESTAMP(6) NOT NULL
status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE'
```

Initial seed permissions:

```text
PRODUCT_READ
PRODUCT_WRITE
ORDER_READ
ORDER_WRITE
INVENTORY_READ
INVENTORY_WRITE
USER_READ
USER_WRITE
ROLE_READ
ROLE_WRITE
```

Rules:

- `code` is a stable machine-readable key, normalized to uppercase snake case.
- New permissions can be created later from an admin API without changing Java code or DB schema.
- Business code should reference permission code constants in one central class if needed, not an enum-backed schema.

### user_roles

```text
user_id BIGINT NOT NULL
role_id BIGINT NOT NULL
PRIMARY KEY (user_id, role_id)
```

This is many-to-many so one user can have multiple roles.

### role_permissions

```text
role_id BIGINT NOT NULL
permission_id BIGINT NOT NULL
PRIMARY KEY (role_id, permission_id)
```

This is many-to-many so one role can have many permissions and one permission can belong to many roles.

## Password Handling

Use Spring Security `PasswordEncoder`.

Password encoder bean:

```java
new BCryptPasswordEncoder()
```

Rules:

- Do not store plaintext passwords.
- Do not use fast hashes like MD5 or SHA-256 for password storage.
- Use bcrypt through Spring Security for the first implementation.
- Validate password length.
- Reject passwords longer than 72 bytes while using bcrypt.

Initial password validation:

```text
min length: 8
max length: 72
must not be blank
```

## Token Strategy

Use backend-issued stateless JWT access tokens.

Suggested claims:

```json
{
  "iss": "order-service",
  "sub": "internal-user-id",
  "email": "user@example.com",
  "roles": ["USER"],
  "permissions": ["PRODUCT_READ", "ORDER_READ", "ORDER_WRITE"],
  "iat": 1710000000,
  "exp": 1710000900,
  "jti": "uuid"
}
```

Suggested lifetime:

```text
access token: 15 minutes
```

Use HS512 with an HMAC secret from configuration for local phase 1.5. Do not commit production secrets.

Important tradeoff:

- Because tokens are not stored, the backend cannot revoke a token before `exp`.
- Keep the access token short-lived.
- If refresh token support is needed later without DB storage, it must be stateless and will have weaker revocation/rotation guarantees unless a shared denylist store such as Redis is introduced.

## Dynamic RBAC Strategy

Authorize by permissions first, not only by role names.

Recommended flow:

```text
UserAccount -> roles -> permissions -> Spring GrantedAuthority
```

Authority naming:

```text
ROLE_ADMIN
ROLE_USER
PERMISSION_PRODUCT_READ
PERMISSION_ORDER_WRITE
```

Implementation guidance:

- Load role and permission data during login.
- Put role codes and permission codes into the JWT so normal request authorization stays stateless.
- For sensitive admin actions, a later phase can reload the user from DB to enforce fresh permissions.
- Keep `Role` and `Permission` as normal entities so admin APIs can create/update them later.
- Do not use Java enums for role or permission catalogs.

## IP Rate Limit Strategy

Rate limit auth endpoints because they are targets for brute force and spam.

Use in-memory token buckets in this phase. This matches the current single-instance local/dev setup. If the app later runs multiple instances, move rate-limit state to Redis or another shared store.

Suggested library:

```text
Bucket4j
```

Rate limit key:

```text
<client-ip>:<endpoint-group>
```

Endpoint groups:

```text
AUTH_LOGIN
AUTH_REGISTER
```

Initial policy:

```text
POST /api/v1/auth/login       5 requests / minute / IP
POST /api/v1/auth/register    3 requests / minute / IP
```

Do not strictly rate limit normal product/order APIs in this phase.

### Client IP Resolution

Create `ClientIpResolver`.

Local development:

```text
request.getRemoteAddr()
```

Future reverse proxy support:

```text
X-Forwarded-For
X-Real-IP
Forwarded
```

Only trust forwarded headers when `app.rate-limit.trusted-proxy-enabled=true`.

### Rate Limit Response

When a request exceeds its limit:

```http
HTTP/1.1 429 Too Many Requests
Retry-After: 60
```

Response body:

```json
{
  "success": false,
  "timestamp": "...",
  "status": 429,
  "code": "RATE_LIMIT_EXCEEDED",
  "error": "Too Many Requests",
  "message": "Too many requests. Please try again later.",
  "path": "/api/v1/auth/login",
  "details": []
}
```

Recommended headers:

```text
Retry-After
X-RateLimit-Limit
X-RateLimit-Remaining
X-RateLimit-Reset
```

### Filter Order

Rate limiting should happen before expensive auth work.

```text
CORS
RateLimitFilter
JwtAuthenticationFilter
Spring authorization
Controller
```

Do not rate limit:

```text
OPTIONS /**
GET /actuator/health
GET /swagger-ui.html
GET /swagger-ui/**
GET /v3/api-docs/**
```

## API Contract

```http
POST /api/v1/auth/register
POST /api/v1/auth/login
GET  /api/v1/me
```

### Register

```json
{
  "email": "user@example.com",
  "password": "password123",
  "fullName": "Demo User"
}
```

### Login

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

### Auth Token Response

```json
{
  "success": true,
  "message": "Login completed",
  "data": {
    "accessToken": "...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": {
      "id": 1,
      "email": "user@example.com",
      "roles": ["USER"],
      "permissions": ["PRODUCT_READ", "ORDER_READ", "ORDER_WRITE"]
    }
  },
  "timestamp": "..."
}
```

There is no `refreshToken` field in this phase.

## Security Rules

```text
Public:
  POST /api/v1/auth/register
  POST /api/v1/auth/login
  GET  /actuator/health
  GET  /swagger-ui.html, /swagger-ui/**, /v3/api-docs/**

Authenticated:
  GET /api/v1/me
  GET /api/v1/orders/**
  POST /api/v1/orders

Permission-based:
  hasAuthority("PERMISSION_PRODUCT_WRITE") for product mutations
  hasAuthority("PERMISSION_INVENTORY_READ") for inventory reads
  hasAuthority("PERMISSION_INVENTORY_WRITE") for inventory mutations
  hasAuthority("PERMISSION_USER_READ") for user admin reads
  hasAuthority("PERMISSION_USER_WRITE") for user admin mutations
```

Product read policy decision:

```text
Phase 1.5: keep current behavior if desired.
Phase 2 catalog: make GET /api/v1/products/** public for storefront.
```

## Error Codes

Add:

```text
AUTH_INVALID_CREDENTIALS
AUTH_EMAIL_ALREADY_EXISTS
AUTH_USER_DISABLED
AUTH_TOKEN_INVALID
AUTH_TOKEN_EXPIRED
AUTH_FORBIDDEN
RATE_LIMIT_EXCEEDED
```

HTTP mapping:

```text
400 validation/malformed request
401 invalid credentials or invalid token
403 authenticated but not allowed
409 duplicate email conflict
429 too many requests
```

Login errors should not reveal whether the email exists. Use:

```text
Invalid email or password
```

## Configuration

Add:

```yaml
app:
  auth:
    issuer: ${APP_AUTH_ISSUER:order-service}
    access-token-ttl-seconds: ${APP_AUTH_ACCESS_TOKEN_TTL_SECONDS:900}
    jwt-secret: ${APP_AUTH_JWT_SECRET:dev-secret-change-me}
  rate-limit:
    enabled: ${APP_RATE_LIMIT_ENABLED:true}
    trusted-proxy-enabled: ${APP_TRUSTED_PROXY_ENABLED:false}
    login-limit-per-minute: ${APP_RATE_LIMIT_LOGIN_PER_MINUTE:5}
    register-limit-per-minute: ${APP_RATE_LIMIT_REGISTER_PER_MINUTE:3}
```

## Implementation Phases

### Phase 1.5A: User, Role, Permission Model

- Add `UserAccount`.
- Add `Role` and `Permission`.
- Add `UserStatus`.
- Add Flyway migration for `user_accounts`, `roles`, `permissions`, `role_permissions`, and `user_roles`.
- Add `UserAccountRepository` and `RoleRepository`.
- Add `PermissionRepository` later with admin permission-management APIs.
- Seed default roles and permissions.
- Do not add role/permission enum catalogs.
- Do not add token persistence.

### Phase 1.5B: Register/Login

- Add auth DTOs.
- Add `BCryptPasswordEncoder` in `SecurityConfig`.
- Add `JwtTokenProvider` under `security`.
- Add `AuthService`.
- Add `AuthController`.
- Assign the default `USER` role during registration by role code lookup.
- Return only access token, token type, expiry, user roles, and permissions.

### Phase 1.5C: IP Rate Limit

- Add `RateLimitConfig`.
- Add `ClientIpResolver`.
- Add `RateLimitPolicy`.
- Add `RateLimitFilter`.
- Apply rate limits to register/login endpoints.
- Return `RATE_LIMIT_EXCEEDED` with HTTP `429`.

### Phase 1.5D: Current User And RBAC Wiring

- Add `JwtAuthenticationFilter`.
- Map role codes and permission codes to Spring authorities.
- Update `MeController`.
- Return user id, email, roles, and permissions from the authenticated token.
- Keep response wrapped by `ApiResponse`.

### Phase 1.5E: Security Rules And Tests

- Update `SecurityConfig`.
- Add tests for register, login, `/me`, permission checks, disabled users, invalid tokens, expired tokens, and rate limit exceeded.

## Acceptance Criteria

- User can register and log in.
- Login returns a short-lived stateless access token only.
- No token-persistence entity, repository, migration, or table exists.
- No role or permission enum catalog exists.
- Role and permission records can be extended later through admin APIs without schema or Java enum changes.
- Access token can call protected APIs.
- Role codes and permission codes are available for RBAC enforcement.
- Auth endpoints are rate limited by IP.
- Exceeding rate limit returns HTTP `429` and `RATE_LIMIT_EXCEEDED`.
- `/api/v1/me` returns the current user.
- Passwords are stored only as hashes.
- Backend tests pass.

## Decisions

- Use backend-issued JWT access tokens.
- Do not persist any token data in DB.
- Do not implement refresh token or server-side logout in this phase.
- Use dynamic role and permission tables for RBAC extensibility.
- Use in-memory IP rate limiting for auth endpoints in this phase.
- Put cross-cutting request security classes under `security`.
- Do not implement OAuth2/OIDC in this phase.
- Continue using the current layer-based package structure outside DTOs and security.
