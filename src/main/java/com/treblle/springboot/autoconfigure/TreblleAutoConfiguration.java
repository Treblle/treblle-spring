package com.treblle.springboot.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.treblle.springboot.config.TreblleProperties;
import com.treblle.springboot.core.MaskingService;
import com.treblle.springboot.core.PathMatcher;
import com.treblle.springboot.core.PayloadFactory;
import com.treblle.springboot.core.TreblleLogger;
import com.treblle.springboot.filter.TreblleExceptionResolver;
import com.treblle.springboot.filter.TreblleFilter;
import com.treblle.springboot.transport.CircuitBreaker;
import com.treblle.springboot.transport.TreblleClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Auto-configuration that wires the Treblle SDK into any Spring MVC (Servlet) application.
 *
 * <p>Users only add the dependency and set {@code treblle.*} properties. Every bean is
 * {@code @ConditionalOnMissingBean} so anything can be overridden.</p>
 *
 * <p>The SDK identifier and version sent in every payload are fixed here: {@code sdk = spring-boot},
 * {@code version = 30} (representing 3.0, matching the Maven artifact version).</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "treblle", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(TreblleProperties.class)
public class TreblleAutoConfiguration {

    /** Payload {@code sdk} identifier (kebab-case). */
    public static final String SDK_NAME = "spring-boot";

    /** Payload {@code version}: 30 == 3.0, tracking the Maven artifact version (×10). */
    public static final int SDK_VERSION = 30;

    @Bean
    @ConditionalOnMissingBean
    public TreblleLogger treblleLogger(TreblleProperties properties) {
        TreblleLogger logger = new TreblleLogger(properties.isDebug());
        logStartup(logger, properties);
        return logger;
    }

    @Bean
    @ConditionalOnMissingBean
    public MaskingService treblleMaskingService(TreblleProperties properties, TreblleLogger logger) {
        MaskingService service = new MaskingService(properties.getMaskedKeywords());
        if (!service.isActive()) {
            logger.warn("Masking is DISABLED: 'treblle.masked-keywords' is empty. "
                    + "No values will be masked before sending to Treblle.");
        }
        return service;
    }

    @Bean
    @ConditionalOnMissingBean
    public PathMatcher trebllePathMatcher(TreblleProperties properties) {
        return new PathMatcher(properties.getExcludedPaths());
    }

    @Bean
    @ConditionalOnMissingBean
    public CircuitBreaker treblleCircuitBreaker(TreblleLogger logger) {
        return new CircuitBreaker(logger);
    }

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper treblleObjectMapper() {
        return new ObjectMapper();
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    public TreblleClient treblleClient(TreblleProperties properties,
                                       TreblleLogger logger,
                                       CircuitBreaker circuitBreaker,
                                       ObjectMapper objectMapper,
                                       PayloadFactory payloadFactory) {
        return new TreblleClient(properties, objectMapper, logger, circuitBreaker, payloadFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public PayloadFactory trebllePayloadFactory(TreblleProperties properties,
                                                MaskingService maskingService,
                                                ObjectMapper objectMapper) {
        return new PayloadFactory(objectMapper, maskingService, properties, SDK_NAME, SDK_VERSION);
    }

    @Bean
    @ConditionalOnMissingBean
    public TreblleExceptionResolver treblleExceptionResolver() {
        return new TreblleExceptionResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public FilterRegistrationBean<TreblleFilter> treblleFilterRegistration(
            TreblleProperties properties,
            TreblleClient client,
            PathMatcher pathMatcher,
            TreblleLogger logger) {

        boolean active = properties.hasRequiredCredentials();
        if (!active) {
            logger.warn("Treblle is installed but DISABLED: 'treblle.sdk-token' and/or "
                    + "'treblle.api-key' are missing. The SDK will not send any data.");
        }

        TreblleFilter filter = new TreblleFilter(properties, client, pathMatcher, logger, active);
        FilterRegistrationBean<TreblleFilter> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.setName("treblleFilter");
        return registration;
    }

    private void logStartup(TreblleLogger logger, TreblleProperties properties) {
        if (!logger.isDebug()) {
            return;
        }
        logger.info("Initializing Treblle SDK (sdk=" + SDK_NAME + ", version=" + SDK_VERSION + ").");
        logger.info("Configuration: enabled=" + properties.isEnabled()
                + ", ingressEndpoint=" + properties.getIngressEndpoint()
                + ", maskedKeywords=" + properties.getMaskedKeywords().size()
                + ", excludedPaths=" + properties.getExcludedPaths().size()
                + ", sdkToken=" + (properties.getSdkToken() != null ? "set" : "MISSING")
                + ", apiKey=" + (properties.getApiKey() != null ? "set" : "MISSING") + ".");
    }
}
