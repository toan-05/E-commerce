# OAuth2 / OIDC Learning Plan

This project will use Keycloak as the identity provider, React as an OAuth2/OIDC client, and Spring Boot as an OAuth2 Resource Server.

## Target Architecture

```text
React FE
  |
  | Authorization Code + PKCE
  v
Keycloak
  |
  | access token (JWT)
  v
Nginx LB
  |
  +--> app1 Spring Boot Resource Server
  +--> app2 Spring Boot Resource Server
```

## What Is Already Scaffolded

- Keycloak Docker service on `http://localhost:8090`.
- Backend `.env.example` has Keycloak admin variables.
- Frontend `.env.example` has Keycloak client variables.
- API is still public for now. Security will be enabled after you understand the Keycloak setup.

## Step 1: Start Keycloak

Run from the backend folder:

```powershell
docker compose up --build -d
```

Open:

```text
http://localhost:8090
```

Login:

```text
username: admin
password: admin
```

## Step 2: Create Realm

Do this manually in Keycloak UI because it teaches the core mental model.

Create a realm:

```text
order-service
```

Learning point:

- A realm is an isolated auth domain.
- Users, clients, roles, and tokens belong to a realm.

## Step 3: Create Frontend Client

Create client:

```text
Client ID: order-service-fe
Client type: OpenID Connect
Client authentication: Off
Standard flow: On
Direct access grants: Off
```

Set valid redirect URIs:

```text
http://localhost:5173/*
http://localhost:5174/*
```

Set valid post logout redirect URIs:

```text
http://localhost:5173/*
http://localhost:5174/*
```

Set web origins:

```text
http://localhost:5173
http://localhost:5174
```

Learning point:

- A React SPA is a public client.
- It must use Authorization Code + PKCE.
- It must not use a client secret in browser code.

## Step 4: Create Roles

Create realm roles:

```text
ADMIN
USER
```

Learning point:

- Roles become authorization data inside the JWT.
- Backend will map these roles into Spring Security authorities.

## Step 5: Create Users

Create:

```text
admin
user
```

Set passwords:

```text
admin / admin123
user / user123
```

Turn off temporary password.

Assign roles:

```text
admin -> ADMIN
user  -> USER
```

## Step 6: Inspect Token Claims

After frontend login is implemented, inspect the access token at:

```text
https://jwt.io
```

You should see:

```json
{
  "iss": "http://localhost:8090/realms/order-service",
  "azp": "order-service-fe",
  "realm_access": {
    "roles": ["USER"]
  }
}
```

Learning point:

- `iss` must match backend `issuer-uri`.
- `realm_access.roles` is where Keycloak realm roles live.
- Spring Security does not automatically map that claim, so we will write a converter.

## Step 7: Backend Security Implementation

This has been scaffolded in code. Read these files carefully:

```text
src/main/java/com/example/order_service/config/SecurityConfig.java
src/main/java/com/example/order_service/config/KeycloakRealmRoleConverter.java
src/main/java/com/example/order_service/controller/MeController.java
```

### What SecurityConfig Does

```text
1. Disables CSRF because this is a stateless Bearer-token API.
2. Enables CORS so the React dev server can call the API.
3. Sets session policy to STATELESS.
4. Defines which routes are public and which routes need roles.
5. Configures Spring as an OAuth2 Resource Server.
6. Uses KeycloakRealmRoleConverter to map Keycloak roles to Spring roles.
```

Target rules:

```text
GET    /api/v1/products                 authenticated
POST   /api/v1/products                 ADMIN
PUT    /api/v1/products/{id}            ADMIN
DELETE /api/v1/products/{id}            ADMIN

GET    /api/v1/orders                   USER or ADMIN
POST   /api/v1/orders                   USER or ADMIN
PUT    /api/v1/orders/{id}              USER or ADMIN
DELETE /api/v1/orders/{id}              USER or ADMIN

GET    /api/v1/inventory-reservations   ADMIN
DELETE /api/v1/inventory-reservations   ADMIN
```

### Why jwk-set-uri differs in Docker

The token issuer is:

```text
http://localhost:8090/realms/order-service
```

That value must match the JWT `iss` claim exactly because the browser logs in through `localhost`.

But app containers cannot call `localhost:8090` to reach Keycloak, because inside a container `localhost` means the app container itself. Therefore Docker uses:

```text
OAUTH2_ISSUER_URI=http://localhost:8090/realms/order-service
OAUTH2_JWK_SET_URI=http://keycloak:8080/realms/order-service/protocol/openid-connect/certs
```

Spring validates the token issuer against the first URL and fetches public signing keys through the second URL.

### What KeycloakRealmRoleConverter Does

Keycloak realm roles appear in JWT like this:

```json
{
  "realm_access": {
    "roles": ["ADMIN", "USER"]
  }
}
```

Spring Security expects role authorities like:

```text
ROLE_ADMIN
ROLE_USER
```

The converter bridges that difference.

### What /api/v1/me Does

After FE login is implemented, call:

```http
GET /api/v1/me
Authorization: Bearer <access_token>
```

It returns:

```json
{
  "subject": "...",
  "username": "admin",
  "authorities": ["ROLE_ADMIN"],
  "claims": {}
}
```

Use this endpoint whenever auth is confusing. It tells you what the backend actually sees from the JWT.

## Step 8: Frontend Auth Implementation

Important files to understand:

```text
src/features/auth/config/authConfig.ts
src/features/auth/providers/AuthProvider.tsx
src/features/auth/hooks/useAuth.ts
src/features/auth/components/RequireAuth.tsx
src/features/auth/components/RequireRole.tsx
```

Target behavior:

- Login redirects to Keycloak.
- Callback route stores the user session in memory/session storage.
- Axios attaches `Authorization: Bearer <access_token>`.
- UI hides admin actions for non-admin users.

## CV Description

Use this after implementation:

```text
Integrated OAuth2/OpenID Connect authentication using Keycloak, React Authorization Code Flow with PKCE, and Spring Boot Resource Server with JWT-based RBAC for ADMIN and USER permissions.
```
