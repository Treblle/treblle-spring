package com.treblle.springboot.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.treblle.springboot.collector.RawRequestData;
import com.treblle.springboot.config.TreblleProperties;
import com.treblle.springboot.core.model.Data;
import com.treblle.springboot.core.model.LanguageInfo;
import com.treblle.springboot.core.model.RequestInfo;
import com.treblle.springboot.core.model.ResponseInfo;
import com.treblle.springboot.core.model.ServerInfo;
import com.treblle.springboot.core.model.TrebllePayload;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Framework-agnostic factory that normalizes raw request/response data into a schema-valid
 * {@link TrebllePayload}. Pure: no Spring imports, no thread-local access, unit-testable in
 * isolation. Everything it needs — including the errors and metadata snapshots — is carried on the
 * {@link RawRequestData} it receives, so it can safely run off the request thread.
 */
public class PayloadFactory {

    /** Treblle enforces a 2 MB limit on request and response bodies. */
    public static final int MAX_BODY_SIZE = 2 * 1024 * 1024;

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    private static final Set<String> ALLOWED_METHODS =
            Set.of("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS");

    private final ObjectMapper objectMapper;
    private final MaskingService maskingService;
    private final TreblleProperties properties;
    private final String sdkName;
    private final int sdkVersion;

    public PayloadFactory(ObjectMapper objectMapper,
                          MaskingService maskingService,
                          TreblleProperties properties,
                          String sdkName,
                          int sdkVersion) {
        this.objectMapper = objectMapper;
        this.maskingService = maskingService;
        this.properties = properties;
        this.sdkName = sdkName;
        this.sdkVersion = sdkVersion;
    }

    public TrebllePayload build(RawRequestData raw) {
        TrebllePayload payload = new TrebllePayload();
        payload.setSdkToken(properties.getSdkToken());
        payload.setApiKey(properties.getApiKey());
        payload.setSdk(sdkName);
        payload.setVersion(sdkVersion);

        Data data = new Data();
        data.setServer(buildServer(raw));
        data.setLanguage(buildLanguage());
        data.setRequest(buildRequest(raw));
        data.setResponse(buildResponse(raw));
        data.setErrors(raw.getErrors());
        data.setQueries(Collections.emptyList());

        Map<String, Object> metadata = raw.getMetadata();
        if (metadata != null && !metadata.isEmpty()) {
            data.setMetadata(metadata);
        }

        payload.setData(data);
        return payload;
    }

    private ServerInfo buildServer(RawRequestData raw) {
        ServerInfo server = new ServerInfo();
        server.setIp(normalizeIp(raw.getServerIp()));
        server.setTimezone("UTC");
        server.setProtocol(raw.getProtocol());
        server.setSoftware(raw.getServerSoftware());

        ServerInfo.Os os = server.getOs();
        os.setName(System.getProperty("os.name"));
        os.setRelease(System.getProperty("os.version"));
        os.setArchitecture(System.getProperty("os.arch"));
        return server;
    }

    private LanguageInfo buildLanguage() {
        LanguageInfo language = new LanguageInfo();
        language.setName("java");
        language.setVersion(System.getProperty("java.version"));
        return language;
    }

    private RequestInfo buildRequest(RawRequestData raw) {
        RequestInfo request = new RequestInfo();
        request.setTimestamp(TIMESTAMP.format(ZonedDateTime.now(ZoneOffset.UTC)));
        request.setIp(normalizeIp(raw.getClientIp()));
        request.setUrl(raw.getUrl() == null ? "" : raw.getUrl());
        request.setUserAgent(raw.getUserAgent() == null ? "" : raw.getUserAgent());
        request.setMethod(normalizeMethod(raw.getMethod()));
        request.setRoutePath(raw.getRoutePath());

        request.setQuery(raw.getQueryParams());
        request.setHeaders(raw.getRequestHeaders());
        maskingService.maskStringMap(request.getHeaders());
        maskingService.maskStringMap(request.getQuery());

        request.setBody(buildBody(raw.getRequestBody(), raw.getRequestContentType(),
                raw.getRequestContentEncoding(), raw.getUploadedFiles()));
        return request;
    }

    private ResponseInfo buildResponse(RawRequestData raw) {
        ResponseInfo response = new ResponseInfo();
        response.setCode(normalizeCode(raw.getStatusCode()));
        response.setLoadTime(raw.getLoadTimeMs());
        response.setHeaders(raw.getResponseHeaders());
        maskingService.maskStringMap(response.getHeaders());

        byte[] body = raw.getResponseBody();
        long size = raw.getResponseSize() > 0 ? raw.getResponseSize() : (body == null ? 0 : body.length);
        response.setSize(size);
        response.setBody(buildBody(body, raw.getResponseContentType(),
                raw.getResponseContentEncoding(), Collections.emptyList()));
        return response;
    }

    /**
     * Turns a raw body into schema-valid JSON, honoring all edge cases:
     * file uploads, compressed bodies, oversized bodies, form/urlencoded conversion, and non-JSON
     * payloads.
     */
    private Object buildBody(byte[] body, String contentType, String contentEncoding,
                             List<RawRequestData.FilePart> files) {
        try {
            boolean hasFiles = files != null && !files.isEmpty();
            String ct = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);

            // Never capture file uploads — describe them instead.
            if (hasFiles || ct.startsWith("multipart/")) {
                return describeFiles(files);
            }

            // Compressed bodies aren't decodable text; describe instead of reporting "invalid JSON".
            if (isCompressed(contentEncoding)) {
                ObjectNode message = objectMapper.createObjectNode();
                message.put("message", "Body omitted (compressed: "
                        + contentEncoding.trim().toLowerCase(Locale.ROOT) + ").");
                return message;
            }

            if (body == null || body.length == 0) {
                return objectMapper.createObjectNode();
            }

            if (body.length > MAX_BODY_SIZE) {
                ObjectNode message = objectMapper.createObjectNode();
                message.put("message", "Payload too large to capture.");
                message.put("size", body.length);
                return message;
            }

            // form-data / urlencoded → convert to JSON
            if (ct.startsWith("application/x-www-form-urlencoded")) {
                return parseUrlEncoded(new String(body, StandardCharsets.UTF_8));
            }

            // Attempt JSON parse; anything else is reported as invalid JSON.
            String text = new String(body, StandardCharsets.UTF_8);
            try {
                JsonNode node = objectMapper.readTree(text);
                if (node == null || node.isMissingNode()) {
                    return invalidJson();
                }
                return maskingService.maskBody(node);
            } catch (Exception notJson) {
                return invalidJson();
            }
        } catch (Throwable t) {
            return invalidJson();
        }
    }

    private ObjectNode describeFiles(List<RawRequestData.FilePart> files) {
        ObjectNode root = objectMapper.createObjectNode();
        if (files == null || files.isEmpty()) {
            root.put("message", "File upload omitted.");
            return root;
        }
        var arr = objectMapper.createArrayNode();
        for (RawRequestData.FilePart file : files) {
            ObjectNode f = objectMapper.createObjectNode();
            f.put("name", file.name() == null ? "" : file.name());
            f.put("size", file.size());
            f.put("mime", file.mime() == null ? "" : file.mime());
            arr.add(f);
        }
        root.set("files", arr);
        return root;
    }

    private ObjectNode parseUrlEncoded(String raw) {
        ObjectNode node = objectMapper.createObjectNode();
        UrlEncodedParser.parse(raw).forEach(node::put); // last value wins
        return (ObjectNode) maskingService.maskBody(node);
    }

    private boolean isCompressed(String contentEncoding) {
        if (contentEncoding == null || contentEncoding.isBlank()) {
            return false;
        }
        String enc = contentEncoding.toLowerCase(Locale.ROOT);
        return enc.contains("gzip") || enc.contains("deflate")
                || enc.contains("br") || enc.contains("compress") || enc.contains("zstd");
    }

    private ObjectNode invalidJson() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("message", "The payload is not valid JSON.");
        return node;
    }

    private String normalizeMethod(String method) {
        if (method == null) {
            return "GET";
        }
        String upper = method.toUpperCase(Locale.ROOT);
        return ALLOWED_METHODS.contains(upper) ? upper : "GET";
    }

    private int normalizeCode(int code) {
        if (code < 100) {
            return 200;
        }
        return Math.min(code, 599);
    }

    private String normalizeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "bogon";
        }
        return ip;
    }
}
