<h1 align="center">Spring Boot Gateway with Spring Cloud and WebFlux</h1>

## Project Overview

This API Gateway Service is a Spring Cloud Gateway-based microservice that acts as a single entry point 
for all client requests, providing essential features such as authentication, authorization, rate limiting, 
circuit breaking, and routing. 
Built with Spring Boot 4.0.5, Spring Cloud Gateway (WebFlux), and integrated with Eureka Service Discovery, 
Redis for distributed rate limiting, and Resilience4j for circuit breaking patterns.

![API gateway](./Diagrams/API%20gateway.svg)

![Horizontal scaling](./Diagrams/Horizontal%20scaling.svg)

## Tech Stack

- **Java**: 25
- **Spring Boot**: 4.0.5
- **Spring Cloud**
- **Spring Cloud Gateway**: WebFlux-based reactive gateway
- **Spring Security**
- **Spring Cloud Netflix Eureka**: Service discovery
- **Resilience4j**: Circuit breaker and retry patterns
- **Redis**: Distributed rate limiting and caching
- **JWT (jjwt)**: Token-based authentication (api, impl, jackson)
- **Lombok**: Boilerplate code reduction
- **Postgres** for storing user data

## Features
- **API request dynamic routing** (using Eureka service discovery)
- **API rate limiting per IP**
- **Authentication**
- **Authorization**
- **Upstream and Downstream API details logging**
- **Circuit Breaking and Resilience Patternsry** (exponential retry + jitter)
- **Monitoring & Actuator**
- **Error handling**

## Quick Start (Docker)

**Prerequisites:** Docker and Docker Compose installed

**Start the project:**
```bash
cp .env.example .env
# Edit .env with your secrets (optional)
docker-compose up --build
```

**Access points:**
- API Gateway: http://localhost:8080
- Only for testing (should not be exposed in deployment)
    - Eureka Dashboard: http://localhost:8761
    - Service A: http://localhost:8081
    - Service B: http://localhost:8082

**Stop the project:**
```bash
docker-compose down
```

## Core Functionality

#### API gateway request to response:
    User 
      ↓
    API Gateway [ 
        Netty Server
            ↓
        HttpWebHandlerAdapter
            ↓
        RoutePredicateHandlerMapping (Matches lb://SERVICE)
            ↓
        FilteringWebHandler (Assembles Filter Chain)
            ↓
        Pre-Filters (Auth, Rate Limiting, RewritePath)
            ↓
        LoadBalancerClientFilter (Eureak Service Discovery)
            ↓
        NettyRoutingFilter (Bridge btw API gateway and Microservices)
    ]
      ↓
    Microservice
      ↓
    API Gateway [Post-Filters ]
      ↓
    User

![API gateway internal processing order](./Diagrams/API%20gateway%20internal%20processing%20order.svg)

#### Fiters
    Global Logging Filter
        ↓
    Rate Limiting Filter
        ↓
    Auth Filter
        ↓
    Role Filter
        ↓
      Retry
        ↓
    Circuit Breaker

![Filter chain](./Diagrams/Filter%20chain.svg)

### Dynamic Routing
The gateway routes incoming requests to appropriate backend microservices using Eureka Service Discovery. 
Routes are configured both via YAML (`application.yaml`) and Java-based configuration (`GatewayResilienceConfig.java`). 
The gateway supports:
- **Path-based routing**: Routes requests based on URL patterns (e.g., `/demo/**` to Service-A, `/auth/**` to Auth Service)
- **Load balancing**: Uses `lb://SERVICE-NAME` syntax to distribute requests across service instances
- **Path rewriting**: Strips prefix paths before forwarding to downstream services (e.g., `/demo/hello` → `/hello`)

### Authentication & Authorization
The gateway implements JWT-based authentication and role-based authorization at the gateway level:

**AuthenticationFilter** (`AuthenticationFilter.java`):
- Intercepts requests to secured endpoints (all except public endpoints defined in `RouteValidator`)
- Validates JWT tokens using the `JwtUtil` class
- Extracts user claims (userId, username, role) from valid tokens
- Adds verified user information as HTTP headers (`X-User-Id`, `X-User-Name`, `X-User-Role`) for downstream services
- **Removes spoofed headers to prevent header injection attacks**
- Returns 401 UNAUTHORIZED for missing/invalid tokens

**RouteValidator** (`RouteValidator.java`):
- Defines public endpoints that bypass authentication: `/auth/user/register`, `/auth/login`, `/eureka`
- Uses a predicate to determine if a request requires authentication

**RoleFilter** (`RoleFilter.java`):
- Enforces role-based access control after authentication
- Checks if the user's role (from `X-User-Role` header) matches the required role
- Grants access if user has the required role OR if user is `ROLE_ADMIN` (admin override)
- Returns 403 FORBIDDEN for unauthorized access

**JwtUtil** (`JwtUtil.java`):
- Validates JWT tokens using HMAC-SHA256 signing
- Extracts claims: username, role, userId (subject)
- Logs validation errors for debugging

![Auth filter](./Diagrams/Auth%20filter.svg)

![Role filter](./Diagrams/Role%20filter.svg)

![Auth Service](./Diagrams/Auth%20Service.svg)

![Auth service user reg](./Diagrams/Auth%20Service%20user%20register.svg)

![Auth service admin reg](./Diagrams/Auth%20Service%20admin%20register.svg)

![Auth service login](./Diagrams/Auth%20Service%20login.svg)


### Rate Limiting
The gateway implements distributed rate limiting per IP using Redis and the Token Bucket algorithm:

**RateLimiterConfig** (`RateLimiterConfig.java`):
- **KeyResolver**: Extracts client IP address for rate limiting key
  - First checks `X-Forwarded-For` header (for load balancer scenarios)
  - Falls back to direct remote address
  - Prevents header spoofing by taking the first IP in the chain
- **RedisRateLimiter**: Configured with replenishRate=3, burstCapacity=3
  - Allows 3 requests per second per IP
  - Supports burst traffic up to 3 requests
  - Uses Redis Lua script for atomic token bucket operations

#### Rate Limiting Flow
    Key Resolver (Rate Limit config)
        ↓
    RequestRateLimiterGatewayFilterFactory (builtin Spring cloud)
    (takes the key and the limit and send the command to redis)
        ↓
    Redis (it runs a Lua script that implements the Token Bucket algorithm, token = 0 => false, else decrement token)
        ↓
    Other Filters

1. KeyResolver extracts client IP
2. RequestRateLimiterGatewayFilterFactory checks Redis for available tokens
3. Redis Lua script decrements token count if available
4. Request proceeds if tokens > 0, otherwise returns 429 TOO_MANY_REQUESTS

### Circuit Breaking & Resilience Patterns
The gateway implements circuit breaking and retry patterns using Resilience4j:

**GatewayResilienceConfig** (`GatewayResilienceConfig.java`):
- **Circuit Breaker Configuration** (for Service-A route):
  - Sliding window size: 10 requests
  - Failure rate threshold: 50% (5 out of 10 failures trips the circuit)
  - Wait duration in OPEN state: 5 seconds before attempting recovery
  - Permitted calls in HALF_OPEN state: 3 calls to test recovery
  - Timeout duration: 2 seconds (kills requests taking longer than 2s)
- **Retry Configuration** (for Service-A route):
  - Max retries: 3
  - Retryable methods: GET, PUT
  - Retryable statuses: BAD_GATEWAY, SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT
  - Exponential backoff with jitter: starts at 100ms, max 1s, factor 2

**FallbackController** (`FallbackController.java`):
- Provides fallback endpoint `/fallback/message` for circuit breaker failures
- Returns 503 SERVICE_UNAVAILABLE with user-friendly message
- Triggered when circuit is OPEN and requests are being rejected

### Global Logging & Error Handling
**GlobalLoggingFilter** (`GlobalLoggingFilter.java`):
- Runs with HIGHEST_PRECEDENCE order (executes before all other filters)
- Logs request start: method and path
- Logs response end: status code and target service
- Logs downstream service errors via `X-Service-Error` header
- Catches gateway-level exceptions and returns structured JSON error responses
- Prevents response conflicts by checking if response is already committed
- Returns 500 INTERNAL_SERVER_ERROR with `{"error": "Gateway Error", "details": "message"}` on failures

### Service Discovery Integration
- Uses Netflix Eureka for service discovery
- Enabled via `@EnableDiscoveryClient` annotation
- Services register with Eureka and gateway resolves them dynamically
- Supports both custom routing (YAML/Java config) and discovery locator (commented out)

### Monitoring & Actuator
- Spring Boot Actuator integration for health checks and monitoring
- Circuit breaker health monitoring enabled (`management.health.circuitbreakers.enabled=true`)
- Provides metrics for gateway operations, rate limiting, and circuit breaker states

## Security Features

- JWT token validation at gateway edge
- Header spoofing prevention (removes and re-adds user headers)
- Role-based access control with admin override
- IP-based rate limiting to prevent abuse
- Public endpoint whitelisting for login/register
- Circuit breaking to prevent cascade failures
- Request timeout protection (2s limit)

## Configuration

- **application.yaml**: Route definitions, service discovery settings, actuator configuration
- **application.properties**: JWT secret, Redis connection details (host, port, password, username)
- **GatewayResilienceConfig.java**: Java-based route configuration for Service-A with resilience patterns
- **RateLimiterConfig.java**: Redis rate limiter and IP key resolver beans

## Error Handling

The service implements error handling at multiple levels:
- **Gateway level**: GlobalLoggingFilter catches all gateway exceptions and returns structured JSON
- **Authentication level**: AuthenticationFilter returns 401 for auth failures
- **Authorization level**: RoleFilter returns 403 for insufficient permissions
- **Circuit breaker**: FallbackController returns 503 when services are unavailable
- **JWT validation**: JwtUtil catches parsing exceptions and returns false

## Development Notes

- Rate limiting is distributed using Redis for horizontal scalability
- Circuit breaker and retry patterns are applied selectively per route
- API gateway has its circuit braker for direct microservice calls and Service A has its own for calling Service B
- Service discovery is dynamic, allowing services to be added/removed without gateway restart
- Logging is comprehensive at the gateway level for debugging and monitoring