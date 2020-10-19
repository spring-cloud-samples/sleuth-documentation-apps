package io.spring.cloud.sleuth.docs.service4;

import io.opentelemetry.baggage.BaggageManager;
import io.opentelemetry.context.Scope;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Tracer;
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
class Service4Controller {
	private static final Logger log = LoggerFactory.getLogger(Service4Controller.class);

	private final Tracer tracer;

	private final BaggageManager baggageManager;

	Service4Controller(Tracer tracer, BaggageManager baggageManager) {
		this.tracer = tracer;
		this.baggageManager = baggageManager;
	}

	@RequestMapping("/baz")
	public String service4MethodInController() throws InterruptedException {
		Thread.sleep(400);
		Span newSpan = this.tracer.spanBuilder("new_span").startSpan();
		try (Scope scope = this.tracer.withSpan(newSpan)) {
			log.info("Hello from service4");
		}
		log.info("Service4: Baggage for [key] is [" + this.baggageManager.getCurrentBaggage().getEntryValue("key") + "]");
		return "Hello from service4";
	}
}