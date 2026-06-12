package com.gajamy.webflux.external;

import com.gajamy.webflux.delay.DelayResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ExternalDelayController {
    private final ExternalDelayService externalDelayService;

    @GetMapping("/external-delay")
    public DelayResponse externalDelay(
            @RequestParam(defaultValue = "1000")
            long ms
    ) {
        return externalDelayService.callExternalDelay(ms);
    }

    @GetMapping("/external-delay-webflux")
    public DelayResponse externalDelayWebflux(
            @RequestParam(defaultValue = "1000")
            long ms
    ) {
        return externalDelayService.callWebFluxExternalDelay(ms);
    }
}
