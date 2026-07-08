package com.treblle.springboot.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.treblle.springboot.config.TreblleProperties;
import com.treblle.springboot.core.TreblleLogger;
import com.treblle.springboot.core.model.TrebllePayload;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TreblleClientSafetyTest {

    @Test
    void gzipRoundTrips() throws Exception {
        byte[] input = "{\"hello\":\"world\"}".getBytes(StandardCharsets.UTF_8);
        byte[] compressed = TreblleClient.gzip(input);
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            String restored = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals("{\"hello\":\"world\"}", restored);
        }
    }

    @Test
    void sendToUnreachableEndpointNeverThrows() {
        TreblleProperties props = new TreblleProperties();
        props.setSdkToken("token");
        props.setApiKey("key");
        // Non-routable/invalid endpoint so the network call fails.
        props.setIngressEndpoint("http://127.0.0.1:1/never");

        CircuitBreaker cb = new CircuitBreaker(new TreblleLogger(false));
        TreblleClient client = new TreblleClient(props, new ObjectMapper(), new TreblleLogger(false), cb);

        TrebllePayload payload = new TrebllePayload();
        payload.setSdk("spring-boot");
        payload.setVersion(10);

        assertDoesNotThrow(() -> {
            for (int i = 0; i < 50; i++) {
                client.send(payload);
            }
        });

        client.shutdown();
    }

    @Test
    void sendWhileCircuitOpenDropsSilently() {
        TreblleProperties props = new TreblleProperties();
        props.setSdkToken("token");
        props.setApiKey("key");

        CircuitBreaker cb = new CircuitBreaker(new TreblleLogger(false));
        cb.onFailure(60L); // force a long backoff window

        TreblleClient client = new TreblleClient(props, new ObjectMapper(), new TreblleLogger(false), cb);
        TrebllePayload payload = new TrebllePayload();

        assertDoesNotThrow(() -> client.send(payload));
        client.shutdown();
    }
}
