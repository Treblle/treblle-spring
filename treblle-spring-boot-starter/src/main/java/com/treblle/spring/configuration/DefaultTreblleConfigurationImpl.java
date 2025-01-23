package com.treblle.spring.configuration;

import jakarta.servlet.http.HttpServletRequest;

public class DefaultTreblleConfigurationImpl implements TreblleConfiguration {

    @Override
    public boolean shouldProcess(HttpServletRequest servletRequest) {
        return true;
    }

}
