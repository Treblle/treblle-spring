package com.treblle.springboot.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.treblle.springboot.autoconfigure.TreblleAutoConfiguration;
import com.treblle.springboot.collector.ErrorCollector;
import com.treblle.springboot.collector.RawRequestData;
import com.treblle.springboot.config.TreblleProperties;
import com.treblle.springboot.core.model.TrebllePayload;
import com.treblle.springboot.metadata.TreblleMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayloadFactoryTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private PayloadFactory factory(List<String> masked) {
        TreblleProperties props = new TreblleProperties();
        props.setSdkToken("token-123");
        props.setApiKey("api-456");
        props.setMaskedKeywords(masked);
        MaskingService masking = new MaskingService(masked);
        return new PayloadFactory(mapper, masking, props,
                TreblleAutoConfiguration.SDK_NAME, TreblleAutoConfiguration.SDK_VERSION);
    }

    private RawRequestData baseRequest() {
        RawRequestData raw = new RawRequestData();
        raw.setMethod("POST");
        raw.setUrl("https://api.example.com/v1/users?page=1");
        raw.setRoutePath("/v1/users");
        raw.setClientIp("1.2.3.4");
        raw.setServerIp("10.0.0.1");
        raw.setUserAgent("JUnit");
        raw.setProtocol("HTTP/1.1");
        raw.getQueryParams().put("page", "1");
        raw.getRequestHeaders().put("content-type", "application/json");
        raw.setRequestContentType("application/json");
        raw.setStatusCode(200);
        raw.setResponseContentType("application/json");
        raw.setResponseBody("{\"ok\":true}".getBytes(StandardCharsets.UTF_8));
        raw.setLoadTimeMs(12.5);
        return raw;
    }

    @AfterEach
    void cleanup() {
        ErrorCollector.clear();
        TreblleMetadata.clear();
    }

    @Test
    void producesSchemaValidPayload() throws Exception {
        RawRequestData raw = baseRequest();
        raw.setRequestBody("{\"name\":\"alice\"}".getBytes(StandardCharsets.UTF_8));
        TrebllePayload payload = factory(List.of()).build(raw);
        JsonNode node = mapper.valueToTree(payload);
        assertSchemaValid(node);
        assertEquals("spring-boot", node.get("sdk").asText());
        assertEquals(30, node.get("version").asInt());
        assertEquals("POST", node.at("/data/request/method").asText());
        assertEquals("alice", node.at("/data/request/body/name").asText());
    }

    @Test
    void invalidJsonBodyReplacedWithMessage() throws Exception {
        RawRequestData raw = baseRequest();
        raw.setRequestContentType("text/html");
        raw.setRequestBody("<html>not json</html>".getBytes(StandardCharsets.UTF_8));
        TrebllePayload payload = factory(List.of()).build(raw);
        JsonNode node = mapper.valueToTree(payload);
        assertSchemaValid(node);
        assertTrue(node.at("/data/request/body/message").asText().contains("not valid JSON"));
    }

    @Test
    void fileUploadReplacedWithMetadata() throws Exception {
        RawRequestData raw = baseRequest();
        raw.setRequestContentType("multipart/form-data; boundary=xyz");
        raw.getUploadedFiles().add(new RawRequestData.FilePart("photo.png", 2048, "image/png"));
        TrebllePayload payload = factory(List.of()).build(raw);
        JsonNode node = mapper.valueToTree(payload);
        assertSchemaValid(node);
        JsonNode file = node.at("/data/request/body/files").get(0);
        assertEquals("photo.png", file.get("name").asText());
        assertEquals(2048, file.get("size").asInt());
        assertEquals("image/png", file.get("mime").asText());
    }

    @Test
    void oversizedBodyReplacedWithTooLargeMessage() throws Exception {
        RawRequestData raw = baseRequest();
        byte[] big = new byte[PayloadFactory.MAX_BODY_SIZE + 1];
        java.util.Arrays.fill(big, (byte) 'a');
        raw.setRequestBody(big);
        raw.setRequestContentType("application/json");
        TrebllePayload payload = factory(List.of()).build(raw);
        JsonNode node = mapper.valueToTree(payload);
        assertSchemaValid(node);
        assertTrue(node.at("/data/request/body/message").asText().toLowerCase().contains("too large"));
        assertTrue(node.at("/data/request/body/size").asLong() > PayloadFactory.MAX_BODY_SIZE);
    }

    @Test
    void formUrlEncodedConvertedToJson() throws Exception {
        RawRequestData raw = baseRequest();
        raw.setRequestContentType("application/x-www-form-urlencoded");
        raw.setRequestBody("name=alice&role=admin".getBytes(StandardCharsets.UTF_8));
        TrebllePayload payload = factory(List.of()).build(raw);
        JsonNode node = mapper.valueToTree(payload);
        assertSchemaValid(node);
        assertEquals("alice", node.at("/data/request/body/name").asText());
        assertEquals("admin", node.at("/data/request/body/role").asText());
    }

    @Test
    void maskingAppliedToBodyAndHeaders() throws Exception {
        RawRequestData raw = baseRequest();
        raw.setRequestBody("{\"password\":\"secret\"}".getBytes(StandardCharsets.UTF_8));
        raw.getRequestHeaders().put("authorization", "Bearer abcdef");
        TrebllePayload payload = factory(List.of("password", "authorization")).build(raw);
        JsonNode node = mapper.valueToTree(payload);
        assertEquals("******", node.at("/data/request/body/password").asText());
        assertEquals("Bearer ******", node.at("/data/request/headers/authorization").asText());
    }

    @Test
    void invalidMethodDefaultsToGet() throws Exception {
        RawRequestData raw = baseRequest();
        raw.setMethod("PROPFIND");
        TrebllePayload payload = factory(List.of()).build(raw);
        assertEquals("GET", payload.getData().getRequest().getMethod());
    }

    @Test
    void missingClientIpBecomesBogon() throws Exception {
        RawRequestData raw = baseRequest();
        raw.setClientIp(null);
        TrebllePayload payload = factory(List.of()).build(raw);
        assertEquals("bogon", payload.getData().getRequest().getIp());
    }

    @Test
    void errorsAndMetadataIncluded() throws Exception {
        ErrorCollector.record("onException", new IllegalStateException("boom"));
        TreblleMetadata.add("tenant", "acme");
        RawRequestData raw = baseRequest();
        TrebllePayload payload = factory(List.of()).build(raw);
        JsonNode node = mapper.valueToTree(payload);
        assertSchemaValid(node);
        assertEquals("onException", node.at("/data/errors/0/source").asText());
        assertEquals("boom", node.at("/data/errors/0/message").asText());
        assertEquals("acme", node.at("/data/metadata/tenant").asText());
    }

    @Test
    void emptyErrorsArrayWhenNoErrors() throws Exception {
        TrebllePayload payload = factory(List.of()).build(baseRequest());
        assertTrue(payload.getData().getErrors().isEmpty());
    }

    // --- Minimal structural schema validation (no external validator dependency) ---

    private void assertSchemaValid(JsonNode payload) {
        for (String key : List.of("sdk_token", "api_key", "sdk", "version", "data")) {
            assertTrue(payload.has(key), "missing top-level: " + key);
        }
        assertTrue(payload.get("sdk").asText().matches("^[a-z]+(-[a-z]+)*$"));
        assertTrue(payload.get("version").isInt());

        JsonNode data = payload.get("data");
        for (String key : List.of("server", "language", "request", "response", "queries", "errors")) {
            assertTrue(data.has(key), "missing data." + key);
        }

        JsonNode request = data.get("request");
        for (String key : Set.of("timestamp", "ip", "url", "user_agent", "method", "headers", "body", "route_path", "query")) {
            assertTrue(request.has(key), "missing request." + key);
        }
        assertTrue(request.get("timestamp").asText().matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$"));
        assertTrue(Set.of("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS")
                .contains(request.get("method").asText()));
        assertHeaderMapOfStrings(request.get("headers"));
        assertHeaderMapOfStrings(request.get("query"));

        JsonNode response = data.get("response");
        for (String key : Set.of("headers", "code", "size", "load_time", "body")) {
            assertTrue(response.has(key), "missing response." + key);
        }
        int code = response.get("code").asInt();
        assertTrue(code >= 100 && code <= 599);
        assertTrue(response.get("size").asDouble() >= 0);
        assertHeaderMapOfStrings(response.get("headers"));

        assertTrue(data.get("errors").isArray());
        assertTrue(data.get("queries").isArray());
        for (JsonNode err : data.get("errors")) {
            for (String key : Set.of("source", "type", "message", "file", "line")) {
                assertTrue(err.has(key), "missing error." + key);
            }
            assertTrue(Set.of("onError", "onException", "onShutdown").contains(err.get("source").asText()));
        }
    }

    private void assertHeaderMapOfStrings(JsonNode map) {
        assertTrue(map.isObject());
        map.fields().forEachRemaining(e -> assertTrue(e.getValue().isTextual(),
                "header/query value not a string: " + e.getKey()));
    }
}
