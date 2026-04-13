package com.cyberronin.apigatewayservice.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/*
Takes the IP of machine which made the request
Ip load balancer is placed then take the IP of the user from the header placed by the lb
To stop header spoofing, the load balancer must first strip the header from incoming request and then add its own header
*/

@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // Try to get IP from X-Forwarded-For header first
            String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");

            String ip;
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                // The first IP in the list is the original client
                ip = xForwardedFor.split(",")[0].trim();
            } else {
                // Fallback to direct remote address
                ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            }
            return Mono.just(ip);
        };
    }

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        // replenishRate: 3, burstCapacity: 3
        return new RedisRateLimiter(3, 3);
    }
}