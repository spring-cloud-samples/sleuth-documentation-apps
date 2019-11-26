package io.spring.cloud.sleuth.docs.service1;

import java.time.LocalDateTime;

import reactor.core.publisher.Mono;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Service1Controller {

	private final Service2Client service2Client;

	public Service1Controller(Service2Client service2Client) {
		this.service2Client = service2Client;
	}

	@RequestMapping("/start")
	public Mono<String> start() {
		return this.service2Client.start();
	}

	@RequestMapping("/readtimeout")
	public Mono<String> timeout() throws InterruptedException {
		return service2Client.timeout(LocalDateTime.now().toString());
	}
}
