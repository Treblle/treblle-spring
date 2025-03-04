package com.treblle.lambda.configuration;

import com.treblle.common.configuration.TreblleProperties;

import java.util.List;

public class TrebllePropertiesBuilder {

    private String endpoint;
    private String apiKey;
    private String projectId;
    private boolean debug = false;
    private List<String> urlPatterns;
    private List<String> maskingKeywords;

    public TrebllePropertiesBuilder(String apiKey, String projectId) {
        this.apiKey = apiKey;
        this.projectId = projectId;
    }

    private TrebllePropertiesBuilder() {
    }

    public TrebllePropertiesBuilder setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public TrebllePropertiesBuilder setDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public TrebllePropertiesBuilder setUrlPatterns(List<String> urlPatterns) {
        this.urlPatterns = urlPatterns;
        return this;
    }

    public TrebllePropertiesBuilder setMaskingKeywords(List<String> maskingKeywords) {
        this.maskingKeywords = maskingKeywords;
        return this;
    }

    public TreblleProperties build() {
        return new TrebllePropertiesImpl(endpoint, apiKey, projectId, debug, urlPatterns, maskingKeywords);
    }

    private static class TrebllePropertiesImpl implements TreblleProperties {

        private final String endpoint;
        private final String apiKey;
        private final String projectId;
        private final boolean debug;
        private final List<String> urlPatterns;
        private final List<String> maskingKeywords;

        private TrebllePropertiesImpl(String endpoint, String apiKey, String projectId, boolean debug, List<String> urlPatterns, List<String> maskingKeywords) {
            this.endpoint = endpoint;
            this.apiKey = apiKey;
            this.projectId = projectId;
            this.debug = debug;
            this.urlPatterns = urlPatterns;
            this.maskingKeywords = maskingKeywords;
        }

        @Override
        public String getEndpoint() {
            return endpoint;
        }

        @Override
        public String getApiKey() {
            return apiKey;
        }

        @Override
        public String getProjectId() {
            return projectId;
        }

        @Override
        public boolean isDebug() {
            return debug;
        }

        @Override
        public List<String> getUrlPatterns() {
            return urlPatterns;
        }

        @Override
        public List<String> getMaskingKeywords() {
            return maskingKeywords;
        }

    }

}
