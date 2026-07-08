package com.treblle.springboot.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The {@code data.response} object.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public class ResponseInfo {

    private Map<String, String> headers = new LinkedHashMap<>();
    private int code = 200;
    private double size = 0;

    @JsonProperty("load_time")
    private double loadTime = 0;

    /** Object, array, or scalar — whatever valid JSON the API returned. */
    private Object body = new LinkedHashMap<>();

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public double getSize() {
        return size;
    }

    public void setSize(double size) {
        this.size = size;
    }

    public double getLoadTime() {
        return loadTime;
    }

    public void setLoadTime(double loadTime) {
        this.loadTime = loadTime;
    }

    public Object getBody() {
        return body;
    }

    public void setBody(Object body) {
        this.body = body;
    }
}
