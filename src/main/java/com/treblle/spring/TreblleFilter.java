package com.treblle.spring;

import com.treblle.spring.dto.TrebllePayload;
import com.treblle.spring.service.TreblleService;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

public class TreblleFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(TreblleFilter.class);

  private final TreblleService service;

  public TreblleFilter(TreblleService service) {
    this.service = service;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
    String contentType = request.getContentType();
    if (contentType != null && !MediaType.APPLICATION_JSON_VALUE.equals(contentType)) {
      log.debug(
          "Attempted to intercept request but content type was not valid. Treblle only works on JSON API's.");
      return true;
    }
    return super.shouldNotFilter(request);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest httpServletRequest,
      HttpServletResponse httpServletResponse,
      FilterChain filterChain)
      throws ServletException, IOException {
    final ContentCachingRequestWrapper cachingRequest =
        new ContentCachingRequestWrapper(httpServletRequest);
    final ContentCachingResponseWrapper cachingResponse =
        new ContentCachingResponseWrapper(httpServletResponse);

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
        TrebllePayload payload =
            service.createPayload(
                cachingRequest, cachingResponse, potentialException, responseTimeInMillis);
        service.maskAndSendPayload(payload, requestBody, responseBody, potentialException);
      } catch (Exception exception) {
        log.error("An error occurred while sending data to Treblle.", exception);
      }
    }
  }
}
