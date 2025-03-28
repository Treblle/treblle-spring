package com.treblle.spring.infrastructure;

import com.treblle.common.infrastructure.ResponseWrapper;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Collection;

public class SpringResponseWrapper implements ResponseWrapper {

    private final HttpServletResponse response;

    public SpringResponseWrapper(HttpServletResponse response) {
        this.response = response;
    }


    @Override
    public int getStatus() {
        return response.getStatus();
    }

    @Override
    public Collection<String> getHeaderNames() {
        return response.getHeaderNames();
    }

    @Override
    public String getHeader(String header) {
        return response.getHeader(header);
    }

}
