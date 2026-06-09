package com.gajamy.webflux.delay;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DelayController {
    private final DelayService delayService;

    @GetMapping("/delay")
    public ResponseEntity<?> delayMethod(
            @RequestParam(defaultValue = "1000")
            Long ms
    ) {
        return ResponseEntity.ok(delayService.delay(ms));
    }
}
