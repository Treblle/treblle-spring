package com.treblle.lambda.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.treblle.common.infrastructure.RequestWrapper;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Optional;

public class StreamRequestContextWrapper implements RequestWrapper {

    private final JsonNode request;
    private final Map<String, JsonNode> headers;
    private final String path;

    public StreamRequestContextWrapper(JsonNode request, String path) {
        this.request = request;
        this.headers = Optional.ofNullable(request.path("headers")).map(JsonNode::fields).map(ContextUtils::iteratorToMap).orElse(Collections.emptyMap());
        this.path = path;
    }

    @Override
    public String getProtocol() {
        return "http";
    }

    @Override
    public String getMethod() {
        return Optional.ofNullable(request.path("httpMethod")).map(JsonNode::asText).orElse(null);
    }

    @Override
    public String getUrl() {
        return constructUrl();
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(headers.keySet());
    }

    @Override
    public String getHeader(String header) {
        return Optional.ofNullable(headers.get(header)).map(JsonNode::asText).orElse(null);
    }

    @Override
    public String getRemoteAddr() {
        return null;
    }

    @Override
    public String getServerAddr() {
        return null;
    }

    private String getPath() {
        return Optional.ofNullable(path).filter(it -> !it.isEmpty())
                .orElse(Optional.ofNullable(request.path("path")).map(JsonNode::asText).orElse(null));
    }

    public String constructUrl() {
        String host = Optional.ofNullable(getHeader("Host")).orElse("localhost");
        String path = getPath();
        try {
            URI uri = new URI(getProtocol(), host, path, null, null);
            return uri.toString();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Error constructing URL", e);
        }
    }

}
