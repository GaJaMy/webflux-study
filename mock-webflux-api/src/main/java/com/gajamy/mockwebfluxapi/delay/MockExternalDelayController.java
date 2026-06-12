package com.gajamy.mockwebfluxapi.delay;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
public class MockExternalDelayController {

	@GetMapping("/mock-external-delay")
	public Mono<DelayResponse> mockExternalDelay(
			@RequestParam(defaultValue = "1000") long ms
	) {
		long start = System.currentTimeMillis();

		return Mono.delay(Duration.ofMillis(ms))
				.map(ignored -> {
					long end = System.currentTimeMillis();

					return new DelayResponse(
							ms,
							end - start,
							Thread.currentThread().getName()
					);
				});
	}
}
