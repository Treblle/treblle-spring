package com.treblle.common.infrastructure;

import java.util.Collection;

public interface ResponseWrapper {

    int getStatus();

    Collection<String> getHeaderNames();

    String getHeader(String header);

}
