package com.cyberronin.apigatewayservice.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/*
For public end points (not valideded using JWT)
Takes the IP of machine which made the request
Ip load balancer is placed then take the IP of the user from the header placed by the lb
To stop header spoofing, the load balancer must first strip the header from incoming request and then add its own header

For endpoints validated using JWT
Take the X-User-Id header from the request which is added if the JWT is valid by auth filter
and this for rate limiting
*/

@Configuration
public class RateLimiterConfig {

    @Bean
    @Primary
    public KeyResolver perIPKeyResolver() {
        return exchange -> {
            // Try to get IP from X-Forwarded-For header first
            String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");

            String ip;
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                ip = xForwardedFor.split(",")[0].trim();
            } else {
                ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            }
            return Mono.just(ip);
        };
    }

    @Bean
    public KeyResolver perJWTKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest()
                    .getHeaders()
                    .getFirst("X-User-Id");

            if (userId == null || userId.isBlank()) {
                return Mono.error(
                        new IllegalStateException(
                                "X-User-Id missing before JWT rate limiting"
                        )
                );
            }

            return Mono.just("USER:" + userId);
        };
    }

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(5, 5);
    }
}