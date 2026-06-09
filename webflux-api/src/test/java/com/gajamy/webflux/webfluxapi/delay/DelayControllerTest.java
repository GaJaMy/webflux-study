package com.gajamy.webflux.webfluxapi.delay;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.when;

@WebFluxTest(DelayController.class)
public class DelayControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private DelayService delayService;

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
}
