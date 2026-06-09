package com.gajamy.webflux.webfluxapi.delay;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
public class DelayService {

    public Mono<DelayResponse> delay(long ms) {
        long start = System.currentTimeMillis();
        return Mono.delay(Duration.ofMillis(ms))
                .map(ignored -> {
                    long end = System.currentTimeMillis();

                    return new DelayResponse(
                            ms,
                            end - start,
                            Thread.currentThread().getName()
                    );
                });
    }
}
