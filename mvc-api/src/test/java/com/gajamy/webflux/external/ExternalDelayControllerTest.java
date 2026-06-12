package com.gajamy.webflux.external;

import com.gajamy.webflux.delay.DelayResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExternalDelayController.class)
public class ExternalDelayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExternalDelayService externalDelayService;

    @Test
    void externalDelayTest() throws Exception {
        long requestedMs = 100L;

        when(externalDelayService.callExternalDelay(requestedMs))
                .thenReturn(new DelayResponse(
                        requestedMs,
                        101L,
                        "test-thread"
                ));

        mockMvc.perform(get("/external-delay")
                        .param("ms", String.valueOf(requestedMs)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedDelayMs").value(100))
                .andExpect(jsonPath("$.actualDelayMs").value(101))
                .andExpect(jsonPath("$.threadName").value("test-thread"));
    }

    @Test
    void externalDelayWebflux() throws Exception {
        long requestedMs = 100L;

        when(externalDelayService.callWebFluxExternalDelay(requestedMs))
                .thenReturn(
                        new DelayResponse(
                                requestedMs,
                                101L,
                                "test-thread"
                        )
                );

        mockMvc.perform(get("/external-delay-webflux")
                    .param("ms", String.valueOf(requestedMs)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedDelayMs").value(100))
                .andExpect(jsonPath("$.actualDelayMs").value(101))
                .andExpect(jsonPath("$.threadName").value("test-thread"));
    }
}
