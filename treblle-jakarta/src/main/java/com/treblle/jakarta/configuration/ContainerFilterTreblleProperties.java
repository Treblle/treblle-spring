package com.treblle.jakarta.configuration;

import com.treblle.common.configuration.TreblleProperties;
import jakarta.ws.rs.core.Configuration;

import java.util.List;
import java.util.Optional;

public class ContainerFilterTreblleProperties implements TreblleProperties {

    private static final String ENDPOINT = "endpoint";
    private static final String API_KEY = "apiKey";
    private static final String PROJECT_ID = "projectId";
    private static final String DEBUG = "debug";
    private static final String URL_PATTERNS = "urlPatterns";
    private static final String MASKING_KEYWORDS = "maskingKeywords";

    private final Configuration filterConfig;

    public ContainerFilterTreblleProperties(Configuration filterConfig) {
        this.filterConfig = filterConfig;
    }

    @Override
    public String getEndpoint() {
        return (String) filterConfig.getProperty(ENDPOINT);
    }

    @Override
    public String getApiKey() {
        return (String) filterConfig.getProperty(API_KEY);
    }

    @Override
    public String getProjectId() {
        return (String) filterConfig.getProperty(PROJECT_ID);
    }

    @Override
    public boolean isDebug() {
        return Optional.ofNullable((String) filterConfig.getProperty(DEBUG)).map(Boolean::parseBoolean).orElse(false);
    }

    @Override
    public List<String> getUrlPatterns() {
        return Optional.ofNullable((String) filterConfig.getProperty(URL_PATTERNS)).map(patterns -> List.of(patterns.split(","))).orElse(List.of());
    }

    @Override
    public List<String> getMaskingKeywords() {
        return Optional.ofNullable((String) filterConfig.getProperty(MASKING_KEYWORDS)).map(keywords -> List.of(keywords.split(","))).orElse(List.of());
    }

}
