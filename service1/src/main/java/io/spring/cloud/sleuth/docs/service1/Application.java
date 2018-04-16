package io.spring.cloud.sleuth.docs.service1;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import zipkin2.reporter.Sender;

@SpringBootApplication
public class Application {

	@Bean WebClient webClient() { return WebClient.create(); }

	public static void main(String... args) {
		new SpringApplication(Application.class).run(args);
	}
}
