package com.treblle.springboot.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.treblle.springboot.autoconfigure.TreblleAutoConfiguration;
import com.treblle.springboot.collector.RawRequestData;
import com.treblle.springboot.config.TreblleProperties;
import com.treblle.springboot.core.MaskingService;
import com.treblle.springboot.core.PayloadFactory;
import com.treblle.springboot.core.TreblleLogger;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TreblleClientSafetyTest {

    private PayloadFactory payloadFactory(TreblleProperties props) {
        return new PayloadFactory(new ObjectMapper(), new MaskingService(List.of()), props,
                TreblleAutoConfiguration.SDK_NAME, TreblleAutoConfiguration.SDK_VERSION);
    }

    private RawRequestData sampleRaw() {
        RawRequestData raw = new RawRequestData();
        raw.setMethod("GET");
        raw.setUrl("https://api.example.com/v1/ping");
        raw.setStatusCode(200);
        return raw;
    }

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
        TreblleClient client = new TreblleClient(props, new ObjectMapper(), new TreblleLogger(false),
                cb, payloadFactory(props));

        assertDoesNotThrow(() -> {
            for (int i = 0; i < 50; i++) {
                client.send(sampleRaw());
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

        TreblleClient client = new TreblleClient(props, new ObjectMapper(), new TreblleLogger(false),
                cb, payloadFactory(props));

        assertDoesNotThrow(() -> client.send(sampleRaw()));
        client.shutdown();
    }
}
