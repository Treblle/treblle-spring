package com.treblle.springboot.filter;

import com.treblle.springboot.core.model.TrebllePayload;
import com.treblle.springboot.transport.TreblleClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "treblle.sdk-token=test-token",
                "treblle.api-key=test-key",
                "treblle.masked-keywords=password,authorization",
                "treblle.excluded-paths=/api/health"
        })
class TreblleFilterIntegrationTest {

    @MockBean
    private TreblleClient client;

    @Autowired
    private TestRestTemplate rest;

    @Test
    void capturesGetRequestAndSendsPayload() {
        rest.getForEntity("/api/users", String.class);

        ArgumentCaptor<TrebllePayload> captor = ArgumentCaptor.forClass(TrebllePayload.class);
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                verify(client, atLeastOnce()).send(captor.capture()));

        TrebllePayload payload = captor.getValue();
        assert payload.getData().getRequest().getMethod().equals("GET");
        assert payload.getData().getResponse().getCode() == 200;
        assert payload.getSdk().equals("spring-boot");
    }

    @Test
    void masksBodyOnPost() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{\"password\":\"secret\",\"user\":\"alice\"}", headers);
        rest.exchange("/api/echo", HttpMethod.POST, entity, String.class);

        ArgumentCaptor<TrebllePayload> captor = ArgumentCaptor.forClass(TrebllePayload.class);
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                verify(client, atLeastOnce()).send(captor.capture()));

        String body = captor.getValue().getData().getRequest().getBody().toString();
        assert body.contains("******") : "expected masked value in: " + body;
        assert !body.contains("secret") : "raw secret leaked in: " + body;
    }

    @Test
    void excludedPathIsNotSent() throws Exception {
        rest.getForEntity("/api/health", String.class);
        TimeUnit.MILLISECONDS.sleep(400);
        verify(client, never()).send(any());
    }

    @Test
    void hostErrorIsCapturedAndResponseStillSent() {
        rest.getForEntity("/api/boom", String.class); // returns 500 to the client

        ArgumentCaptor<TrebllePayload> captor = ArgumentCaptor.forClass(TrebllePayload.class);
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                verify(client, atLeastOnce()).send(captor.capture()));

        TrebllePayload payload = captor.getValue();
        assert !payload.getData().getErrors().isEmpty() : "expected captured error";
        assert payload.getData().getResponse().getCode() >= 500
                : "expected 5xx, got " + payload.getData().getResponse().getCode();
    }
}
