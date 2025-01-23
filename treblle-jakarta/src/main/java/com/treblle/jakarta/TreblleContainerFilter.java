package com.treblle.jakarta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.treblle.common.configuration.TreblleProperties;
import com.treblle.common.dto.TrebllePayload;
import com.treblle.common.service.TreblleService;
import com.treblle.jakarta.configuration.ContainerFilterTreblleProperties;
import com.treblle.jakarta.infrastructure.ContainerRequestContextWrapper;
import com.treblle.jakarta.infrastructure.ContainerResponseContextWrapper;
import com.treblle.jakarta.service.TreblleServiceImpl;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class TreblleContainerFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger log = LoggerFactory.getLogger(TreblleContainerFilter.class);

    private static final String SDK_NAME = "jakarta-container";

    private static final String REQUEST_BODY_PROPERTY = "requestBody";
    private static final String START_TIME_PROPERTY = "startTime";

    @Context
    private Configuration configuration;

    private final TreblleService treblleService;

    public TreblleContainerFilter() {
        this.treblleService = new TreblleServiceImpl(
                SDK_NAME,
                new ContainerFilterTreblleProperties(configuration),
                new ObjectMapper()
        );
    }

    public TreblleContainerFilter(TreblleProperties treblleProperties) {
        this.treblleService = new TreblleServiceImpl(
                SDK_NAME,
                treblleProperties,
                new ObjectMapper()
        );
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        InputStream inputStream = containerRequestContext.getEntityStream();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }

        String requestBody = byteArrayOutputStream.toString(StandardCharsets.UTF_8);
        containerRequestContext.setProperty(REQUEST_BODY_PROPERTY, requestBody);
        containerRequestContext.setProperty(START_TIME_PROPERTY, System.currentTimeMillis());

        containerRequestContext.setEntityStream(new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        OutputStream originalOutputStream = containerResponseContext.getEntityStream();
        containerResponseContext.setEntityStream(new CaptureOutputStream(byteArrayOutputStream, originalOutputStream));

        byte[] responseBody = byteArrayOutputStream.toByteArray();
        String requestBodyString = (String) containerRequestContext.getProperty(REQUEST_BODY_PROPERTY);
        byte[] requestBody = requestBodyString.getBytes(StandardCharsets.UTF_8);

        long startTime = (long) containerRequestContext.getProperty(START_TIME_PROPERTY);
        long responseTimeInMillis = System.currentTimeMillis() - startTime;

        try {
            TrebllePayload payload = treblleService.createPayload(
                    new ContainerRequestContextWrapper(containerRequestContext),
                    new ContainerResponseContextWrapper(containerResponseContext),
                    null,
                    responseTimeInMillis
            );
            treblleService.maskAndSendPayload(payload, requestBody, responseBody, null);
        } catch (Exception exception) {
            log.error("An error occurred while sending data to Treblle.", exception);
        }
    }

    private static class CaptureOutputStream extends OutputStream {

        private final ByteArrayOutputStream byteArrayOutputStream;
        private final OutputStream originalOutputStream;

        public CaptureOutputStream(ByteArrayOutputStream byteArrayOutputStream, OutputStream originalOutputStream) {
            this.byteArrayOutputStream = byteArrayOutputStream;
            this.originalOutputStream = originalOutputStream;
        }

        @Override
        public void write(int b) throws IOException {
            byteArrayOutputStream.write(b);
            originalOutputStream.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            byteArrayOutputStream.write(b, off, len);
            originalOutputStream.write(b, off, len);
        }

    }

}
