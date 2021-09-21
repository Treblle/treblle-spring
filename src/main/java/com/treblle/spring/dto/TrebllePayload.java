package com.treblle.spring.dto;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TrebllePayload {

  private static final String TREBLLE_VERSION = "0.6";
  private static final String SDK_NAME = "spring-boot";

  @JsonProperty("api_key")
  private String apiKey;

  @JsonProperty("project_id")
  private String projectId;

  private Data data;

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public String getProjectId() {
    return projectId;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  @JsonGetter("version")
  public String getVersion() {
    return TREBLLE_VERSION;
  }

  @JsonGetter("sdk")
  public String getSdk() {
    return SDK_NAME;
  }

  public Data getData() {
    return data;
  }

  public void setData(Data data) {
    this.data = data;
  }
}
