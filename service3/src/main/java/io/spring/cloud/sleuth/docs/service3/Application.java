package io.spring.cloud.sleuth.docs.service3;

import brave.baggage.BaggageField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class Application {

	public static void main(String... args) {
		new SpringApplication(Application.class).run(args);
	}
}

@RestController
class Service3Controller {
	private static final Logger log = LoggerFactory.getLogger(Service3Controller.class);

	@RequestMapping("/bar")
	public String service3MethodInController() throws InterruptedException {
		Thread.sleep(300);
		log.info("Hello from service3");
		log.info("Service3: Baggage for [key] is [" + BaggageField.getByName("key").getValue() + "]");
		return "Hello from service3";
	}
}
