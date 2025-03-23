package com.treblle.spring.infrastructure;

import com.treblle.common.infrastructure.RequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

public class SpringRequestWrapper extends ContentCachingRequestWrapper implements RequestWrapper {

    private final Environment environment;
    private final UriComponents uriComponents;

    public SpringRequestWrapper(HttpServletRequest request, Environment environment) {
        super(request);
        this.environment = environment;
        this.uriComponents = ServletUriComponentsBuilder.fromRequestUri(request).build();
    }

    public SpringRequestWrapper(HttpServletRequest request, int contentCacheLimit, Environment environment) {
        super(request, contentCacheLimit);
        this.environment = environment;
        this.uriComponents = ServletUriComponentsBuilder.fromRequestUri(request).build();
    }

    @Override
    public String getUrl() {
        return uriComponents.toUriString();
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
