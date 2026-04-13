### Things working / Done
- Routing (dynamic)
- Auth
- Rate limiting
- Logging
- Monitoring using actuator
- Circuit breaking
- Retry pattern

### TODO:
- Dockerize compose
- Unit tests

- Medium Priority:
  - Implement error handling
  - Add integration tests for auth flow
- Low Priority: 
  - Add performance benchmarks (JMeter)
  - Add load testing results

### Architecture Diagrams

![API gateway](../Diagrams/API%20gateway.svg)

![API gateway internal processing order](../Diagrams/API%20gateway%20internal%20processing%20order.svg)

![Horizontal scaling](../Diagrams/Horizontal%20scaling.svg)

![Filter chain](../Diagrams/Filter%20chain.svg)

![Auth filter](../Diagrams/Auth%20filter.svg)

![Role filter](../Diagrams/Role%20filter.svg)

![Auth Service](../Diagrams/Auth%20Service.svg)

![Auth service user reg](../Diagrams/Auth%20Service%20user%20register.svg)

![Auth service admin reg](../Diagrams/Auth%20Service%20admin%20register.svg)

![Auth service login](../Diagrams/Auth%20Service%20login.svg)