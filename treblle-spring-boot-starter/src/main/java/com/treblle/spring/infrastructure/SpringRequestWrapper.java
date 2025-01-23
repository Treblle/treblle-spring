package com.treblle.spring.infrastructure;

import com.treblle.common.infrastructure.RequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.ContentCachingRequestWrapper;

public class SpringRequestWrapper extends ContentCachingRequestWrapper implements RequestWrapper {

    private final Environment environment;

    public SpringRequestWrapper(HttpServletRequest request, Environment environment) {
        super(request);
        this.environment = environment;
    }

    public SpringRequestWrapper(HttpServletRequest request, int contentCacheLimit, Environment environment) {
        super(request, contentCacheLimit);
        this.environment = environment;
    }

    @Override
    public String getUrl() {
        return ServletUriComponentsBuilder.fromRequestUri((HttpServletRequest) getRequest()).toUriString();
    }

    @Override
    public String getServerAddr() {
        return environment.getProperty("server.address");
    }

}
