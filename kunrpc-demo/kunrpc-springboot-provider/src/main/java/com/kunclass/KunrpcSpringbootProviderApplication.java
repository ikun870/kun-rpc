package com.kunclass;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class KunrpcSpringbootProviderApplication {

	public static void main(String[] args) {
		SpringApplication.run(KunrpcSpringbootProviderApplication.class, args);
	}

	@GetMapping("test")
	public String Hello() {
		return "Hello";
	}

}
