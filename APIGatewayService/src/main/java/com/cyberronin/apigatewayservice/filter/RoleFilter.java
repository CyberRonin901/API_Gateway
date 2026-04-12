package com.cyberronin.apigatewayservice.filter;

import com.cyberronin.apigatewayservice.util.RouteValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;


@Component
public class RoleFilter extends AbstractGatewayFilterFactory<RoleFilter.Config>
{
    private static final Logger logger = LoggerFactory.getLogger(RoleFilter.class);

    private final RouteValidator validator;

    public RoleFilter(RouteValidator validator) {
        super(Config.class);
        this.validator = validator;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            // If route is public (Login/Register), bypass filter
            if (validator.isSecured.test(request)) {

                // Grab the role from the header
                String userRole = exchange.getRequest().getHeaders().getFirst("X-User-Role");

                String required = config.getRequiredRole();

                // Allow if user_role = required role or if use_role == ROLE_ADMIN
                boolean isAuthorized = userRole != null && (
                        userRole.equalsIgnoreCase(required) || userRole.equalsIgnoreCase("ROLE_ADMIN")
                );

                if (!isAuthorized) {
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    logger.info("User is Unauthorized");
                    return exchange.getResponse().setComplete();
                }
                logger.info("User is Authorized");
            }
            return chain.filter(exchange);
        };
    }

    public static class Config {
        private String requiredRole;
        public String getRequiredRole() { return requiredRole; }
        public void setRequiredRole(String requiredRole) { this.requiredRole = requiredRole; }
    }
}