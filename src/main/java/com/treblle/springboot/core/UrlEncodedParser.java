package com.treblle.springboot.core;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared parser for {@code key=value&key2=value2} strings, used for both URL query strings and
 * {@code application/x-www-form-urlencoded} bodies.
 *
 * <p>Values are URL-decoded. Repeated keys follow last-value-wins. Undecodable tokens are passed
 * through unchanged rather than throwing.</p>
 */
public final class UrlEncodedParser {

    private UrlEncodedParser() {
    }

    /** Parses a {@code key=value&...} string into an ordered map. Never returns {@code null}. */
    public static Map<String, String> parse(String raw) {
        Map<String, String> out = new LinkedHashMap<>();
        if (raw == null || raw.isEmpty()) {
            return out;
        }
        for (String pair : raw.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            String key = eq >= 0 ? pair.substring(0, eq) : pair;
            String value = eq >= 0 ? pair.substring(eq + 1) : "";
            out.put(decode(key), decode(value)); // last value wins
        }
        return out;
    }

    /** URL-decodes a single token, returning it unchanged if decoding fails. */
    public static String decode(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }
}
