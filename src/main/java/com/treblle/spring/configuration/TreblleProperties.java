package com.treblle.spring.configuration;

import java.util.Collections;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "treblle")
public class TreblleProperties {

  private String apiKey;
  private String projectId;
  private List<String> urlPatterns = Collections.emptyList();
  private List<String> maskingKeywords = Collections.emptyList();

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

  public List<String> getUrlPatterns() {
    return urlPatterns;
  }

  public void setUrlPatterns(List<String> urlPatterns) {
    this.urlPatterns = urlPatterns;
  }

  public List<String> getMaskingKeywords() {
    return maskingKeywords;
  }

  public void setMaskingKeywords(List<String> maskingKeywords) {
    this.maskingKeywords = maskingKeywords;
  }
}
