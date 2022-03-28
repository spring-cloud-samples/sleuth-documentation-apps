package com.example.presentationservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
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

	private final String serviceUrl;

	PresentationController(RestTemplate restTemplate, @Value("${service1.address:localhost:9081}") String serviceUrl) {
		this.restTemplate = restTemplate;
		this.serviceUrl = serviceUrl;
	}

	@GetMapping("/")
	String start() {
		log.info("HELLO FROM PRESENTATION-SERVICE");
		return "PRESENTATION SERVICE: " + this.restTemplate.postForObject("http://" + this.serviceUrl + "/start", "", String.class);
	}
}
