package com.gajamy.mockapi.delay;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MockExternalDelayController {

	@GetMapping("/mock-external-delay")
	public DelayResponse mockExternalDelay(
			@RequestParam(defaultValue = "1000") long ms
	) {
		long start = System.currentTimeMillis();

		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Delay interrupted", e);
		}

		long end = System.currentTimeMillis();

		return new DelayResponse(
				ms,
				end - start,
				Thread.currentThread().getName()
		);
	}
}
