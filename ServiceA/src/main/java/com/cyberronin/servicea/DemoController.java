package com.cyberronin.servicea;

import com.cyberronin.servicea.feign.ServiceBInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

    @Autowired
    private ServiceBInterface serviceB_interface;

    @GetMapping("/hello")
    public String hello(){
        return "Hello this is Service A";
    }

    @GetMapping("/data")
    public String getData(){
        String data = serviceB_interface.getData();
        return "Data from Service A | " + data;
    }
}