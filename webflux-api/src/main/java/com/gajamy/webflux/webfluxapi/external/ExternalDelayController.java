package com.gajamy.webflux.webfluxapi.external;

import com.gajamy.webflux.webfluxapi.delay.DelayResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class ExternalDelayController {
    private final ExternalDelayService externalDelayService;

    @GetMapping("/external-delay")
    public Mono<DelayResponse> externalElasticDelay(
            @RequestParam(defaultValue = "1000")
            Long ms
    ) {
        return externalDelayService.callExternalDelay(ms);
    }
}
