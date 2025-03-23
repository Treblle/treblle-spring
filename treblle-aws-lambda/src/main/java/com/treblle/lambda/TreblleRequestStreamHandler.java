package com.treblle.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.treblle.common.configuration.TreblleProperties;
import com.treblle.common.dto.TrebllePayload;
import com.treblle.common.service.TreblleService;
import com.treblle.lambda.infrastructure.StreamRequestContextWrapper;
import com.treblle.lambda.infrastructure.StreamResponseContextWrapper;
import com.treblle.lambda.service.TreblleServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class TreblleRequestStreamHandler implements RequestStreamHandler {

    private static final Logger log = LoggerFactory.getLogger(TreblleRequestStreamHandler.class);

    private final RequestStreamHandler actualHandler;
    private final TreblleService treblleService;
    private final ObjectMapper objectMapper;
    private String path;

    public TreblleRequestStreamHandler(RequestStreamHandler actualHandler, TreblleProperties treblleProperties) {
        this.actualHandler = actualHandler;
        this.objectMapper = new ObjectMapper();
        this.treblleService = new TreblleServiceImpl(treblleProperties, objectMapper);
    }

    public TreblleRequestStreamHandler(RequestStreamHandler actualHandler, TreblleProperties treblleProperties, String path) {
        this(actualHandler, treblleProperties);
        this.path = path;
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        JsonNode requestJson = objectMapper.readTree(copyInputStream(inputStream));
        ByteArrayOutputStream tempOutputStream = new ByteArrayOutputStream();

        Exception potentialException = null;
        final long start = System.currentTimeMillis();

        try {
            actualHandler.handleRequest(inputStream, tempOutputStream, context);
        } catch (Exception exception) {
            potentialException = exception;
            throw exception;
        } finally {
            final long responseTimeInMillis = System.currentTimeMillis() - start;
            final byte[] response = tempOutputStream.toByteArray();
            JsonNode responseJson = objectMapper.readTree(response);

            try {
                TrebllePayload payload = treblleService.createPayload(new StreamRequestContextWrapper(requestJson, path), new StreamResponseContextWrapper(responseJson), potentialException, responseTimeInMillis);

                JsonNode requestBody = requestJson.get("body");
                byte[] requestBodyBytes = requestBody != null ? objectMapper.writeValueAsBytes(requestBody) : null;

                JsonNode responseBody = responseJson.get("body");
                byte[] responseBodyBytes = responseBody != null ? objectMapper.writeValueAsBytes(responseBody) : null;

                treblleService.maskAndSendPayload(payload, requestBodyBytes, responseBodyBytes, potentialException);
            } catch (Exception exception) {
                log.error("An error occurred while sending data to Treblle.", exception);
            }
            outputStream.write(response);
        }
    }

    private InputStream copyInputStream(InputStream original) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = original.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, length);
        }
        return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    }

}
