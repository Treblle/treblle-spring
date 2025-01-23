package com.treblle.javax.configuration;

import com.treblle.common.configuration.TreblleProperties;

import javax.servlet.FilterConfig;
import java.util.*;

public class ServletFilterTreblleProperties implements TreblleProperties {

    private static final String ENDPOINT = "endpoint";
    private static final String API_KEY = "apiKey";
    private static final String PROJECT_ID = "projectId";
    private static final String DEBUG = "debug";
    private static final String URL_PATTERNS = "urlPatterns";
    private static final String MASKING_KEYWORDS = "maskingKeywords";

    private final FilterConfig filterConfig;

    public ServletFilterTreblleProperties(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

    @Override
    public String getEndpoint() {
        return filterConfig.getInitParameter(ENDPOINT);
    }

    @Override
    public String getApiKey() {
        return filterConfig.getInitParameter(API_KEY);
    }

    @Override
    public String getProjectId() {
        return filterConfig.getInitParameter(PROJECT_ID);
    }

    @Override
    public boolean isDebug() {
        return Optional.ofNullable(filterConfig.getInitParameter(DEBUG)).map(Boolean::parseBoolean).orElse(false);
    }

    @Override
    public List<String> getUrlPatterns() {
        return Optional.ofNullable(filterConfig.getInitParameter(URL_PATTERNS)).map(patterns -> Arrays.asList(patterns.split(","))).orElse(Collections.emptyList());
    }

    @Override
    public List<String> getMaskingKeywords() {
        return Optional.ofNullable(filterConfig.getInitParameter(MASKING_KEYWORDS)).map(keywords -> Arrays.asList(keywords.split(","))).orElse(Collections.emptyList());
    }

}
