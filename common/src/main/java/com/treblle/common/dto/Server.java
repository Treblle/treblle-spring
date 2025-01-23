package com.treblle.common.dto;

public class Server {

  private String ip;
  private String timezone;
  private String software;
  private String signature;
  private String protocol;
  private OperatingSystem os;

  public String getIp() {
    return ip;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }

  public String getTimezone() {
    return timezone;
  }

  public void setTimezone(String timezone) {
    this.timezone = timezone;
  }

  public String getSoftware() {
    return software;
  }

  public void setSoftware(String software) {
    this.software = software;
  }

  public String getSignature() {
    return signature;
  }

  public void setSignature(String signature) {
    this.signature = signature;
  }

  public String getProtocol() {
    return protocol;
  }

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  public OperatingSystem getOs() {
    return os;
  }

  public void setOs(OperatingSystem os) {
    this.os = os;
  }
}
