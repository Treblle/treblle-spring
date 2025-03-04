package com.treblle.lambda.infrastructure;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.treblle.common.infrastructure.ResponseWrapper;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class APIGatewayProxyResponseEventWrapper implements ResponseWrapper {

    private final APIGatewayProxyResponseEvent response;

    public APIGatewayProxyResponseEventWrapper(APIGatewayProxyResponseEvent response) {
        this.response = response;
    }

    @Override
    public int getStatus() {
        return Optional.ofNullable(response).map(APIGatewayProxyResponseEvent::getStatusCode).orElse(500);
    }

    @Override
    public Collection<String> getHeaderNames() {
        return getHeaders().keySet();
    }

    @Override
    public String getHeader(String header) {
        return getHeaders().get(header);
    }

    private Map<String, String> getHeaders() {
        return Optional.ofNullable(response).map(APIGatewayProxyResponseEvent::getHeaders).orElse(Collections.emptyMap());
    }

}
