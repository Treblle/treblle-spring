package com.treblle.common.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public class Request {

  private String timestamp;
  private String ip;
  private String url;
  private String user_agent;
  private String method;
  private Map<String, String> headers;
  private JsonNode body;
  private Map<String, String> query;

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  public String getIp() {
    return ip;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getUser_agent() {
    return user_agent;
  }

  public void setUser_agent(String user_agent) {
    this.user_agent = user_agent;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public void setHeaders(Map<String, String> headers) {
    this.headers = headers;
  }

  public JsonNode getBody() {
    return body;
  }

  public void setBody(JsonNode body) {
    this.body = body;
  }

  public Map<String, String> getQuery() {
    return query;
  }

  public void setQuery(Map<String, String> query) {
    this.query = query;
  }

}
