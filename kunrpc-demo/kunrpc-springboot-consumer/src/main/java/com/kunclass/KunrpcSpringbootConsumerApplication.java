package com.kunclass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
public class KunrpcSpringbootConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(KunrpcSpringbootConsumerApplication.class, args);
    }

    @GetMapping("/test")
    public String test() {
        return "test consumer";
    }

}
