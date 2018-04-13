package io.spring.cloud.sleuth.docs.service1;

import java.time.LocalDateTime;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Service1Controller {

	private final Service2Client service2Client;

	public Service1Controller(Service2Client service2Client) {
		this.service2Client = service2Client;
	}

	@RequestMapping("/start")
	public String start() throws InterruptedException {
		return this.service2Client.start();
	}

	@RequestMapping("/readtimeout")
	public String timeout() {
		return service2Client.timeout(LocalDateTime.now().toString());
	}
}
