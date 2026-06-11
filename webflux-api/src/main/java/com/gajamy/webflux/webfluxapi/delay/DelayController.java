package com.gajamy.webflux.webfluxapi.delay;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class DelayController {
    private final DelayService delayService;
    private final BlockingDelayService blockingDelayService;
    private final BoundedElasticDelayService boundedElasticDelayService;

    @GetMapping("/delay")
    public Mono<DelayResponse> delay(
            @RequestParam(defaultValue = "1000")
            Long ms
    ) {
        return delayService.delay(ms);
    }

    @GetMapping("/blocking-delay")
    public DelayResponse blockingDelay(
            @RequestParam(defaultValue = "1000")
            Long ms
    ) {
        return blockingDelayService.delay(ms);
    }

    @GetMapping("/bounded-elastic-delay")
    public Mono<DelayResponse> boundedElasticDelay(
            @RequestParam(defaultValue = "1000")
            Long ms
    ) {
        return boundedElasticDelayService.delay(ms);
    }
}
