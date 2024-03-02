package com.treblle.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.treblle.spring.configuration.TreblleProperties;
import com.treblle.spring.utils.DataMasker;
import com.treblle.spring.utils.DataMaskerImpl;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TreblleProperties.class)
public class TestConfig {

    private final TreblleProperties treblleProperties;

    public TestConfig(TreblleProperties treblleProperties) {
        this.treblleProperties = treblleProperties;
    }

    @Bean
    public DataMasker dataMasker() {
        return new DataMaskerImpl(treblleProperties);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

}
