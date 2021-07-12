package com.example.presentationservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class PresentationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PresentationServiceApplication.class, args);
	}

	@Bean
	RestTemplate restTemplate() {
		return new RestTemplate();
	}
}

@RestController
class PresentationController {

	private static final Logger log = LoggerFactory.getLogger(PresentationController.class);

	private final RestTemplate restTemplate;

	PresentationController(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	@GetMapping("/")
	String start() {
		log.info("HELLO FROM PRESENTATION-SERVICE");
		return "PRESENTATION SERVICE: " + this.restTemplate.postForObject("http://localhost:9081/start", "", String.class);
	}
}
