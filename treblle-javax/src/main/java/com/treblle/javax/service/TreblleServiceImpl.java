package com.treblle.javax.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.treblle.common.configuration.TreblleProperties;
import com.treblle.common.dto.TrebllePayload;
import com.treblle.common.service.AbstractTreblleService;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.treblle.common.utils.HttpUtils.APPLICATION_JSON_VALUE;

public class TreblleServiceImpl extends AbstractTreblleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TreblleServiceImpl.class);

    public TreblleServiceImpl(String sdkName, TreblleProperties treblleProperties, ObjectMapper objectMapper) {
        super(sdkName, treblleProperties, objectMapper);
    }

    @Override
    protected void sendPayload(TrebllePayload payload) {
        CompletableFuture.runAsync(() -> {
            final HttpPost httpPost = new HttpPost(Optional.ofNullable(treblleProperties.getEndpoint()).orElse(getRandomAPIEndpoint()));
            httpPost.setHeader("Content-Type", APPLICATION_JSON_VALUE);
            httpPost.setHeader(TREBLLE_API_KEY_HEADER, treblleProperties.getApiKey());

            HttpClientBuilder httpClientBuilder = HttpClients.custom().disableAutomaticRetries();
            if (treblleProperties.isDebug()) {
                httpClientBuilder
                        .addRequestInterceptorFirst(new RequestLogger())
                        .addResponseInterceptorFirst(new ResponseLogger());
            }

            try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
                StringEntity entity = new StringEntity(objectMapper.writeValueAsString(payload));
                httpPost.setEntity(entity);

                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    if (response.getCode() != 200 && treblleProperties.isDebug()) {
                        LOGGER.error("An error occurred while sending network request to Treblle. Status Code: {}", response.getCode());
                    }
                }
            } catch (IOException exception) {
                if (treblleProperties.isDebug()) {
                    LOGGER.error("An error occurred while sending network request to Treblle.", exception);
                }
            }
        });
    }

    public static class RequestLogger implements HttpRequestInterceptor {

        @Override
        public void process(HttpRequest httpRequest, EntityDetails entityDetails, HttpContext httpContext) throws HttpException, IOException {
            LOGGER.info("Request Method: {}", httpRequest.getMethod());
            LOGGER.info("Request Authority: {}", httpRequest.getAuthority().toString());
        }

    }

    public static class ResponseLogger implements HttpResponseInterceptor {

        @Override
        public void process(HttpResponse httpResponse, EntityDetails entityDetails, HttpContext httpContext) throws HttpException, IOException {
            LOGGER.info("Response Status Code: {}", httpResponse.getCode());
            LOGGER.info("Response Status Reason: {}", httpResponse.getReasonPhrase());
        }

    }

}
