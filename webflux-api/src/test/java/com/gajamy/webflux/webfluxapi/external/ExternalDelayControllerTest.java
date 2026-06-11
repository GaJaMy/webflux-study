package com.gajamy.webflux.webfluxapi.external;

import com.gajamy.webflux.webfluxapi.delay.DelayResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.when;

@WebFluxTest(ExternalDelayController.class)
public class ExternalDelayControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ExternalDelayService externalDelayService;

    @Test
    void webfluxExternalElastic() {
        long requestedMs = 100L;

        when(externalDelayService.callExternalDelay(requestedMs))
                .thenReturn(Mono.just(
                        new DelayResponse(
                                requestedMs,
                                101L,
                                "test-thread"
                        )
                ));

        webTestClient.get()
                .uri("/external-delay?ms={ms}", requestedMs)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.requestedDelayMs").isEqualTo(100)
                .jsonPath("$.actualDelayMs").isEqualTo(101)
                .jsonPath("$.threadName").isEqualTo("test-thread");
    }
}
