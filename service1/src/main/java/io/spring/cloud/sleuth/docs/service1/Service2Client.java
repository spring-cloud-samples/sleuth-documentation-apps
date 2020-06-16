package io.spring.cloud.sleuth.docs.service1;

import java.lang.invoke.MethodHandles;

import brave.Span;
import brave.Tracer;
import brave.baggage.BaggageField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.cloud.sleuth.annotation.SpanTag;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
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

	public Mono<String> start() {
		log.info("Hello from service1. Setting baggage foo=>bar");
		Span span = tracer.currentSpan();
		BaggageField secretBaggageField = BaggageField.getByName(span.context(), "baggage");
		String secretBaggage = secretBaggageField.getValue();
		log.info("Super secret baggage item for key [baggage] is [{}]", secretBaggage);
		if (StringUtils.hasText(secretBaggage)) {
			span.annotate("secret_baggage_received");
			span.tag("baggage", secretBaggage);
		}
		String baggageKey = "key";
		String baggageValue = "foo";
		BaggageField baggageField = BaggageField.create(baggageKey);
		baggageField.updateValue(baggageValue);
		span.annotate("baggage_set");
		span.tag(baggageKey, baggageValue);
		log.info("Hello from service1. Calling service2");
		return webClient.get()
				.uri("http://" + serviceAddress + "/foo")
				.exchange()
				.doOnSuccess(clientResponse -> {
					log.info("Got response from service2 [{}]", clientResponse);
					log.info("Service1: Baggage for [key] is [" + BaggageField.getByName("key") + "]");
				})
				.flatMap(clientResponse -> clientResponse.bodyToMono(String.class));
	}

	@NewSpan("first_span")
	Mono<String> timeout(@SpanTag("someTag") String tag) throws InterruptedException {
		Thread.sleep(300);
		log.info("Hello from service1. Calling service2 - should end up with read timeout");
		return webClient.get()
				.uri("http://" + serviceAddress + "/readtimeout")
				.retrieve()
				.onStatus(HttpStatus::is5xxServerError, ClientResponse::createException)
				.bodyToMono(String.class);
	}
}
