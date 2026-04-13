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
import java.util.HashMap;
import java.util.Map;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(GlobalLoggingFilter.class);
    private final ObjectMapper objectMapper;

    public GlobalLoggingFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // LOG REQUEST START
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();
        logger.info(">>> START: {} {}", method, path);

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    // LOG RESPONSE END
                    HttpStatusCode status = exchange.getResponse().getStatusCode();

                    if (status == HttpStatus.TOO_MANY_REQUESTS) {
                        String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
                        logger.warn("!!! RATE LIMIT EXCEEDED | IP: {} | Path: {}", ip, path);
                    }

                    URI target = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
                    logger.info("Target details: {}", target);

                    // Simple logic: If we have a target, show it. Otherwise, it failed early.
                    String targetInfo = (target != null) ? target.getHost() + target.getPath() : "NONE";
                    logger.info("<<< END: {} | Target: {}", status, targetInfo);

                    // LOG DOWNSTREAM TRACE (If Service A/B sent an error header)
                    String errorHeader = exchange.getResponse().getHeaders().getFirst("X-Service-Error");
                    if (errorHeader != null) {
                        logger.error("!!! MICROSERVICE ERROR: {}", errorHeader);
                    }
                })
                .onErrorResume(ex -> handleGatewayError(exchange, ex));
    }

    private Mono<Void> handleGatewayError(ServerWebExchange exchange, Throwable ex) {
        // LOG THE ERROR
        // This is the only place we print the big stack trace
        logger.error("!!! GATEWAY ERROR: {}", ex.getMessage());

        var response = exchange.getResponse();

        // [5] CONFLICT PREVENTION
        // If the response is already committed (headers sent), we CANNOT write JSON.
        // This stops the "UnsupportedOperationException" you were seeing.
        if (response.isCommitted()) {
            return Mono.empty();
//            return Mono.error(ex);
        }

        // PREPARE ERROR RESPONSE
        try {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("error", "Gateway Error");
            body.put("details", ex.getMessage());

            byte[] bytes = objectMapper.writeValueAsBytes(body);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
        } catch (Exception e) {
            // If anything fails here (like headers being read-only), just finish the request
            return response.setComplete();
        }
    }

    @Override
    public int getOrder() {
        // Runs before all other filters
        return Ordered.HIGHEST_PRECEDENCE;
    }
}