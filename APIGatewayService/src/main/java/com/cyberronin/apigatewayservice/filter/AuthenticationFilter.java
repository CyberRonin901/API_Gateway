package com.cyberronin.apigatewayservice.filter;

import com.cyberronin.apigatewayservice.util.JwtUtil;
import com.cyberronin.apigatewayservice.util.RouteValidator;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

// TODO: add order for the filter chain
@Component
@Order(1)
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final JwtUtil jwtUtil;
    private final RouteValidator validator;

    public AuthenticationFilter(JwtUtil jwtUtil, RouteValidator validator) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
        this.validator = validator;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // If route is public (Login/Register), bypass filter
            if (validator.isSecured.test(request)) {

                // Check for Authorization header
                if (!request.getHeaders().containsHeader(HttpHeaders.AUTHORIZATION)) {
                    return onError(exchange, "Missing Authorization Header", HttpStatus.UNAUTHORIZED);
                }

                String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    return onError(exchange, "Invalid Header Format", HttpStatus.UNAUTHORIZED);
                }

                String token = authHeader.substring(7);

                try {
                    // Validate Token
                    if (!jwtUtil.validateToken(token)) {
                        return onError(exchange, "Invalid Token", HttpStatus.UNAUTHORIZED);
                    }

                    // Extract info & mutate request for downstream services
                    // so other services can use Username and UserId and Role
                    String userId = jwtUtil.extractUserId(token);
                    String username = jwtUtil.extractUsername(token);
                    String role = jwtUtil.extractRole(token);

                    ServerHttpRequest mutatedRequest = request.mutate()
                            .headers(httpHeaders -> {
                                // REMOVE spoofed headers here (security feature)
                                httpHeaders.remove("X-User-Id");
                                httpHeaders.remove("X-User-Name");
                                httpHeaders.remove("X-User-Role");
                            })
                            // ADD verified headers here
                            .header("X-User-Id", userId)
                            .header("X-User-Name", username)
                            .header("X-User-Role", role)
                            .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());

                } catch (Exception e) {
                    return onError(exchange, "Token Validation Failed", HttpStatus.UNAUTHORIZED);
                }
            }
            return chain.filter(exchange);
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    public static class Config {}
}