package com.treblle.spring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.treblle.spring.utils.DataMasker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest(classes = TestConfig.class)
@TestPropertySource(properties = {
        "treblle.masking-keywords=firstLevel.*"
})
public class DataMaskerTest {

    @Autowired
    private DataMasker dataMasker;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testJsonMasking() {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode firstLevel = objectMapper.createObjectNode();
        firstLevel.put("card_number", "some_secret");

        root.set("firstLevel", firstLevel);
        root.put("CCV", "some_secret");
        root.put("hello", "treblle");

        JsonNode result = dataMasker.mask(root);

        assert "******".equals(result.get("CCV").asText());
        assert "******".equals(result.get("firstLevel").get("card_number").asText());
        assert "treblle".equals(result.get("hello").asText());
    }

    @Test
    public void testCatchAllJsonMasking() {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode firstLevel = objectMapper.createObjectNode();
        firstLevel.put("some_field", "some_secret");
        firstLevel.put("some_field2", "some_secret2");

        root.set("firstLevel", firstLevel);
        root.put("CCV", "some_secret");
        root.put("hello", "treblle");

        JsonNode result = dataMasker.mask(root);

        assert "******".equals(result.get("CCV").asText());
        assert "******".equals(result.get("firstLevel").get("some_field").asText());
        assert "******".equals(result.get("firstLevel").get("some_field2").asText());
        assert "treblle".equals(result.get("hello").asText());
    }

    @Test
    public void testHeaderMasking() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Password", "some_secret");
        headers.put("User-Agent", "Treblle");

        Map<String, String> result = dataMasker.mask(headers);

        assert "******".equals(result.get("Password"));
        assert "Treblle".equals(result.get("User-Agent"));
    }

}