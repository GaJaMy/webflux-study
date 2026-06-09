package com.gajamy.webflux.webfluxapi.delay;

import org.springframework.stereotype.Service;

@Service
public class BlockingDelayService {

    public DelayResponse delay(long ms) {
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
