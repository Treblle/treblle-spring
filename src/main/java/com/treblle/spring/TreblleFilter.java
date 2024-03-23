package com.treblle.spring;

import com.treblle.spring.dto.TrebllePayload;
import com.treblle.spring.service.TreblleService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

public class TreblleFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(TreblleFilter.class);

  private final TreblleService treblleService;

  public TreblleFilter(TreblleService treblleService) {
    this.treblleService = treblleService;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
    if (!isJSONRequest(request)) {
      log.debug("Attempted to intercept request but content type was not valid. Treblle only works on JSON API's.");
      return true;
    }
    return super.shouldNotFilter(request);
  }

  private boolean isJSONRequest(HttpServletRequest request) {
    String contentType = request.getContentType();
    return contentType != null && contentType.startsWith(MediaType.APPLICATION_JSON_VALUE);
  }

  @Override
  protected void doFilterInternal(HttpServletRequest servletRequest, HttpServletResponse servletResponse,
                                                           FilterChain filterChain) throws ServletException, IOException {

    final ContentCachingRequestWrapper cachingRequest = new ContentCachingRequestWrapper(servletRequest);
    final ContentCachingResponseWrapper cachingResponse = new ContentCachingResponseWrapper(servletResponse);

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
