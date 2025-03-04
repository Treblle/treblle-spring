package com.treblle.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.treblle.common.configuration.TreblleProperties;
import com.treblle.common.dto.TrebllePayload;
import com.treblle.common.service.TreblleService;
import com.treblle.lambda.infrastructure.APIGatewayProxyRequestEventWrapper;
import com.treblle.lambda.infrastructure.APIGatewayProxyResponseEventWrapper;
import com.treblle.lambda.service.TreblleServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class TreblleRequestHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger log = LoggerFactory.getLogger(TreblleRequestHandler.class);

    private final RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> actualHandler;
    private final TreblleService treblleService;

    public TreblleRequestHandler(RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> actualHandler, TreblleProperties treblleProperties) {
        this.actualHandler = actualHandler;
        this.treblleService = new TreblleServiceImpl(treblleProperties, new ObjectMapper());
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        final byte[] requestBody = Optional.ofNullable(apiGatewayProxyRequestEvent.getBody()).map(String::getBytes).orElse(null);

        Exception potentialException = null;
        final long start = System.currentTimeMillis();

        APIGatewayProxyResponseEvent apiGatewayProxyResponseEvent = null;
        try {
            apiGatewayProxyResponseEvent = actualHandler.handleRequest(apiGatewayProxyRequestEvent, context);
        } catch (Exception exception) {
            potentialException = exception;
            throw exception;
        } finally {
            final long responseTimeInMillis = System.currentTimeMillis() - start;
            final byte[] responseBody = Optional.ofNullable(apiGatewayProxyResponseEvent)
                    .map(APIGatewayProxyResponseEvent::getBody).map(String::getBytes).orElse(null);
            try {
                TrebllePayload payload = treblleService.createPayload(new APIGatewayProxyRequestEventWrapper(apiGatewayProxyRequestEvent), new APIGatewayProxyResponseEventWrapper(apiGatewayProxyResponseEvent), potentialException, responseTimeInMillis);
                treblleService.maskAndSendPayload(payload, requestBody, responseBody, potentialException);
            } catch (Exception exception) {
                log.error("An error occurred while sending data to Treblle.", exception);
            }
        }
        return apiGatewayProxyResponseEvent;
    }

}
