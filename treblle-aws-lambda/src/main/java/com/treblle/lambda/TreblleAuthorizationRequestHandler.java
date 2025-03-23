package com.treblle.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayCustomAuthorizerEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.treblle.common.configuration.TreblleProperties;
import com.treblle.common.dto.TrebllePayload;
import com.treblle.common.service.TreblleService;
import com.treblle.lambda.infrastructure.APIGatewayCustomAuthorizerEventRequestWrapper;
import com.treblle.lambda.infrastructure.APIGatewayCustomAuthorizerEventResponseWrapper;
import com.treblle.lambda.service.TreblleServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class TreblleAuthorizationRequestHandler implements RequestHandler<APIGatewayCustomAuthorizerEvent, Map<String, Object>> {

    private static final Logger log = LoggerFactory.getLogger(TreblleAuthorizationRequestHandler.class);

    private final RequestHandler<APIGatewayCustomAuthorizerEvent, Map<String, Object>> actualHandler;
    private final TreblleService treblleService;
    private final ObjectMapper objectMapper;
    private String path;

    public TreblleAuthorizationRequestHandler(RequestHandler<APIGatewayCustomAuthorizerEvent, Map<String, Object>> actualHandler, TreblleProperties treblleProperties) {
        this.actualHandler = actualHandler;
        this.objectMapper = new ObjectMapper();
        this.treblleService = new TreblleServiceImpl(treblleProperties, objectMapper);
    }

    public TreblleAuthorizationRequestHandler(RequestHandler<APIGatewayCustomAuthorizerEvent, Map<String, Object>> actualHandler, TreblleProperties treblleProperties, String path) {
        this(actualHandler, treblleProperties);
        this.path = path;
    }

    @Override
    public Map<String, Object> handleRequest(APIGatewayCustomAuthorizerEvent apiGatewayCustomAuthorizerEvent, Context context) {
        Exception potentialException = null;
        final long start = System.currentTimeMillis();

        Map<String, Object> apiGatewayCustomAuthorizerResponseEvent = null;
        try {
            apiGatewayCustomAuthorizerResponseEvent = actualHandler.handleRequest(apiGatewayCustomAuthorizerEvent, context);
        } catch (Exception exception) {
            potentialException = exception;
            throw exception;
        } finally {
            final long responseTimeInMillis = System.currentTimeMillis() - start;
            try {
                final byte[] responseBody = apiGatewayCustomAuthorizerResponseEvent != null ? objectMapper.writeValueAsBytes(apiGatewayCustomAuthorizerResponseEvent) : null;

                TrebllePayload payload = treblleService.createPayload(new APIGatewayCustomAuthorizerEventRequestWrapper(apiGatewayCustomAuthorizerEvent, path), new APIGatewayCustomAuthorizerEventResponseWrapper(apiGatewayCustomAuthorizerResponseEvent), potentialException, responseTimeInMillis);
                treblleService.maskAndSendPayload(payload, null, responseBody, potentialException);
            } catch (Exception exception) {
                log.error("An error occurred while sending data to Treblle.", exception);
            }
        }
        return apiGatewayCustomAuthorizerResponseEvent;
    }

}
