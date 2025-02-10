package com.treblle.common.infrastructure;

import java.util.Enumeration;

public interface RequestWrapper {

    String getProtocol();

    String getMethod();

    String getUrl();

    Enumeration<String> getHeaderNames();

    String getHeader(String header);

    String getRemoteAddr();

    String getServerAddr();

}
