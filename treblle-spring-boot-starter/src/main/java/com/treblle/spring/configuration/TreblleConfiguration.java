package com.treblle.spring.configuration;

import jakarta.servlet.http.HttpServletRequest;

public interface TreblleConfiguration {

    boolean shouldProcess(HttpServletRequest servletRequest);

}
