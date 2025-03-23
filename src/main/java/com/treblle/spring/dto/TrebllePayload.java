package com.treblle.spring.dto;

public class TrebllePayload {

  private static final Float TREBLLE_VERSION = 1.0f;
  private static final String SDK_NAME = "spring-boot";

  private String api_key;
  private String project_id;
  private String sdk = SDK_NAME;
  private Float version = TREBLLE_VERSION;
  private Data data;

  public String getApi_key() {
    return api_key;
  }

  public void setApi_key(String api_key) {
    this.api_key = api_key;
  }

  public String getProject_id() {
    return project_id;
  }

  public void setProject_id(String project_id) {
    this.project_id = project_id;
  }

  public String getSdk() {
    return sdk;
  }

  public void setSdk(String sdk) {
    this.sdk = sdk;
  }

  public Float getVersion() {
    return version;
  }

  public void setVersion(Float version) {
    this.version = version;
  }

  public Data getData() {
    return data;
  }

  public void setData(Data data) {
    this.data = data;
  }

}
