package com.gajamy.webflux.webfluxapi.delay;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@WebFluxTest(DelayController.class)
public class DelayControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private DelayService delayService;

    @MockitoBean
    private BlockingDelayService blockingDelayService;

    @MockitoBean
    private BoundedElasticDelayService boundedElasticDelayService;

    @Test
    @DisplayName("Get /delay는 지연 결과를 JSON으로 반환한다.")
    void delay_shouldReturnDelayResponse() throws Exception {
        long requestedMs = 100L;

        when(delayService.delay(requestedMs))
                .thenReturn(Mono.just(
                        new DelayResponse(
                                requestedMs,
                                101L,
                                "test-thread"
                        )
                ));

        webTestClient.get()
                .uri("/delay?ms={ms}", requestedMs)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.requestedDelayMs").isEqualTo(100)
                .jsonPath("$.actualDelayMs").isEqualTo(101)
                .jsonPath("$.threadName").isEqualTo("test-thread");
    }

    @Test
    @DisplayName("Get /block은 지연 결과를 JSON으로 반환한다.")
    void blockDelay_shouldReturnDelayResponse() throws Exception {
        long requestedMs = 100L;

        when(blockingDelayService.delay(requestedMs))
                .thenReturn(
                        new DelayResponse(
                                requestedMs,
                                101L,
                                "test-thread"
                        )
                );

        webTestClient.get()
                .uri("/blocking-delay?ms={ms}", requestedMs)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.requestedDelayMs").isEqualTo(100)
                .jsonPath("$.actualDelayMs").isEqualTo(101)
                .jsonPath("$.threadName").isEqualTo("test-thread");
    }

    @Test
    @DisplayName("Get /bounded-elastic-delay는 지연 결과를 JSON으로 반환한다.")
    void boundedElasticDelay_shouldReturnDelayResponse() throws Exception {
        long requestedMs = 100L;

        when(boundedElasticDelayService.delay(requestedMs))
                .thenReturn(Mono.just(
                                new DelayResponse(
                                        requestedMs,
                                        101L,
                                        "boundedElastic-1"
                                )
                        )
                );

        webTestClient.get()
                .uri("/bounded-elastic-delay?ms={ms}", requestedMs)
                .exchange()
                .expectStatus().isOk()
                .expectBody(DelayResponse.class)
                .value(response -> {
                    assertThat(response.requestedDelayMs()).isEqualTo(100);
                    assertThat(response.actualDelayMs()).isEqualTo(101);
                    assertThat(response.threadName()).contains("boundedElastic");
                });
    }
}
