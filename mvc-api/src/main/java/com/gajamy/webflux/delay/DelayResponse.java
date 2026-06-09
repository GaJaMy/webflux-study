package com.gajamy.webflux.delay;

public record DelayResponse(
        long requestedDelayMs,
        long actualDelayMs,
        String threadName
        ) {
}
