package com.example.presentationservice;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.observability.event.Recorder;
import org.springframework.core.observability.event.instant.InstantEvent;
import org.springframework.core.observability.event.interval.IntervalEvent;
import org.springframework.core.observability.event.interval.IntervalRecording;
import org.springframework.core.observability.event.tag.Cardinality;
import org.springframework.core.observability.event.tag.Tag;
import org.springframework.stereotype.Service;
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

	private final RestTemplateService service;

	private final Random random = new Random();

	PresentationController(RestTemplateService service) {
		this.service = service;
	}

	@GetMapping("/")
	String start() {
		log.info("HELLO FROM PRESENTATION-SERVICE");
		return "PRESENTATION SERVICE: " + this.service.call(String.valueOf(this.random.nextInt()));
	}
}

@Service
class RestTemplateService {
	private final RestTemplate restTemplate;

	private final Recorder<?> recorder;

	RestTemplateService(RestTemplate restTemplate, Recorder<?> recorder) {
		this.restTemplate = restTemplate;
		this.recorder = recorder;
	}

	String call(String param) {
		IntervalRecording<?> intervalRecording = this.recorder.recordingFor((IntervalEvent) () -> "calling-start")
				.tag(Tag.of("method.name", "call", Cardinality.LOW))
				.tag(Tag.of("method.param", param, Cardinality.HIGH))
				.highCardinalityName("calling-start " + param)
				.start();
		try {
			this.recorder.recordingFor((InstantEvent) () -> "calling-start.before-call").record();
			return this.restTemplate.postForObject("http://localhost:9081/start", "", String.class);
		} catch (Exception ex) {
			intervalRecording.error(ex);
			throw ex;
		} finally {
			intervalRecording.stop();
		}
	}
}