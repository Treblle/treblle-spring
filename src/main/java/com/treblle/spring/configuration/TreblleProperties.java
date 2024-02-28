package com.treblle.spring.configuration;

import java.util.Collections;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;

@ConfigurationProperties(prefix = "treblle")
public class TreblleProperties {

  private String endpoint;
  private String apiKey;
  private String projectId;
  private Integer filterOrder = Ordered.LOWEST_PRECEDENCE - 10; // Similar to HttpTraceFilter
  private List<String> urlPatterns = Collections.emptyList();
  private List<String> maskingKeywords = Collections.emptyList();

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

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

  public Integer getFilterOrder() {
    return filterOrder;
  }

  public void setFilterOrder(Integer filterOrder) {
    this.filterOrder = filterOrder;
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
