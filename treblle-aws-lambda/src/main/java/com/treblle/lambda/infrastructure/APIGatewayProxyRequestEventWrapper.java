package com.treblle.lambda.infrastructure;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.treblle.common.infrastructure.RequestWrapper;

import java.net.InetAddress;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Optional;

public class APIGatewayProxyRequestEventWrapper implements RequestWrapper {

    private final APIGatewayProxyRequestEvent request;

    public APIGatewayProxyRequestEventWrapper(APIGatewayProxyRequestEvent request) {
        this.request = request;
    }

    @Override
    public String getProtocol() {
        return getHeaders().getOrDefault("X-Forwarded-Proto", "http");
    }

    @Override
    public String getMethod() {
        return request.getHttpMethod();
    }

    @Override
    public String getUrl() {
        return request.getPath();
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(getHeaders().keySet());
    }

    @Override
    public String getHeader(String header) {
        return getHeaders().get(header);
    }

    @Override
    public String getRemoteAddr() {
        return Optional.ofNullable(request.getRequestContext())
                .map(APIGatewayProxyRequestEvent.ProxyRequestContext::getIdentity)
                .map(APIGatewayProxyRequestEvent.RequestIdentity::getSourceIp)
                .orElse(getHeaders().getOrDefault("X-Forwarded-For", null));
    }

    @Override
    public String getServerAddr() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, String> getHeaders() {
        return Optional.ofNullable(request.getHeaders()).orElse(Collections.emptyMap());
    }

}
