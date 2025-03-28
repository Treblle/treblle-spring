package com.treblle.common.configuration;

import java.util.List;

public interface TreblleProperties {

    default String getEndpoint() {
        return null;
    }

    String getApiKey();

    String getProjectId();
    
    default boolean isDebug() {
        return false;
    }

    default List<String> getUrlPatterns() {
        return List.of();
    }

    default List<String> getMaskingKeywords() {
        return List.of();
    }

    default int getConnectTimeoutInSeconds() {
        return 3;
    }

    default int getReadTimeoutInSeconds() {
        return 3;
    }

}
