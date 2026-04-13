package com.cyberronin.servicea.util;

import com.cyberronin.servicea.feign.ServiceBInterface;
import org.springframework.stereotype.Component;

@Component
public class ServiceBFallback implements ServiceBInterface {
    @Override
    public String getData() {
        // Return a default value, a cached value, or a friendly message
        return "Service B is currently down. Returning default system data.";
    }
}