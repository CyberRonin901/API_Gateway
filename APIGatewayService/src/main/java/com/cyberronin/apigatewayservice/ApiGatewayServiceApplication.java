package com.cyberronin.apigatewayservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayServiceApplication
{
    private static final Logger logger = LoggerFactory.getLogger(ApiGatewayServiceApplication.class);

    {
        logger.info("Gateway Application is starting.....");
    }

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayServiceApplication.class, args);
        logger.info("Gateway Application is started !!");
    }
}