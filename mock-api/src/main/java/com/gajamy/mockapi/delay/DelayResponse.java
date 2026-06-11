package com.gajamy.mockapi.delay;

public record DelayResponse(
		long requestedDelayMs,
		long actualDelayMs,
		String threadName
) {
}
