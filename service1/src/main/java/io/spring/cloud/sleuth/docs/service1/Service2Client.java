package io.spring.cloud.sleuth.docs.service1;

import java.lang.invoke.MethodHandles;

import brave.Span;
import brave.Tracer;
import brave.propagation.ExtraFieldPropagation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.cloud.sleuth.annotation.SpanTag;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author Marcin Grzejszczak
 */
@Component
class Service2Client {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final WebClient webClient;
	private final String serviceAddress;
	private final Tracer tracer;

	Service2Client(WebClient webClient,
			@Value("${service2.address:localhost:8082}") String serviceAddress,
			Tracer tracer) {
		this.webClient = webClient;
		this.serviceAddress = serviceAddress;
		this.tracer = tracer;
	}

	public String start() throws InterruptedException {
		log.info("Hello from service1. Setting baggage foo=>bar");
		Span span = tracer.currentSpan();
		String secretBaggage = ExtraFieldPropagation.get("baggage");
		log.info("Super secret baggage item for key [baggage] is [{}]", secretBaggage);
		if (StringUtils.hasText(secretBaggage)) {
			span.annotate("secret_baggage_received");
			span.tag("baggage", secretBaggage);
		}
		String baggageKey = "key";
		String baggageValue = "foo";
		ExtraFieldPropagation.set(baggageKey, baggageValue);
		span.annotate("baggage_set");
		span.tag(baggageKey, baggageValue);
		log.info("Hello from service1. Calling service2");
		String response = webClient.get()
				.uri("http://" + serviceAddress + "/foo")
				.exchange()
				.block()
				.bodyToMono(String.class).block();
		Thread.sleep(100);
		log.info("Got response from service2 [{}]", response);
		log.info("Service1: Baggage for [key] is [" + ExtraFieldPropagation.get("key") + "]");
		return response;
	}

	@NewSpan("first_span")
	String timeout(@SpanTag("someTag") String tag) {
		try {
			Thread.sleep(300);
			log.info("Hello from service1. Calling service2 - should end up with read timeout");
			String response = webClient.get()
					.uri("http://" + serviceAddress + "/readtimeout")
					.retrieve()
					.onStatus(httpStatus -> httpStatus.isError(), clientResponse -> {
						throw new IllegalStateException("Exception!");
					})
					.bodyToMono(String.class)
					.block();
			log.info("Got response from service2 [{}]", response);
			return response;
		} catch (Exception e) {
			log.error("Exception occurred while trying to send a request to service 2", e);
			throw new RuntimeException(e);
		}
	}
}
