package io.spring.cloud.sleuth.docs.service1;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.cloud.sleuth.annotation.SpanTag;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * @author Marcin Grzejszczak
 */
@Component
class Service2Client {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final RestTemplate restTemplate;
	private final String serviceAddress;
	private final Tracer tracer;

	Service2Client(RestTemplate restTemplate,
			@Value("${service2.address:localhost:8082}") String serviceAddress,
			Tracer tracer) {
		this.restTemplate = restTemplate;
		this.serviceAddress = serviceAddress;
		this.tracer = tracer;
	}

	public String start() throws InterruptedException {
		log.info("Hello from service1. Setting baggage foo=>bar");
		Span span = tracer.getCurrentSpan();
		String secretBaggage = span.getBaggageItem("baggage");
		log.info("Super secret baggage item for key [baggage] is [{}]", secretBaggage);
		if (StringUtils.hasText(secretBaggage)) {
			span.logEvent("secret_baggage_received");
			tracer.addTag("baggage", secretBaggage);
		}
		String baggageKey = "key";
		String baggageValue = "foo";
		span.setBaggageItem(baggageKey, baggageValue);
		span.logEvent("baggage_set");
		tracer.addTag(baggageKey, baggageValue);
		log.info("Hello from service1. Calling service2");
		String response = restTemplate.getForObject("http://" + serviceAddress + "/foo", String.class);
		Thread.sleep(100);
		log.info("Got response from service2 [{}]", response);
		log.info("Service1: Baggage for [foo] is [" + span.getBaggageItem("foo") + "]");
		return response;
	}

	@NewSpan("first_span")
	String timeout(@SpanTag("someTag") String tag) {
		try {
			Thread.sleep(300);
			log.info("Hello from service1. Calling service2 - should end up with read timeout");
			String response = restTemplate.getForObject("http://" + serviceAddress + "/readtimeout", String.class);
			log.info("Got response from service2 [{}]", response);
			return response;
		} catch (Exception e) {
			log.error("Exception occurred while trying to send a request to service 2", e);
			throw new RuntimeException(e);
		}
	}
}
