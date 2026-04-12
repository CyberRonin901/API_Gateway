### Things working / Done
- Routing (Custom)
- Auth
- Rate limiting

### TODO:
- Circuit breaking

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
    Rate Limiting Filter
        ↓
    Auth Filter
        ↓
    Role Filter

### Rate Limiting
    Key Resolver (Rate Limit config)
        ↓
    RequestRateLimiterGatewayFilterFactory (builtin Spring cloud)
    (takes the key and the limit and send the command to redis)
        ↓
    Redis (it runs a Lua script that implements the Token Bucket algorithm, token = 0 => false, else decrement token)
        ↓
    Other Filters