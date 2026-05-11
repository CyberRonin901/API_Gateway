package com.cyberronin.apigatewayservice.config;

import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CircuitBreakerConfig {
    /**
     This method defines how Circuit Breaker works
     It creates a Customizer bean that Spring Cloud Gateway looks for when
     it initializes any Circuit Breaker filter by name.
     */
    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configure(builder -> builder
                        .circuitBreakerConfig(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                                .slidingWindowSize(100) // Look at the last 100 requests to calculate failure rate
                                .failureRateThreshold(80) // If 8 out of 10 (880%) fail, trip the circuit to OPEN
                                .waitDurationInOpenState(Duration.ofSeconds(2)) // Stay in OPEN state for 5s before trying again
                                .permittedNumberOfCallsInHalfOpenState(10) // When testing recovery, allow 10 calls through
                                .build())
                        // timeout config
                        .timeLimiterConfig(TimeLimiterConfig.custom()
                                .timeoutDuration(Duration.ofSeconds(2)) // If the backend takes > 2s, kill the request and return error
                                .build())
                        .build(),
                "CIRCUIT-BREAKER" // This name must match the .setName() in RouteLocator
        );
    }
}
