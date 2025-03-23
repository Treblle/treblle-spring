package com.treblle.lambda.infrastructure;

import com.amazonaws.services.lambda.runtime.events.APIGatewayCustomAuthorizerEvent;
import com.treblle.common.infrastructure.RequestWrapper;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class APIGatewayCustomAuthorizerEventRequestWrapper implements RequestWrapper {

    private static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    private final APIGatewayCustomAuthorizerEvent request;
    private final String path;

    public APIGatewayCustomAuthorizerEventRequestWrapper(APIGatewayCustomAuthorizerEvent request, String path) {
        this.request = request;
        this.path = path;
    }

    @Override
    public String getProtocol() {
        return getHeaders().getOrDefault(X_FORWARDED_PROTO, "http");
    }

    @Override
    public String getMethod() {
        return request.getHttpMethod();
    }

    @Override
    public String getUrl() {
        return constructUrl(request);
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
                .map(APIGatewayCustomAuthorizerEvent.RequestContext::getIdentity)
                .map(APIGatewayCustomAuthorizerEvent.Identity::getSourceIp)
                .orElse(getHeaders().getOrDefault(X_FORWARDED_FOR, null));
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

    private String getPath() {
        String potentialPath = Optional.ofNullable(path).filter(it -> !it.isEmpty()).orElse(request.getPath());
        return potentialPath.startsWith("/") ? potentialPath : "/" + potentialPath;
    }

    public String constructUrl(APIGatewayCustomAuthorizerEvent event) {
        String host = getHeaders().getOrDefault("Host", "localhost");
        String path = getPath();
        String queryString = null;
        Map<String, String> queryParams = event.getQueryStringParameters();
        if (queryParams != null && !queryParams.isEmpty()) {
            queryString = queryParams.entrySet()
                    .stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining("&"));
        }
        try {
            URI uri = new URI(getProtocol(), host, path, queryString, null);
            return uri.toString();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Error constructing URL", e);
        }
    }

}
