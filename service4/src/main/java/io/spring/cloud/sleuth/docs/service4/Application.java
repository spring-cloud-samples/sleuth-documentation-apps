package io.spring.cloud.sleuth.docs.service4;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class Application {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Autowired Tracer tracer;

	@RequestMapping("/baz")
	public String service4MethodInController() throws InterruptedException {
		Thread.sleep(400);
		log.info("Hello from service4");
		log.info("Service4: Baggage for [key] is [" + tracer.getCurrentSpan().getBaggageItem("key") + "]");
		return "Hello from service4";
	}

	public static void main(String... args) {
		new SpringApplication(Application.class).run(args);
	}
}
