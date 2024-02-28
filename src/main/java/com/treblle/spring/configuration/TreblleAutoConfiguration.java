package com.treblle.spring.configuration;

import com.treblle.spring.TreblleFilter;
import com.treblle.spring.service.TreblleService;
import com.treblle.spring.service.TreblleServiceImpl;
import com.treblle.spring.utils.JsonMasker;
import com.treblle.spring.utils.JsonMaskerImpl;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
@EnableConfigurationProperties(TreblleProperties.class)
public class TreblleAutoConfiguration {

  private final TreblleProperties treblleProperties;

  public TreblleAutoConfiguration(TreblleProperties treblleProperties) {
    this.treblleProperties = treblleProperties;
  }

  @Bean
  public TreblleService treblleService() {
    return new TreblleServiceImpl();
  }

  @Bean
  public JsonMasker jsonMasker() {
    return new JsonMaskerImpl();
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
