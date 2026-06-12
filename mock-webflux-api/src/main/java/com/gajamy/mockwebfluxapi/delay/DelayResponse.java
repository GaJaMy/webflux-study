package com.gajamy.mockwebfluxapi.delay;

public record DelayResponse(
		long requestedDelayMs,
		long actualDelayMs,
		String threadName
) {
}
