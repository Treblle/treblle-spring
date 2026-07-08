package com.treblle.springboot.filter;

import com.treblle.springboot.transport.TreblleClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * When credentials are missing, the SDK must silently disable itself: the filter is present but
 * no data is ever sent, and the host application works normally.
 */
@SpringBootTest(
        classes = TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                // No sdk-token / api-key on purpose.
                "treblle.masked-keywords=password"
        })
class TreblleDisabledTest {

    @MockBean
    private TreblleClient client;

    @Autowired
    private TestRestTemplate rest;

    @Test
    void missingCredentialsSendsNothingButHostWorks() throws Exception {
        var response = rest.getForEntity("/api/users", String.class);
        assert response.getStatusCode().is2xxSuccessful();
        assert response.getBody() != null && response.getBody().contains("alice");

        TimeUnit.MILLISECONDS.sleep(400);
        verify(client, never()).send(any());
    }
}
