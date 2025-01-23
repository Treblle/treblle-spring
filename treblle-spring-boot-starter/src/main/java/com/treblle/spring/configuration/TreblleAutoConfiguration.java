package com.treblle.spring.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.treblle.common.service.TreblleService;
import com.treblle.common.utils.DataMasker;
import com.treblle.common.utils.DataMaskerImpl;
import com.treblle.spring.TreblleFilter;
import com.treblle.spring.service.TreblleServiceImpl;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
@EnableConfigurationProperties(SpringTreblleProperties.class)
public class TreblleAutoConfiguration {

  private final SpringTreblleProperties treblleProperties;

  private final ObjectMapper objectMapper;

  private final RestTemplateBuilder restTemplateBuilder;

  public TreblleAutoConfiguration(
          SpringTreblleProperties treblleProperties,
          ObjectMapper objectMapper,
          RestTemplateBuilder restTemplateBuilder) {
    this.treblleProperties = treblleProperties;
    this.objectMapper = objectMapper;
    this.restTemplateBuilder = restTemplateBuilder;
  }

  @Bean
  public TreblleService treblleService(DataMasker dataMasker) {
    return new TreblleServiceImpl(treblleProperties, objectMapper, restTemplateBuilder);
  }

  @Bean
  public DataMasker jsonMasker() {
    return new DataMaskerImpl(treblleProperties);
  }

  @Bean
  public TreblleConfiguration treblleConfiguration() {
    return new DefaultTreblleConfigurationImpl();
  }

  @Bean
  public FilterRegistrationBean<TreblleFilter> filterRegistration(SpringTreblleProperties properties, TreblleConfiguration treblleConfiguration, TreblleService treblleService) {
    final FilterRegistrationBean<TreblleFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new TreblleFilter(treblleService, treblleConfiguration));
    registrationBean.setOrder(treblleProperties.getFilterOrder());
    if (!properties.getUrlPatterns().isEmpty()) {
      registrationBean.setUrlPatterns(properties.getUrlPatterns());
    }
    return registrationBean;
  }
}
