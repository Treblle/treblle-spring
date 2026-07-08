package com.treblle.springboot.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.treblle.springboot.collector.RawRequestData;
import com.treblle.springboot.config.TreblleProperties;
import com.treblle.springboot.core.MaskingService;
import com.treblle.springboot.core.PayloadFactory;
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
import java.util.List;
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

    /**
     * Builds a payload from captured raw data exactly as the transport would off-thread, so these
     * tests exercise the real filter capture + factory masking pipeline end-to-end.
     */
    private TrebllePayload buildPayload(RawRequestData raw, String... maskedKeywords) {
        TreblleProperties props = new TreblleProperties();
        props.setSdkToken("test-token");
        props.setApiKey("test-key");
        PayloadFactory factory = new PayloadFactory(new ObjectMapper(),
                new MaskingService(List.of(maskedKeywords)), props, "spring-boot", 30);
        return factory.build(raw);
    }

    private RawRequestData captureRaw() {
        ArgumentCaptor<RawRequestData> captor = ArgumentCaptor.forClass(RawRequestData.class);
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                verify(client, atLeastOnce()).send(captor.capture()));
        return captor.getValue();
    }

    @Test
    void capturesGetRequestAndSendsPayload() {
        rest.getForEntity("/api/users", String.class);

        TrebllePayload payload = buildPayload(captureRaw());
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

        TrebllePayload payload = buildPayload(captureRaw(), "password", "authorization");
        String body = payload.getData().getRequest().getBody().toString();
        assert body.contains("******") : "expected masked value in: " + body;
        assert !body.contains("secret") : "raw secret leaked in: " + body;
    }

    @Test
    void queryParamsExcludedFromUrl() {
        rest.getForEntity("/api/users?token=supersecret&page=2", String.class);

        RawRequestData raw = captureRaw();
        assert !raw.getUrl().contains("supersecret") : "raw secret leaked in url: " + raw.getUrl();
        assert !raw.getUrl().contains("?") : "query string not stripped from url: " + raw.getUrl();
        assert raw.getQueryParams().containsKey("token") : "query params not captured separately";

        // And the sensitive query value is masked in the built payload's query map.
        TrebllePayload payload = buildPayload(raw, "token");
        assert payload.getData().getRequest().getQuery().get("token").equals("***********")
                : "expected masked token, got: " + payload.getData().getRequest().getQuery().get("token");
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

        RawRequestData raw = captureRaw();
        TrebllePayload payload = buildPayload(raw);
        assert !payload.getData().getErrors().isEmpty() : "expected captured error";
        assert payload.getData().getResponse().getCode() >= 500
                : "expected 5xx, got " + payload.getData().getResponse().getCode();
    }
}
