package com.gajamy.webflux.webfluxapi.delay;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

public class DelayServiceTest {
    private DelayService delayService;

    @BeforeEach
    void setUp() {
        delayService = new DelayService();
    }

    @Test
    void webfluxDelayMethodTest() {
        long requestedMs = 100L;

        Mono<DelayResponse> response = delayService.delay(requestedMs);

        StepVerifier.create(response)
                .assertNext(delayResponse -> {
                    assertThat(delayResponse.requestedDelayMs()).isEqualTo(requestedMs);
                    assertThat(delayResponse.actualDelayMs()).isGreaterThanOrEqualTo(requestedMs);
                    assertThat(delayResponse.threadName()).isNotBlank();
                })
                .verifyComplete();
    }
}
