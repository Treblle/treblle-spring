package com.treblle.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public class Response {

  private Map<String, String> headers;
  private Integer code;
  private Long size;

  @JsonProperty("load_time")
  private Double loadTime;

  private JsonNode body;

  public Map<String, String> getHeaders() {
    return headers;
  }

  public void setHeaders(Map<String, String> headers) {
    this.headers = headers;
  }

  public Integer getCode() {
    return code;
  }

  public void setCode(Integer code) {
    this.code = code;
  }

  public Long getSize() {
    return size;
  }

  public void setSize(Long size) {
    this.size = size;
  }

  public Double getLoadTime() {
    return loadTime;
  }

  public void setLoadTime(Double loadTime) {
    this.loadTime = loadTime;
  }

  public JsonNode getBody() {
    return body;
  }

  public void setBody(JsonNode body) {
    this.body = body;
  }
}
