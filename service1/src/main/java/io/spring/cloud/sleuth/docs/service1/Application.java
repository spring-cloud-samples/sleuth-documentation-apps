package io.spring.cloud.sleuth.docs.service1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class Application {

	@Bean
	RestTemplate restTemplate() {
		return new RestTemplate();
	}
//	@Bean
//	public RestTemplate restTemplate(RestTemplateBuilder builder) {
//		return builder.build();
//	}

	public static void main(String... args) {
		new SpringApplication(Application.class).run(args);
	}
}
