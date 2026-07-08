package com.treblle.springboot.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaskingServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode json(String s) throws Exception {
        return mapper.readTree(s);
    }

    @Test
    void masksSimpleBodyValuePreservingLength() throws Exception {
        MaskingService service = new MaskingService(List.of("password"));
        JsonNode node = service.maskBody(json("{\"password\":\"secret123\",\"user\":\"alice\"}"));
        assertEquals("*********", node.get("password").asText());
        assertEquals("alice", node.get("user").asText());
    }

    @Test
    void matchingIsCaseInsensitive() throws Exception {
        MaskingService service = new MaskingService(List.of("PassWord"));
        JsonNode node = service.maskBody(json("{\"PASSWORD\":\"abc\"}"));
        assertEquals("***", node.get("PASSWORD").asText());
    }

    @Test
    void masksNestedObjectsRecursively() throws Exception {
        MaskingService service = new MaskingService(List.of("token"));
        JsonNode node = service.maskBody(json("{\"auth\":{\"token\":\"xyzq\"}}"));
        assertEquals("****", node.get("auth").get("token").asText());
    }

    @Test
    void masksArraysItemByItemPreservingStructure() throws Exception {
        MaskingService service = new MaskingService(List.of("cards"));
        JsonNode node = service.maskBody(json("{\"cards\":[\"1234\",\"5678\"]}"));
        assertTrue(node.get("cards").isArray());
        assertEquals("****", node.get("cards").get(0).asText());
        assertEquals("****", node.get("cards").get(1).asText());
    }

    @Test
    void masksObjectsInsideMatchedKeyRecursively() throws Exception {
        MaskingService service = new MaskingService(List.of("secret"));
        JsonNode node = service.maskBody(json("{\"secret\":{\"a\":\"one\",\"b\":\"two\"}}"));
        assertEquals("***", node.get("secret").get("a").asText());
        assertEquals("***", node.get("secret").get("b").asText());
    }

    @Test
    void preservesAuthorizationScheme() {
        MaskingService service = new MaskingService(List.of("authorization"));
        String masked = service.maskHeaderValue("Authorization", "Bearer secret123");
        assertEquals("Bearer *********", masked);
    }

    @Test
    void masksHeadersInMap() {
        MaskingService service = new MaskingService(List.of("authorization", "x-api-key"));
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("authorization", "Bearer abcdef");
        headers.put("x-api-key", "keyvalue");
        headers.put("content-type", "application/json");
        service.maskStringMap(headers);
        assertEquals("Bearer ******", headers.get("authorization"));
        assertEquals("********", headers.get("x-api-key"));
        assertEquals("application/json", headers.get("content-type"));
    }

    @Test
    void skipsNullAndEmptyValues() throws Exception {
        MaskingService service = new MaskingService(List.of("password"));
        JsonNode node = service.maskBody(json("{\"password\":\"\",\"other\":null}"));
        assertEquals("", node.get("password").asText());
        assertTrue(node.get("other").isNull());
    }

    @Test
    void emptyKeywordsSkipsMaskingEntirely() throws Exception {
        MaskingService service = new MaskingService(List.of());
        assertFalse(service.isActive());
        JsonNode node = service.maskBody(json("{\"password\":\"secret\"}"));
        assertEquals("secret", node.get("password").asText());
    }

    @Test
    void doesNotMaskUnlistedKeys() throws Exception {
        MaskingService service = new MaskingService(List.of("password"));
        JsonNode node = service.maskBody(json("{\"email\":\"a@b.com\"}"));
        assertEquals("a@b.com", node.get("email").asText());
    }
}
