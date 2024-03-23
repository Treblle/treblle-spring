package com.treblle.spring.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface TreblleConfiguration {

    boolean shouldProcess(HttpServletRequest servletRequest);

}
