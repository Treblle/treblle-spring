package com.treblle.lambda.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.treblle.common.infrastructure.ResponseWrapper;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class StreamResponseContextWrapper implements ResponseWrapper {

    private final JsonNode response;
    private final Map<String, JsonNode> headers;

    public StreamResponseContextWrapper(JsonNode response) {
        this.response = response;
        this.headers = Optional.ofNullable(response.path("headers")).map(JsonNode::fields).map(ContextUtils::iteratorToMap).orElse(Collections.emptyMap());
    }

    @Override
    public int getStatus() {
        return Optional.ofNullable(response.path("statusCode")).map(JsonNode::asInt).orElse(200);
    }

    @Override
    public Collection<String> getHeaderNames() {
        return headers.keySet();
    }

    @Override
    public String getHeader(String header) {
        return Optional.ofNullable(headers.get(header)).map(JsonNode::asText).orElse(null);
    }

}
