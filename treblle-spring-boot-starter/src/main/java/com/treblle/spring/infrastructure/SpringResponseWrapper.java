package com.treblle.spring.infrastructure;

import com.treblle.common.infrastructure.ResponseWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.util.ContentCachingResponseWrapper;

public class SpringResponseWrapper extends ContentCachingResponseWrapper implements ResponseWrapper {

    public SpringResponseWrapper(HttpServletResponse response) {
        super(response);
    }

}
