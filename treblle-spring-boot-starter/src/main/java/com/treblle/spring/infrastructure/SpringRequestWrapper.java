package com.treblle.spring.infrastructure;

import com.treblle.common.infrastructure.RequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;

import java.util.Enumeration;
import java.util.Map;

public class SpringRequestWrapper implements RequestWrapper {

    private final HttpServletRequest request;
    private final Environment environment;
    private final UriComponents uriComponents;

    public SpringRequestWrapper(HttpServletRequest request, Environment environment) {
        this.request = request;
        this.environment = environment;
        this.uriComponents = ServletUriComponentsBuilder.fromRequestUri(request).build();
    }

    @Override
    public String getProtocol() {
        return request.getProtocol();
    }

    @Override
    public String getMethod() {
        return request.getMethod();
    }

    @Override
    public String getUrl() {
        return uriComponents.toUriString();
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return request.getHeaderNames();
    }

    @Override
    public String getHeader(String header) {
        return request.getHeader(header);
    }

    @Override
    public String getRemoteAddr() {
        return request.getRemoteAddr();
    }

    @Override
    public String getServerAddr() {
        return environment.getProperty("server.address");
    }

    @Override
    public Map<String, String> getQueryParams() {
        return uriComponents.getQueryParams().toSingleValueMap();
    }

}
