# Auth-Service

## Architecture Overview

This Auth Service is responsible for **user registration** and **JWT token generation** only. 
All JWT validation, authentication, and authorization are handled by the **API Gateway**. 
This service operates with a permissive security configuration (`.anyExchange().permitAll()`) 
since the API Gateway enforces all security rules before requests reach this service.

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

## JWT payload Structure
- Sub: UserId as string
- Username
- Role: ROLE_USER or ROLE_ADMIN
- IssuedAt: current timestamp
- Expiration: current time + JWT_EXPIRATION

## Authentication & Authorization Flow

#### 1. User Registration Flow (Create new user and store in DB)
- **Endpoint**: `POST /auth/user/register` (for regular users) or `POST /auth/admin/register` (for admins)
- **Process**:
  1. Request reaches `AuthController.register()` or `registerAdmin()`
  2. Password is encoded using `BCryptPasswordEncoder`
  3. Role is set: `"ROLE_USER"` for regular users, `"ROLE_ADMIN"` for admins
  4. User entity is saved to PostgreSQL via R2DBC
  5. Returns saved User entity
- **Note**: Admin registration endpoint should be protected by API Gateway to prevent unauthorized admin creation

#### 2. User Login Flow (Token Generation)
- **Endpoint**: `POST /auth/login`
- **Process**:
  1. Request reaches `AuthController.login()`
  2. `UserRepo.findUserByUsername()` queries database reactively
  3. `BCryptPasswordEncoder.matches()` verifies password hash
  4. If valid, `JwtUtil.generateToken()` creates JWT
  5. Returns JWT string to client
- **Note**: only issues tokens. Token validation is done by API Gateway.

---

#### How Registration Works
1. **User Signs Up**: A new user sends their details (name, username, password) to the registration endpoint
2. **Password Protection**: Th
3. **Role Assignment**:
   - Regular users get "ROLE_USER" role
   - Admin registration endpoint gives "ROLE_ADMIN" role 
4. **Database Storage**: User information is saved in PostgreSQL database
5. **Response**: System confirms the user was created successfully

#### How Login Works (Token Issuance)
1. **User Submits Credentials**: User sends username and password to login endpoint
2. **Database Lookup**: System finds the user in the database
3. **Password Check**: System compares the submitted password with the stored hashed password
4. **Token Generation**: If password matches, system creates JWT token
5. **Token Signing**: The token is digitally signed with a secret key so no one can forge it
6. **Token Return**: System gives this token back to the user

#### At API gateway
Once the user has a JWT token:
- The user includes this token in all subsequent requests
- The **API Gateway** validates the token (checks signature, expiration, etc.)
- The **API Gateway** extracts user information from the token
- The **API Gateway** decides if the user can access the requested resource
- The **API Gateway** forwards the request to downstream services only if authorized

---

## Dependencies
- **Spring web flux**
- **Spring Security** for authentication and authorization
- **JJWT (api, impl, jackson)** for stateless authentication
- **R2DBC** for reactive database access
- **PostgreSQL** for user data storage
- **Lombok** for reducing boilerplate code