package com.gajamy.webflux.external;

import com.gajamy.webflux.delay.DelayResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class ExternalDelayService {
    private final RestTemplate restTemplate;

    public DelayResponse callExternalDelay(long ms) {
        String url = "http://localhost:8090/mock-external-delay?ms={ms}";

        return restTemplate.getForObject(
                url,
                DelayResponse.class,
                ms
        );
    }
}
