package com.treblle.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.treblle.common.utils.DataMasker;
import com.treblle.common.utils.DataMaskerImpl;
import com.treblle.spring.configuration.SpringTreblleProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SpringTreblleProperties.class)
public class TestConfig {

    private final SpringTreblleProperties treblleProperties;

    public TestConfig(SpringTreblleProperties treblleProperties) {
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
