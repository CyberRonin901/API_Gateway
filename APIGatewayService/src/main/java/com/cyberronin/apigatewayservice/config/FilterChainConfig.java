package com.cyberronin.apigatewayservice.config;

import com.cyberronin.apigatewayservice.filter.AuthenticationFilter;
import com.cyberronin.apigatewayservice.filter.RoleFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.time.Duration;

@Configuration
public class FilterChainConfig {

    private final AuthenticationFilter authFilter;
    private final RoleFilter roleFilter;
    private final RedisRateLimiter redisRateLimiter;
    private final KeyResolver perJWTKeyResolver;
    private final KeyResolver perIPKeyResolver;

    public FilterChainConfig(AuthenticationFilter authFilter,
                             RoleFilter roleFilter,
                             RedisRateLimiter redisRateLimiter,

                             @Qualifier("perJWTKeyResolver")
                             KeyResolver perJWTKeyResolver,

                             @Qualifier("perIPKeyResolver")
                             KeyResolver perIPKeyResolver) {
        this.authFilter = authFilter;
        this.roleFilter = roleFilter;
        this.redisRateLimiter = redisRateLimiter;
        this.perJWTKeyResolver = perJWTKeyResolver;
        this.perIPKeyResolver = perIPKeyResolver;
    }

    @Bean
    public RouteLocator serviceA_Route(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("SERVICE-A", r -> r
                        .path("/demo/**")
                        .filters(f -> f
                                // Rewrite Path to strip /demo and convert ex. /demo/hello to /hello and send to Service-A
                                .rewritePath("/demo/(?<segment>.*)", "/${segment}")

//                                 Authentication
                                .filter(authFilter.apply(new AuthenticationFilter.Config()))
//                                 Authorization
                                .filter(roleFilter.apply(config -> config.setRequiredRole("ROLE_USER")))

//                                 Rate Limiting
//                                .requestRateLimiter(config -> config
//                                        .setRateLimiter(redisRateLimiter)
//                                        .setKeyResolver(perJWTKeyResolver))

                                // Retry (exponential retry + jitter)
                                .retry(retryConfig -> retryConfig
                                        .setRetries(3)
                                        .setMethods(HttpMethod.GET, HttpMethod.PUT)
                                        .setStatuses(HttpStatus.BAD_GATEWAY, HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.GATEWAY_TIMEOUT)
                                        // param order: firstBackoff, maxBackoff, factor, basedOnPreviousDelay
                                        .setBackoff(Duration.ofMillis(100), Duration.ofSeconds(1), 2, true)
                                )

                                // Circuit Breaker
                                .circuitBreaker(cbConfig -> cbConfig
                                        .setName("CIRCUIT-BREAKER")
                                        .setFallbackUri("forward:/fallback/message"))
                        )
                        .uri("lb://SERVICE-A")
                )
                .build();
    }
}