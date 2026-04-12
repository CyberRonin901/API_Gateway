package com.cyberronin.authservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

// Dummy controller for testing auth
@RestController
@RequestMapping("/user")
public class HelloController{

    @GetMapping("/hello")
    public Mono<String> getData(){
        return Mono.just("Auth Service say hello :)");
    }
}
