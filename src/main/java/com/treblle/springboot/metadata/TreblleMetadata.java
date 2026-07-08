package com.treblle.springboot.metadata;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Native, developer-friendly way to attach custom metadata to the current request.
 *
 * <p>Metadata is sent as key/value pairs in the payload's {@code metadata} object. It is bound to
 * the request thread and cleared automatically once the request completes.</p>
 *
 * <p><strong>Metadata is NOT masked.</strong> Never put secrets, credentials, or personal data here.</p>
 *
 * <p>Usage from anywhere on the request thread (controller, service, etc.):</p>
 * <pre>{@code
 * TreblleMetadata.add("tenant", "acme");
 * TreblleMetadata.add("feature_flag_x", true);
 * }</pre>
 *
 * <p>The Treblle schema caps metadata at 20 entries, keys at 64 characters, and string values at
 * 128 characters. Entries that violate these limits are ignored so the payload always stays valid.</p>
 */
public final class TreblleMetadata {

    private static final int MAX_ENTRIES = 20;
    private static final int MAX_KEY_LENGTH = 64;
    private static final int MAX_STRING_VALUE_LENGTH = 128;

    private static final ThreadLocal<Map<String, Object>> HOLDER = new ThreadLocal<>();

    private TreblleMetadata() {
    }

    /**
     * Adds a metadata entry to the current request. Accepts String, Number, or Boolean values.
     * Invalid or oversized entries are silently ignored to keep the payload schema-valid.
     */
    public static void add(String key, Object value) {
        try {
            if (key == null || key.isBlank() || key.length() > MAX_KEY_LENGTH) {
                return;
            }
            if (!isValidValue(value)) {
                return;
            }
            Map<String, Object> map = HOLDER.get();
            if (map == null) {
                map = new LinkedHashMap<>();
                HOLDER.set(map);
            }
            if (map.size() >= MAX_ENTRIES && !map.containsKey(key)) {
                return;
            }
            map.put(key, value);
        } catch (Throwable ignored) {
            // Never surface SDK errors to the host application.
        }
    }

    private static boolean isValidValue(Object value) {
        if (value instanceof String s) {
            return s.length() <= MAX_STRING_VALUE_LENGTH;
        }
        return value instanceof Number || value instanceof Boolean;
    }

    /**
     * @return an unmodifiable snapshot of the current thread's metadata, or an empty map.
     */
    public static Map<String, Object> snapshot() {
        Map<String, Object> map = HOLDER.get();
        if (map == null || map.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(map));
    }

    /** Clears metadata for the current thread. Called by the SDK when the request completes. */
    public static void clear() {
        HOLDER.remove();
    }
}
