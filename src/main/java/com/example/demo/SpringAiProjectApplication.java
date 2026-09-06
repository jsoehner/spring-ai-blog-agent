package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
		org.springframework.boot.http.client.autoconfigure.reactive.ReactiveHttpClientAutoConfiguration.class
})
@EnableScheduling
public class SpringAiProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringAiProjectApplication.class, args);
	}

}
