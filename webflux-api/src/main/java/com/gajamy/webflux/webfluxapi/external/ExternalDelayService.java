package com.gajamy.webflux.webfluxapi.external;

import com.gajamy.webflux.webfluxapi.delay.DelayResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ExternalDelayService {
    private final WebClient webClient;

    public Mono<DelayResponse> callExternalDelay(long ms) {
        return webClient.get()
                .uri("/mock-external-delay?ms={ms}", ms)
                .retrieve()
                .bodyToMono(DelayResponse.class);
    }
}
