package com.treblle.spring.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.treblle.spring.configuration.TreblleProperties;
import com.treblle.spring.dto.*;
import com.treblle.spring.utils.DataMasker;
import com.treblle.spring.utils.HttpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class TreblleServiceImpl implements TreblleService {

  private static final Logger LOGGER = LoggerFactory.getLogger(TreblleServiceImpl.class);

  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private static final String[] TREBLLE_API_ENDPOINT = {
          "https://rocknrolla.treblle.com",
          "https://punisher.treblle.com",
          "https://sicario.treblle.com"
  };

  private static final Random RANDOM = new SecureRandom();

  private static final String TREBLLE_API_KEY_HEADER = "x-api-key";

  private final Environment environment;

  private final TreblleProperties treblleProperties;

  private final ObjectMapper objectMapper;

  private final DataMasker dataMasker;

  private final RestTemplate restTemplate;

  public TreblleServiceImpl(Environment environment, TreblleProperties treblleProperties, ObjectMapper objectMapper, DataMasker dataMasker, RestTemplateBuilder restTemplateBuilder) {
    this.environment = environment;
    this.treblleProperties = treblleProperties;
    this.objectMapper = objectMapper;
    this.dataMasker = dataMasker;

    if (!StringUtils.hasLength(treblleProperties.getApiKey())) {
      throw new IllegalStateException("Treblle API key is required.");
    }

    if (!StringUtils.hasLength(treblleProperties.getProjectId())) {
      throw new IllegalStateException("Treblle Project ID is required.");
    }

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

  @Override
  public TrebllePayload createPayload(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
                                                                    Exception chainException, long responseTimeInMillis) {

    final Language language = new Language();
    language.setName("java");
    language.setVersion(System.getProperty("java.version"));

    final OperatingSystem os = new OperatingSystem();
    os.setName(System.getProperty("os.name"));
    os.setArchitecture(System.getProperty("os.arch"));
    os.setRelease(System.getProperty("os.version"));

    final Server server = new Server();
    server.setIp(environment.getProperty("server.address"));
    server.setTimezone(TimeZone.getDefault().getID());
    server.setProtocol(httpRequest.getProtocol());
    server.setOs(os);

    final Request request = new Request();
    request.setTimestamp(ZonedDateTime.now(ZoneOffset.UTC).format(DATE_TIME_FORMATTER));
    request.setIp(HttpUtils.getClientAddress(httpRequest));
    request.setUserAgent(httpRequest.getHeader(HttpHeaders.USER_AGENT));
    request.setMethod(httpRequest.getMethod());
    request.setUrl(ServletUriComponentsBuilder.fromRequestUri(httpRequest).toUriString());
    final Map<String, String> requestHeaders = readHeaders(Collections.list(httpRequest.getHeaderNames()), httpRequest::getHeader);
    if (!requestHeaders.isEmpty()) {
      request.setHeaders(requestHeaders);
    }

    final Response response = new Response();
    response.setCode(chainException != null ? 500 : httpResponse.getStatus());
    response.setLoadTime((double) (responseTimeInMillis / 1000f));
    final Map<String, String> responseHeaders =
            readHeaders(
                    Optional.ofNullable(httpResponse.getHeaderNames()).orElseGet(Collections::emptyList),
                    httpResponse::getHeader);
    if (!responseHeaders.isEmpty()) {
      response.setHeaders(responseHeaders);
    }

    final Data data = new Data();
    data.setServer(server);
    data.setLanguage(language);
    data.setRequest(request);
    data.setResponse(response);

    final TrebllePayload payload = new TrebllePayload();
    payload.setApiKey(treblleProperties.getApiKey());
    payload.setProjectId(treblleProperties.getProjectId());
    payload.setData(data);

    return payload;
  }

  @Async
  @Override
  public void maskAndSendPayload(TrebllePayload payload, byte[] requestBody, byte[] responseBody, Exception chainException) {

    try {
      final List<RuntimeError> errors = new ArrayList<>(2);
      Request request = payload.getData().getRequest();
      request.setBody(
              Optional.ofNullable(readBody(requestBody, errors::add))
                      .map(dataMasker::mask)
                      .orElse(null)
      );

      Response response = payload.getData().getResponse();
      response.setSize((long) responseBody.length);
      response.setBody(
              Optional.ofNullable(readBody(responseBody, errors::add))
                      .map(dataMasker::mask)
                      .orElse(null)
      );

      if (chainException != null) {
        StackTraceElement[] stackTrace = chainException.fillInStackTrace().getStackTrace();
        final RuntimeError error = new RuntimeError();
        error.setSource("onError");
        error.setType(chainException.getClass().getName());
        error.setMessage(chainException.getMessage());
        error.setFile(Arrays.stream(stackTrace).findFirst().map(StackTraceElement::getFileName).orElse(null));
        error.setLine(
                Arrays.stream(stackTrace)
                        .findFirst()
                        .map(StackTraceElement::getLineNumber)
                        .orElse(null));

        errors.add(error);
      }

      if (!errors.isEmpty()) {
        payload.getData().setErrors(errors);
      }

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
    } catch (Exception exception) {
      LOGGER.error("An error occurred while preparing data for Treblle.", exception);
    }
  }

  private String getRandomAPIEndpoint() {
    int randomIndex = RANDOM.nextInt(TREBLLE_API_ENDPOINT.length);
    return TREBLLE_API_ENDPOINT[randomIndex];
  }

  private Map<String, String> readHeaders(Collection<String> headers, UnaryOperator<String> extractor) {
    return dataMasker.mask(headers.stream().collect(Collectors.toMap(name -> name, extractor, (first, second) -> first)));
  }

  private JsonNode readBody(byte[] body, Consumer<RuntimeError> errorConsumer) {
    if (body.length > 0) {
      try {
        return objectMapper.readTree(body, 0, body.length);
      } catch (IOException exception) {
        RuntimeError error = new RuntimeError();
        error.setSource("onShutdown");
        error.setType("INVALID_JSON");
        error.setMessage("Invalid JSON format");
        errorConsumer.accept(error);
        return null;
      }
    } else {
      return null;
    }
  }

  public static class TreblleLoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TreblleLoggingInterceptor.class);

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
      LOGGER.debug("Request Method: " + request.getMethod());
      LOGGER.debug("Request URI: " + request.getURI());
      LOGGER.debug("Request Headers: " + request.getHeaders());
      if (body.length > 0) {
        LOGGER.debug("Request Body: " + new String(body, StandardCharsets.UTF_8));
      }

      ClientHttpResponse response = execution.execute(request, body);

      LOGGER.debug("Response Status Code: " + response.getStatusCode());
      LOGGER.debug("Response Headers: " + response.getHeaders());
      if (response.getBody() != null) {
        String responseBody = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
        LOGGER.debug("Response Body: " + responseBody);
      }

      return response;
    }

  }

}
