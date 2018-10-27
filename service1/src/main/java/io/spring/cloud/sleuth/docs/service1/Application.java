package io.spring.cloud.sleuth.docs.service1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
public class Application {

	@Bean WebClient webClient() { return WebClient.create(); }

	public static void main(String... args) {
		new SpringApplication(Application.class).run(args);
	}
}
