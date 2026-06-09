package com.gajamy.webflux.webfluxapi.delay;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

public class BlockingDelayServiceTest {
    private BlockingDelayService blockingDelayService;

    @BeforeEach
    void setUp() {
        blockingDelayService = new BlockingDelayService();
    }

    @Test
    void webfluxBlockingDelayTest() {
        long requestedMs = 100L;

        DelayResponse response = blockingDelayService.delay(requestedMs);

        assertThat(response.requestedDelayMs()).isEqualTo(requestedMs);
        assertThat(response.actualDelayMs()).isGreaterThanOrEqualTo(requestedMs);
        assertThat(response.threadName()).isNotBlank();
    }

}
