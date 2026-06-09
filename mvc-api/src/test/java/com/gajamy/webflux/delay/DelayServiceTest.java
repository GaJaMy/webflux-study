package com.gajamy.webflux.delay;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DelayServiceTest {
    private DelayService delayService;

    @BeforeEach
    void setUp() {
        delayService = new DelayService();
    }

    @Test
    void delayMethodTest() {
        long requestedMs = 100;

        DelayResponse response = delayService.delay(requestedMs);

        assertThat(response.requestedDelayMs()).isEqualTo(requestedMs);
        assertThat(response.actualDelayMs()).isGreaterThanOrEqualTo(requestedMs);
        assertThat(response.threadName()).isNotBlank();
    }
}
