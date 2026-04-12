package com.cyberronin.apigatewayservice.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

@Component
@Order(1)
public class GlobalLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(GlobalLoggingFilter.class);
    private final ObjectMapper objectMapper;

    public GlobalLoggingFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // UPSTREAM LOGGING (Request coming from Client)
        URI upstreamUri = exchange.getRequest().getURI();
        String method = exchange.getRequest().getMethod().name();

        logger.info(">>> UPSTREAM: {} {}", method, upstreamUri.getPath());

        return chain.filter(exchange)
                .then(Mono.fromRunnable(() -> {
                    // DOWNSTREAM LOGGING (Request sent to Microservice)
                    // This attribute is populated by the routing filter after predicates match
                    URI downstreamUri = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
                    HttpStatusCode statusCode = exchange.getResponse().getStatusCode();

                    if (downstreamUri != null) {
                        logger.info("<<< DOWNSTREAM: {} | Target: {}{}",
                                statusCode, downstreamUri.getHost(), downstreamUri.getPath());
                    } else {
                        logger.warn("<<< COMPLETED: {} | No downstream target found", statusCode);
                    }
                }))
                .onErrorResume(ex -> handleGatewayError(exchange, ex)).then();
    }

    private Mono<Void> handleGatewayError(ServerWebExchange exchange, Throwable ex) {
        logger.error("GATEWAY FAILURE: {}", ex.getMessage());

        var response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // Error Response
        var errorBody = java.util.Map.of(
                "code", "GW-500",
                "message", "An unexpected gateway error occurred"
        );

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorBody);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
        } catch (Exception jsonEx) {
            return response.setComplete();
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}