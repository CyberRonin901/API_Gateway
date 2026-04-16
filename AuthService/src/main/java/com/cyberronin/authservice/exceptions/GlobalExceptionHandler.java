package com.cyberronin.authservice.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler
{
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public Mono<ResponseEntity<Object>> handleUsernameExists(UsernameAlreadyExistsException ex, ServerWebExchange exchange)
    {
        return createProblemDetail(ex, HttpStatus.CONFLICT, "Conflict", ex.getMessage(), exchange);
    }

    @Override
    protected Mono<ResponseEntity<Object>> handleWebExchangeBindException(
            WebExchangeBindException ex, HttpHeaders headers, HttpStatusCode status, ServerWebExchange exchange) {

        // Extract field names and their corresponding error messages
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Invalid value"
                ));

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, "Validation failed for one or more fields.");
        problemDetail.setTitle("Validation Error");
        problemDetail.setInstance(exchange.getRequest().getURI());
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("errors", errors); // Injects the field errors into the response

        return Mono.just(ResponseEntity.status(status).body(problemDetail));
    }

    private Mono<ResponseEntity<Object>> createProblemDetail(Exception ex, HttpStatus status, String title, String detail, ServerWebExchange exchange) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setInstance(exchange.getRequest().getURI());
        problemDetail.setProperty("timestamp", Instant.now());

        return Mono.just(ResponseEntity.status(status).body(problemDetail));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Object>> handleAllUncaughtException(Exception ex, ServerWebExchange exchange)
    {
        logger.error("Unexpected error: ", ex);

        return createProblemDetail(
                ex,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred.",
                exchange
        );
    }
}