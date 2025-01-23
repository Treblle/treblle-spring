package com.treblle.common.dto;

import java.util.List;

public class Data {

  private Server server;
  private Language language;
  private Request request;
  private Response response;
  private List<RuntimeError> errors;

  public Server getServer() {
    return server;
  }

  public void setServer(Server server) {
    this.server = server;
  }

  public Language getLanguage() {
    return language;
  }

  public void setLanguage(Language language) {
    this.language = language;
  }

  public Request getRequest() {
    return request;
  }

  public void setRequest(Request request) {
    this.request = request;
  }

  public Response getResponse() {
    return response;
  }

  public void setResponse(Response response) {
    this.response = response;
  }

  public List<RuntimeError> getErrors() {
    return errors;
  }

  public void setErrors(List<RuntimeError> errors) {
    this.errors = errors;
  }
}
