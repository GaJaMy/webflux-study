package com.gajamy.webflux.webfluxapi.delay;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

public class BoundedElasticDelayServiceTest {
    private BoundedElasticDelayService boundedElasticDelayService;

    @BeforeEach
    void setUp() {
        boundedElasticDelayService = new BoundedElasticDelayService();
    }

    @Test
    void webFluxBoundedElasticDelayTest() {
        long requestedMs = 100L;

        Mono<DelayResponse> response = boundedElasticDelayService.delay(requestedMs);

        StepVerifier.create(response)
                .assertNext(delayResponse -> {
                    assertThat(delayResponse.requestedDelayMs()).isEqualTo(requestedMs);
                    assertThat(delayResponse.actualDelayMs()).isGreaterThanOrEqualTo(requestedMs);
                    assertThat(delayResponse.threadName()).isNotBlank();
                    assertThat(delayResponse.threadName()).contains("boundedElastic");
                })
                .verifyComplete();
    }
}
