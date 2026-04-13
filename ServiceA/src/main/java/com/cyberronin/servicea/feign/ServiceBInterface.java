package com.cyberronin.servicea.feign;

import com.cyberronin.servicea.util.ServiceBFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(
        name = "SERVICE-B",
        fallback = ServiceBFallback.class)
public interface ServiceBInterface
{
    @GetMapping("/data")
    String getData();
}