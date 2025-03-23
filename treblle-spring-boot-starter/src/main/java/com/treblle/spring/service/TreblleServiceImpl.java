package com.treblle.spring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.treblle.common.dto.TrebllePayload;
import com.treblle.common.service.AbstractTreblleService;
import com.treblle.spring.configuration.SpringTreblleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

public class TreblleServiceImpl extends AbstractTreblleService {

  private static final Logger LOGGER = LoggerFactory.getLogger(TreblleServiceImpl.class);

  private static final String SDK_NAME = "spring-boot";

  private final SpringTreblleProperties treblleProperties;

  private final RestTemplate restTemplate;

  public TreblleServiceImpl(SpringTreblleProperties treblleProperties, ObjectMapper objectMapper, RestTemplateBuilder restTemplateBuilder) {
    super(SDK_NAME, treblleProperties, objectMapper);
    this.treblleProperties = treblleProperties;

    if (treblleProperties.isDebug()) {
      restTemplateBuilder = restTemplateBuilder
              .requestFactory(() -> new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()))
              .additionalInterceptors(new TreblleLoggingInterceptor());
    }

    this.restTemplate =
            restTemplateBuilder
                    .setConnectTimeout(Duration.ofSeconds(1))
                    .setReadTimeout(Duration.ofSeconds(1))
                    .build();
  }

  @Async
  @Override
  public void maskAndSendPayload(TrebllePayload payload, byte[] requestBody, byte[] responseBody, Exception chainException) {
    super.maskAndSendPayload(payload, requestBody, responseBody, chainException);
  }

  @Override
  protected void sendPayload(TrebllePayload payload) {
    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set(TREBLLE_API_KEY_HEADER, treblleProperties.getApiKey());
    final HttpEntity<TrebllePayload> requestEntity = new HttpEntity<>(payload, headers);

    try {
      restTemplate.postForEntity(Optional.ofNullable(treblleProperties.getEndpoint()).orElse(getRandomAPIEndpoint()), requestEntity, Void.class);
    } catch (RestClientException exception) {
      if (treblleProperties.isDebug()) {
        LOGGER.error("An error occurred while sending network request to Treblle.", exception);
      }
    }
  }

  public static class TreblleLoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TreblleLoggingInterceptor.class);

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
      LOGGER.info("Request Method: " + request.getMethod());
      LOGGER.info("Request URI: " + request.getURI());
      LOGGER.info("Request Headers: " + request.getHeaders());
      if (body.length > 0) {
        LOGGER.info("Request Body: " + new String(body, StandardCharsets.UTF_8));
      }

      ClientHttpResponse response = execution.execute(request, body);

      LOGGER.info("Response Status Code: " + response.getStatusCode());
      LOGGER.info("Response Headers: " + response.getHeaders());
      if (response.getBody() != null) {
        String responseBody = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
        LOGGER.info("Response Body: " + responseBody);
      }

      return response;
    }

  }

}
