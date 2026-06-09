package com.gajamy.webflux.delay;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DelayController.class)
public class DelayControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DelayService delayService;

    @Test
    @DisplayName("Get /delay는 지연 결과를 JSON으로 반환한다.")
    void delay_shouldReturnDelayResponse() throws Exception {
        long requestedMs = 100L;

        when(delayService.delay(requestedMs))
                .thenReturn(new DelayResponse(
                        requestedMs,
                        101L,
                        "test-thread"
                ));

        mockMvc.perform(get("/delay")
                    .param("ms", String.valueOf(requestedMs)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedDelayMs").value(100))
                .andExpect(jsonPath("$.actualDelayMs").value(101))
                .andExpect(jsonPath("$.threadName").value("test-thread"));
    }

    @Test
    @DisplayName("ms 파라미터가 없으면 기본값 1000ms를 사용한다.")
    void delay_shouldUseDefaultDelayWhenMsIsMissing() throws Exception {
        when(delayService.delay(1000L))
                .thenReturn(new DelayResponse(
                        1000L,
                        1001L,
                        "test-thread"
                ));

        mockMvc.perform(get("/delay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedDelayMs").value(1000))
                .andExpect(jsonPath("$.actualDelayMs").value(1001))
                .andExpect(jsonPath("$.threadName").value("test-thread"));
    }
}
