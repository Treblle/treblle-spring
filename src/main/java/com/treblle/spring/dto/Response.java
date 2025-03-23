package com.treblle.spring.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

public class Response {

  private Map<String, String> headers;
  private Integer code;
  private Long size;
  private Long load_time;
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

  public Long getLoad_time() {
    return load_time;
  }

  public void setLoad_time(Long load_time) {
    this.load_time = load_time;
  }

  public JsonNode getBody() {
    return body;
  }

  public void setBody(JsonNode body) {
    this.body = body;
  }

}
