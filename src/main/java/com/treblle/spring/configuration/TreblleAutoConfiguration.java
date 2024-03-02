package com.treblle.spring.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.treblle.spring.TreblleFilter;
import com.treblle.spring.service.TreblleService;
import com.treblle.spring.service.TreblleServiceImpl;
import com.treblle.spring.utils.DataMasker;
import com.treblle.spring.utils.DataMaskerImpl;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
@EnableConfigurationProperties(TreblleProperties.class)
public class TreblleAutoConfiguration {

  private final Environment environment;

  private final TreblleProperties treblleProperties;

  private final ObjectMapper objectMapper;

  private final RestTemplateBuilder restTemplateBuilder;

  public TreblleAutoConfiguration(
          Environment environment,
          TreblleProperties treblleProperties,
          ObjectMapper objectMapper,
          RestTemplateBuilder restTemplateBuilder) {
    this.environment = environment;
    this.treblleProperties = treblleProperties;
    this.objectMapper = objectMapper;
    this.restTemplateBuilder = restTemplateBuilder;
  }

  @Bean
  public TreblleService treblleService(DataMasker dataMasker) {
    return new TreblleServiceImpl(environment, treblleProperties, objectMapper, dataMasker, restTemplateBuilder);
  }

  @Bean
  public DataMasker jsonMasker() {
    return new DataMaskerImpl(treblleProperties);
  }

  @Bean
  public FilterRegistrationBean<TreblleFilter> filterRegistration(TreblleProperties properties, TreblleService treblleService) {
    final FilterRegistrationBean<TreblleFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new TreblleFilter(treblleService));
    registrationBean.setOrder(treblleProperties.getFilterOrder());
    if (!properties.getUrlPatterns().isEmpty()) {
      registrationBean.setUrlPatterns(properties.getUrlPatterns());
    }
    return registrationBean;
  }
}
