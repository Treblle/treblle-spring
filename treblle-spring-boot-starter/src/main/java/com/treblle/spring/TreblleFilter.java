package com.treblle.spring;

import com.treblle.common.dto.TrebllePayload;
import com.treblle.common.service.TreblleService;
import com.treblle.spring.configuration.TreblleConfiguration;
import com.treblle.spring.infrastructure.SpringRequestWrapper;
import com.treblle.spring.infrastructure.SpringResponseWrapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class TreblleFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(TreblleFilter.class);

  private final TreblleService treblleService;

  private final TreblleConfiguration treblleConfiguration;

  public TreblleFilter(TreblleService treblleService, TreblleConfiguration treblleConfiguration) {
    this.treblleService = treblleService;
    this.treblleConfiguration = treblleConfiguration;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
    if (!isJSONRequest(request)) {
      log.debug("Attempted to intercept request but content type was not valid. Treblle only works on JSON API's.");
      return true;
    }
    return !treblleConfiguration.shouldProcess(request);
  }

  private boolean isJSONRequest(HttpServletRequest request) {
    String contentType = request.getContentType();
    return contentType != null && contentType.startsWith(MediaType.APPLICATION_JSON_VALUE);
  }

  @Override
  protected void doFilterInternal(HttpServletRequest servletRequest, HttpServletResponse servletResponse,
                                                           FilterChain filterChain) throws ServletException, IOException {

    final SpringRequestWrapper cachingRequest = new SpringRequestWrapper(servletRequest, getEnvironment());
    final SpringResponseWrapper cachingResponse = new SpringResponseWrapper(servletResponse);

    Exception potentialException = null;
    final long start = System.currentTimeMillis();

    try {
      filterChain.doFilter(cachingRequest, cachingResponse);
    } catch (Exception exception) {
      potentialException = exception;
      throw exception;
    } finally {
      final long responseTimeInMillis = System.currentTimeMillis() - start;
      final byte[] requestBody = cachingRequest.getContentAsByteArray();
      final byte[] responseBody = cachingResponse.getContentAsByteArray();
      cachingResponse.copyBodyToResponse(); // Important

      try {
        TrebllePayload payload = treblleService.createPayload(cachingRequest, cachingResponse, potentialException, responseTimeInMillis);
        treblleService.maskAndSendPayload(payload, requestBody, responseBody, potentialException);
      } catch (Exception exception) {
        log.error("An error occurred while sending data to Treblle.", exception);
      }
    }
  }
}
