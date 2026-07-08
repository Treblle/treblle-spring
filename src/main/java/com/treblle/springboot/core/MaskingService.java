package com.treblle.springboot.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Framework-agnostic masking engine. Replaces the values of configured keys with {@code *},
 * preserving string length, recursing through nested objects and arrays.
 *
 * <p>Matching is case-insensitive and applies to both body fields and headers. If the configured
 * keyword set is empty, masking is skipped entirely.</p>
 */
public class MaskingService {

    private final Set<String> maskedKeywords;

    public MaskingService(List<String> maskedKeywords) {
        this.maskedKeywords = new HashSet<>();
        if (maskedKeywords != null) {
            for (String keyword : maskedKeywords) {
                if (keyword != null && !keyword.isBlank()) {
                    this.maskedKeywords.add(keyword.toLowerCase(Locale.ROOT));
                }
            }
        }
    }

    /** @return {@code true} when at least one keyword is configured. */
    public boolean isActive() {
        return !maskedKeywords.isEmpty();
    }

    private boolean shouldMask(String key) {
        return key != null && maskedKeywords.contains(key.toLowerCase(Locale.ROOT));
    }

    /**
     * Masks a JSON body in place. The node is mutated and returned. Null/empty values are skipped.
     */
    public JsonNode maskBody(JsonNode node) {
        if (!isActive() || node == null) {
            return node;
        }
        maskNode(node, false);
        return node;
    }

    /**
     * Masks a string-to-string header/query map in place, honoring the {@code Authorization}
     * scheme-preservation rule.
     */
    public void maskStringMap(Map<String, String> map) {
        if (!isActive() || map == null) {
            return;
        }
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (shouldMask(entry.getKey())) {
                entry.setValue(maskHeaderValue(entry.getKey(), entry.getValue()));
            }
        }
    }

    private void maskNode(JsonNode node, boolean maskEverything) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            List<String> fieldNames = new java.util.ArrayList<>();
            obj.fieldNames().forEachRemaining(fieldNames::add);
            for (String field : fieldNames) {
                JsonNode child = obj.get(field);
                boolean maskChild = maskEverything || shouldMask(field);
                if (child.isContainerNode()) {
                    // Recurse; if this field name matches, mask everything beneath it.
                    maskNode(child, maskChild);
                } else if (maskChild) {
                    obj.set(field, maskValueNode(child, field));
                }
            }
        } else if (node.isArray()) {
            ArrayNode arr = (ArrayNode) node;
            for (int i = 0; i < arr.size(); i++) {
                JsonNode child = arr.get(i);
                if (child.isContainerNode()) {
                    maskNode(child, maskEverything);
                } else if (maskEverything) {
                    arr.set(i, maskValueNode(child, null));
                }
            }
        }
    }

    private JsonNode maskValueNode(JsonNode value, String key) {
        if (value == null || value.isNull()) {
            return value; // skip null
        }
        String text = value.asText();
        if (text == null || text.isEmpty()) {
            return value; // skip empty
        }
        String masked = "authorization".equalsIgnoreCase(key)
                ? maskAuthorization(text)
                : stars(text.length());
        return TextNode.valueOf(masked);
    }

    /**
     * Masks a plain header/query value. {@code Authorization} preserves the scheme.
     */
    public String maskHeaderValue(String key, String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if ("authorization".equalsIgnoreCase(key)) {
            return maskAuthorization(value);
        }
        return stars(value.length());
    }

    private String maskAuthorization(String value) {
        int space = value.indexOf(' ');
        if (space > 0 && space < value.length() - 1) {
            String scheme = value.substring(0, space);
            String credentials = value.substring(space + 1);
            return scheme + " " + stars(credentials.length());
        }
        return stars(value.length());
    }

    private String stars(int length) {
        if (length <= 0) {
            return "";
        }
        char[] chars = new char[length];
        java.util.Arrays.fill(chars, '*');
        return new String(chars);
    }
}
