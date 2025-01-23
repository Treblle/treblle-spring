package com.treblle.spring.configuration;

import com.treblle.common.configuration.TreblleProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;

import java.util.Collections;
import java.util.List;

@ConfigurationProperties(prefix = "treblle")
public class SpringTreblleProperties implements TreblleProperties {

  private String endpoint;
  private String apiKey;
  private String projectId;
  private Integer filterOrder = Ordered.LOWEST_PRECEDENCE - 10; // Similar to HttpTraceFilter
  private boolean debug = false;
  private List<String> urlPatterns = Collections.emptyList();
  private List<String> maskingKeywords = Collections.emptyList();

  @Override
  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  @Override
  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  @Override
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

  @Override
  public boolean isDebug() {
    return debug;
  }

  public void setDebug(boolean debug) {
    this.debug = debug;
  }

  @Override
  public List<String> getUrlPatterns() {
    return urlPatterns;
  }

  public void setUrlPatterns(List<String> urlPatterns) {
    this.urlPatterns = urlPatterns;
  }

  @Override
  public List<String> getMaskingKeywords() {
    return maskingKeywords;
  }

  public void setMaskingKeywords(List<String> maskingKeywords) {
    this.maskingKeywords = maskingKeywords;
  }

}
