### Things working / Done
- Routing (Custom)

### TODO:
- Make a new Auth Service that generate a JWT

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
    ]
      ↓
    NettyRoutingFilter (Bridge btw API gateway and Microservices)
      ↓
    Microservice
      ↓
    API Gateway [Post-Filters ]
      ↓
    User