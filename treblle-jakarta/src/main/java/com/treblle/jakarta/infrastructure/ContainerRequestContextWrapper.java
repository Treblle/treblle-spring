package com.treblle.jakarta.infrastructure;

import com.treblle.common.infrastructure.RequestWrapper;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedMap;

import java.util.Collections;
import java.util.Enumeration;

public class ContainerRequestContextWrapper implements RequestWrapper {

    private final ContainerRequestContext containerRequestContext;

    public ContainerRequestContextWrapper(ContainerRequestContext containerRequestContext) {
        this.containerRequestContext = containerRequestContext;
    }

    @Override
    public String getProtocol() {
        return containerRequestContext.getUriInfo().getRequestUri().getScheme();
    }

    @Override
    public String getMethod() {
        return containerRequestContext.getMethod();
    }

    @Override
    public String getUrl() {
        return containerRequestContext.getUriInfo().getRequestUri().toString();
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        MultivaluedMap<String, String> headers = containerRequestContext.getHeaders();
        return Collections.enumeration(headers.keySet());    }

    @Override
    public String getHeader(String header) {
        return containerRequestContext.getHeaderString(header);
    }

    @Override
    public String getRemoteAddr() {
        return containerRequestContext.getHeaders().getFirst("X-Forwarded-For");
    }

    @Override
    public String getServerAddr() {
        return null;
    }

}
