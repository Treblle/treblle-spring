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

  private final RestTemplateBuilder restTemplateBuilder;

  public TreblleAutoConfiguration(
          SpringTreblleProperties treblleProperties,
          RestTemplateBuilder restTemplateBuilder) {
    this.treblleProperties = treblleProperties;
    this.restTemplateBuilder = restTemplateBuilder;
  }

  @Bean
  public TreblleService treblleService() {
    return new TreblleServiceImpl(treblleProperties, new ObjectMapper(), restTemplateBuilder);
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
