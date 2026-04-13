package com.cyberronin.apigatewayservice.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/*
Takes the IP of machine which made the request
If a load-balancer is btw the user and the API gateway,
in that case it will not work as IP of the load-balancer will bw passed
*/

@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver userKeyResolver() {
        // Limits based on the client's IP address
        return exchange -> Mono.just(
                exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
        );
    }

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        // replenishRate: 3, burstCapacity: 3
        return new RedisRateLimiter(3, 3);
    }
}