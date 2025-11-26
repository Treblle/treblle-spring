package com.treblle.spring.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.treblle.spring.configuration.TreblleProperties;
import com.treblle.spring.dto.Data;
import com.treblle.spring.dto.Language;
import com.treblle.spring.dto.OperatingSystem;
import com.treblle.spring.dto.Request;
import com.treblle.spring.dto.Response;
import com.treblle.spring.dto.RuntimeError;
import com.treblle.spring.dto.Server;
import com.treblle.spring.dto.TrebllePayload;
import com.treblle.spring.utils.HttpUtils;
import com.treblle.spring.utils.JsonMasker;
import java.io.IOException;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

public class TreblleServiceImpl implements TreblleService {

  private static final Logger log = LoggerFactory.getLogger(TreblleServiceImpl.class);

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
          DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private static final Pattern IPV4_PATTERN =
          Pattern.compile("^((25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])\\.){3}(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])$");

  private static final String TREBLLE_API_ENDPOINT = "https://rocknrolla.treblle.com";
  private static final String TREBLLE_API_KEY_HEADER = "x-api-key";

  @Autowired private Environment environment;

  @Autowired private TreblleProperties properties;

  @Autowired private JsonMasker jsonMasker;

  @Autowired private RestTemplateBuilder restTemplateBuilder;

  private ObjectMapper objectMapper;
  private RestTemplate client;

  @PostConstruct
  private void init() {
    if (!StringUtils.hasLength(properties.getApiKey())) {
      throw new IllegalStateException("Treblle API key is required.");
    }

    if (!StringUtils.hasLength(properties.getProjectId())) {
      throw new IllegalStateException("Treblle Project ID is required.");
    }

    this.objectMapper = new ObjectMapper();
    this.client =
        restTemplateBuilder
            .setConnectTimeout(Duration.ofSeconds(1))
            .setReadTimeout(Duration.ofSeconds(1))
            .build();
  }

  @Override
  public TrebllePayload createPayload(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse,
      Exception chainException,
      long responseTimeInMillis) {
    final Language language = new Language();
    language.setName("java");
    language.setVersion(System.getProperty("java.version"));

    final OperatingSystem os = new OperatingSystem();
    os.setName(System.getProperty("os.name"));
    os.setArchitecture(System.getProperty("os.arch"));
    os.setRelease(System.getProperty("os.version"));

    final Server server = new Server();
    server.setIp(filterIPv4Only(environment.getProperty("server.address")));
    server.setTimezone(TimeZone.getDefault().getID());
    server.setProtocol(httpRequest.getProtocol());
    server.setOs(os);

    final Request request = new Request();
    request.setTimestamp(ZonedDateTime.now(ZoneOffset.UTC).format(DATE_TIME_FORMATTER));
    request.setIp(filterIPv4Only(HttpUtils.getClientAddress(httpRequest)));
    request.setUser_agent(httpRequest.getHeader(HttpHeaders.USER_AGENT));
    request.setMethod(httpRequest.getMethod());
    request.setUrl(ServletUriComponentsBuilder.fromRequestUri(httpRequest).toUriString());
    request.setQuery(request.getQuery());
    final Map<String, String> requestHeaders =
        readHeaders(Collections.list(httpRequest.getHeaderNames()), httpRequest::getHeader);
    if (!requestHeaders.isEmpty()) {
      request.setHeaders(requestHeaders);
    }

    final Response response = new Response();
    response.setCode(chainException != null ? 500 : httpResponse.getStatus());
    response.setLoad_time(responseTimeInMillis);
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
    payload.setApi_key(properties.getApiKey());
    payload.setProject_id(properties.getProjectId());
    payload.setData(data);

    return payload;
  }

  private String filterIPv4Only(String ip) {
    if (ip == null || ip.isEmpty()) {
      return null;
    }
    boolean valid = IPV4_PATTERN.matcher(ip).matches();
    return valid ? ip : null;
  }

  @Async
  @Override
  public void maskAndSendPayload(
      TrebllePayload payload, byte[] requestBody, byte[] responseBody, Exception chainException) {

    try {
      final List<RuntimeError> errors = new ArrayList<>(2);

      Request request = payload.getData().getRequest();
      request.setBody(
          Optional.ofNullable(readBody(requestBody, errors::add))
              .map(jsonMasker::mask)
              .orElse(null));

      Response response = payload.getData().getResponse();
      response.setSize((long) responseBody.length);
      response.setBody(
              Optional.ofNullable(readBody(responseBody, errors::add))
                      .map(jsonMasker::mask)
                      .orElse(null));

      if (chainException != null) {
        StackTraceElement[] stackTrace = chainException.fillInStackTrace().getStackTrace();
        final RuntimeError error = new RuntimeError();
        error.setSource("onError");
        error.setType(chainException.getClass().getName());
        error.setMessage(chainException.getMessage());
        error.setFile(
            Arrays.stream(stackTrace).findFirst().map(StackTraceElement::getFileName).orElse(null));
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
      headers.set(TREBLLE_API_KEY_HEADER, properties.getApiKey());

      final HttpEntity<TrebllePayload> requestEntity = new HttpEntity<>(payload, headers);

      try {
        client.postForEntity(TREBLLE_API_ENDPOINT, requestEntity, Void.class);
      } catch (RestClientException exception) {
        log.error("An error occurred while sending network request to Treblle.", exception);
      }
    } catch (Exception exception) {
      log.error("An error occurred while sending data to Treblle.", exception);
    }
  }

  private Map<String, String> readHeaders(Collection<String> headers, UnaryOperator<String> extractor) {
    return headers.stream()
            .collect(Collectors.toMap(
                    name -> name,
                    name -> {
                      String value = extractor.apply(name);
                      return value != null ? value : "";
                    },
                    (first, second) -> first));
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
}
