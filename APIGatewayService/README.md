### Things working / Done
- Routing (dynamic)
- Auth
- Rate limiting
- Logging
- Monitoring using actuator
- Circuit breaking
- Retry pattern

### TODO:
- Create root README with architecture overview and setup guide
- Dockerize compose
- Unit tests

- Medium Priority:
  - Implement error handling
  - Add integration tests for auth flow
- Low Priority: 
  - Add performance benchmarks (JMeter)
  - Add load testing results

API gatway has its own circuit breaker, and Service A has its own for retrying and circuit breaking Service B

### API gateway request to response:
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


### Fiters
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

### Rate Limiting
    Key Resolver (Rate Limit config)
        ↓
    RequestRateLimiterGatewayFilterFactory (builtin Spring cloud)
    (takes the key and the limit and send the command to redis)
        ↓
    Redis (it runs a Lua script that implements the Token Bucket algorithm, token = 0 => false, else decrement token)
        ↓
    Other Filters