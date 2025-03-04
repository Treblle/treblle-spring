package com.treblle.lambda.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.treblle.common.infrastructure.RequestWrapper;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Optional;

public class StreamRequestContextWrapper implements RequestWrapper {

    private final JsonNode request;
    private final Map<String, JsonNode> headers;

    public StreamRequestContextWrapper(JsonNode request) {
        this.request = request;
        this.headers = Optional.ofNullable(request.path("headers")).map(JsonNode::fields).map(ContextUtils::iteratorToMap).orElse(Collections.emptyMap());
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
        return Optional.ofNullable(request.path("path")).map(JsonNode::asText).orElse(null);
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
        return "";
    }

    @Override
    public String getServerAddr() {
        return "";
    }

}
