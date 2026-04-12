# Auth-Service

## Architecture Overview

This Auth Service is responsible for **user registration** and **JWT token generation** only. All JWT validation, authentication, and authorization are handled by the **API Gateway**. This service operates with a permissive security configuration (`.anyExchange().permitAll()`) since the API Gateway enforces all security rules before requests reach this service.

**Key Responsibilities:**
- User registration (regular users and admins)
- User login and JWT token generation
- Password hashing with BCrypt
- User data storage in PostgreSQL

**NOT Responsible For:**
- JWT token validation (handled by API Gateway)
- Authentication enforcement (handled by API Gateway)
- Authorization/role-based access control (handled by API Gateway)

---

## Hardcoded Configuration Values (Should Use .env)

### AuthService (application.properties)
1. **JWT_SECRET** - Line 16: Hardcoded JWT signing key
   - Current value: "ksfjhfsklelkfjseajwlfiohiholdaiholdghjbkvhoawuqpieopwnmcadjaklvbnmbakjelhquiyllidhajkbcjkahgdhauhauiiohlioawhdvfjhkoksfjhfsklelkfjseajwlfiohiholdaiholdghjbkvhoawuqpieopwnmcadjaklvbnmbakjelhquiyllidhajkbcjkahgdhauhauiiohlioawhdvfjhkoksfjhfsklelkfjseajwlfioh"
   - Should be: `JWT_SECRET=${JWT_SECRET}` (read from environment variable)
   - Security risk: Exposed in version control

2. **JWT_EXPIRATION** - Line 18: Token expiration time
   - Current value: 86400000 (24 hours in milliseconds)
   - Should be: `JWT_EXPIRATION=${JWT_EXPIRATION:86400000}` (with default fallback)

3. **Database URL** - Line 6: PostgreSQL connection string
   - Current value: `r2dbc:postgresql://localhost:5432/AuthServiceDB`
   - Should be: `spring.r2dbc.url=${DB_URL:r2dbc:postgresql://localhost:5432/AuthServiceDB}`

4. **Database Username** - Line 7: PostgreSQL username
   - Current value: postgres
   - Should be: `spring.r2dbc.username=${DB_USERNAME:postgres}`

5. **Database Password** - Line 8: PostgreSQL password
   - Current value: 1234
   - Should be: `spring.r2dbc.password=${DB_PASSWORD}`

6. **Server Port** - Line 3: Application port
   - Current value: 8100
   - Should be: `server.port=${SERVER_PORT:8100}`
---

## Authentication & Authorization Flow

### Service Responsibilities

This Auth Service handles **only** the following operations:
- User registration (creating new users in database)
- User login (validating credentials and issuing JWT tokens)
- Password hashing with BCrypt

---

#### 1. User Registration Flow
- **Endpoint**: `POST /auth/user/register` (for regular users) or `POST /auth/admin/register` (for admins)
- **Security Configuration**: All endpoints permitted via `.anyExchange().permitAll()` in `SecurityConfig`
- **Process**:
  1. Request reaches `AuthController.register()` or `registerAdmin()` (no JWT filter active)
  2. Password is encoded using `BCryptPasswordEncoder`
  3. Role is set: `"ROLE_USER"` for regular users, `"ROLE_ADMIN"` for admins
  4. User entity is saved to PostgreSQL via R2DBC
  5. Returns saved User entity
- **Note**: Admin registration endpoint exists but should be protected by API Gateway to prevent unauthorized admin creation

#### 2. User Login Flow (Token Generation)
- **Endpoint**: `POST /auth/login`
- **Security Configuration**: Permitted via `.anyExchange().permitAll()` in `SecurityConfig`
- **Process**:
  1. Request reaches `AuthController.login()` (no JWT filter active)
  2. `UserRepo.findUserByUsername()` queries database reactively
  3. `BCryptPasswordEncoder.matches()` verifies password hash
  4. If valid, `JwtUtil.generateToken()` creates JWT with:
     - Subject: userId (as String)
     - Claims: username, role
     - IssuedAt: current timestamp
     - Expiration: current time + JWT_EXPIRATION
     - Signature: HMAC-SHA with JWT_SECRET
  5. Returns JWT string to client
- **Note**: This service only issues tokens. Token validation is done by API Gateway.

#### 3. JWT Token Structure
- **Subject**: User ID (as String) - primary identifier
- **Claims**:
  - `username`: User's username
  - `role`: User's role (e.g., "ROLE_USER", "ROLE_ADMIN")
- **Signature**: HMAC-SHA using JWT_SECRET
- **Expiration**: Configurable via JWT_EXPIRATION (default 24 hours)

#### 4. Security Configuration
- **Current State**: `.anyExchange().permitAll()` - all endpoints are open
- **Reason**: API Gateway handles all security enforcement
- **JwtFilter**: Present in codebase but **not active** (commented out in SecurityConfig)
- **CustomUserDetails**: Available for use by API Gateway if needed for token parsing

---

#### How Registration Works
1. **User Signs Up**: A new user sends their details (name, username, password) to the registration endpoint
2. **Password Protection**: The system immediately scrambles (hashes) the password so it's never stored as plain text
3. **Role Assignment**:
   - Regular users get "ROLE_USER" role
   - Admin registration endpoint gives "ROLE_ADMIN" role (should be protected by API Gateway)
4. **Database Storage**: User information is saved in PostgreSQL database
5. **Response**: System confirms the user was created successfully

#### How Login Works (Token Issuance)
1. **User Submits Credentials**: User sends username and password to login endpoint
2. **Database Lookup**: System finds the user in the database
3. **Password Check**: System compares the submitted password with the stored hashed password
4. **Token Generation**: If password matches, system creates a special digital pass (JWT token) that contains:
   - User ID (as the main identifier/subject)
   - Username (stored in the token's data section)
   - Role (what permissions they have)
   - Expiration time (when the pass expires)
5. **Token Signing**: The token is digitally signed with a secret key so no one can forge it
6. **Token Return**: System gives this token back to the user

#### What Happens After Login
Once the user has a JWT token:
- The user includes this token in all subsequent requests
- The **API Gateway** validates the token (checks signature, expiration, etc.)
- The **API Gateway** extracts user information from the token
- The **API Gateway** decides if the user can access the requested resource
- The **API Gateway** forwards the request to downstream services only if authorized

#### JWT Token Structure
The JWT token issued by this service contains:
- **User ID**: The primary identifier (stored as the token's subject)
- **Username**: The user's login name (stored in token data)
- **Role**: The user's permissions (e.g., "ROLE_USER", "ROLE_ADMIN")
- **Expiration**: When the token expires (default 24 hours)

#### Security Architecture
- **Auth Service**: Open endpoints (`.anyExchange().permitAll()`) - issues tokens only
- **API Gateway**: Enforces all security rules - validates tokens and checks permissions
- **Downstream Services**: Receive only pre-validated requests from API Gateway

---

## Dependencies
- **Spring web flux**
- **Spring Security** for authentication and authorization
- **JJWT (api, impl, jackson)** for stateless authentication
- **R2DBC** for reactive database access
- **PostgreSQL** for user data storage
- **Lombok** for reducing boilerplate code