package com.treblle.javax.infrastructure;

import com.treblle.common.infrastructure.ResponseWrapper;

import javax.ws.rs.container.ContainerResponseContext;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ContainerResponseContextWrapper implements ResponseWrapper {

    private final ContainerResponseContext containerResponseContext;

    public ContainerResponseContextWrapper(ContainerResponseContext containerResponseContext) {
        this.containerResponseContext = containerResponseContext;
    }

    @Override
    public int getStatus() {
        return containerResponseContext.getStatus();
    }

    @Override
    public Collection<String> getHeaderNames() {
        return containerResponseContext.getHeaders().keySet();
    }

    @Override
    public String getHeader(String header) {
        List<Object> headerValues = containerResponseContext.getHeaders().get(header);
        if (headerValues != null) {
            return headerValues.stream().map(Object::toString).collect(Collectors.joining(", "));
        }
        return null;
    }

}
