package com.cyberronin.serviceb;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

    @GetMapping("/data")
    public String getData(){
        return "this is data from Service B";
    }
}