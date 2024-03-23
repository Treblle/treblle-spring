package com.treblle.spring.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class DefaultTreblleConfigurationImpl implements TreblleConfiguration {

    @Override
    public boolean shouldProcess(HttpServletRequest servletRequest) {
        return true;
    }

}
