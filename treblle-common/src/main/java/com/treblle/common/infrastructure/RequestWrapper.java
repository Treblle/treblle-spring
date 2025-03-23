package com.treblle.common.infrastructure;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

public interface RequestWrapper {

    String getProtocol();

    String getMethod();

    String getUrl();

    Enumeration<String> getHeaderNames();

    String getHeader(String header);

    String getRemoteAddr();

    String getServerAddr();

    default Map<String, String> getQueryParams() {
        return Collections.emptyMap();
    }

}
