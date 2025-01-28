package com.treblle.jakarta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.treblle.common.dto.TrebllePayload;
import com.treblle.common.service.TreblleService;
import com.treblle.jakarta.configuration.ServletFilterTreblleProperties;
import com.treblle.jakarta.infrastructure.ContentCachingRequestWrapper;
import com.treblle.jakarta.infrastructure.ContentCachingResponseWrapper;
import com.treblle.jakarta.service.TreblleServiceImpl;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.treblle.common.utils.HttpUtils.APPLICATION_JSON_VALUE;

public class TreblleServletFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(TreblleServletFilter.class);

    private static final String SDK_NAME = "jakarta-servlet";

    private TreblleService treblleService;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.treblleService = new TreblleServiceImpl(SDK_NAME, new ServletFilterTreblleProperties(filterConfig), new ObjectMapper());
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        final ContentCachingRequestWrapper cachingRequest = new ContentCachingRequestWrapper((HttpServletRequest) servletRequest);
        final ContentCachingResponseWrapper cachingResponse = new ContentCachingResponseWrapper((HttpServletResponse) servletResponse);

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

    @Override
    public void destroy() {

    }

}
