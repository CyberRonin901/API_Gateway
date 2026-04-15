package com.cyberronin.apigatewayservice.config;

import com.cyberronin.apigatewayservice.filter.AuthenticationFilter;
import com.cyberronin.apigatewayservice.filter.RoleFilter;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
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
public class GatewayResilienceConfig {

    // custom filters beans to make the filter chain
    private final AuthenticationFilter authFilter;
    private final RoleFilter roleFilter;
    private final RedisRateLimiter redisRateLimiter;
    private final KeyResolver userKeyResolver;

    public GatewayResilienceConfig(AuthenticationFilter authFilter,
                                   RoleFilter roleFilter,
                                   RedisRateLimiter redisRateLimiter,
                                   KeyResolver userKeyResolver) {
        this.authFilter = authFilter;
        this.roleFilter = roleFilter;
        this.redisRateLimiter = redisRateLimiter;
        this.userKeyResolver = userKeyResolver;
    }

    @Bean
    public RouteLocator serviceA_Route(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("service-A", r -> r
                        .path("/demo/**")
                        .filters(f -> f
                                // Rewrite Path to strip /demo and convert /demo/hello to /hello and send to Service-A
                                .rewritePath("/demo/(?<segment>.*)", "/${segment}")

                                // Rate Limiting
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(redisRateLimiter)
                                        .setKeyResolver(userKeyResolver))

                                // Authentication
                                .filter(authFilter.apply(new AuthenticationFilter.Config()))
                                // Authorization
                                .filter(roleFilter.apply(config -> config.setRequiredRole("ROLE_USER")))

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
                                        .setName("SERVICE-A")
                                        .setFallbackUri("forward:/fallback/message"))
                        )
                        .uri("lb://SERVICE-A"))
                .build();
    }

    /**
     This method defines how Circuit Breaker works
     It creates a Customizer bean that Spring Cloud Gateway looks for when
     it initializes any Circuit Breaker filter by name.
    */
    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configure(builder -> builder
                        // --- CIRCUIT BREAKER SETTINGS ---
                        .circuitBreakerConfig(CircuitBreakerConfig.custom()
                                .slidingWindowSize(10) // Look at the last 10 requests to calculate failure rate
                                .failureRateThreshold(50) // If 5 out of 10 (50%) fail, trip the circuit to OPEN
                                .waitDurationInOpenState(Duration.ofSeconds(5)) // Stay in OPEN state for 5s before trying again
                                .permittedNumberOfCallsInHalfOpenState(3) // When testing recovery, allow 3 calls through
                                .build())
                        // --- TIMEOUT SETTINGS ---
                        .timeLimiterConfig(TimeLimiterConfig.custom()
                                .timeoutDuration(Duration.ofSeconds(2)) // If the backend takes > 2s, kill the request
                                .build())
                        .build(),
                "SERVICE-A" // This name must match the .setName() in RouteLocator
        );
    }
}