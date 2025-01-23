package com.treblle.javax.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.treblle.common.configuration.TreblleProperties;
import com.treblle.common.dto.TrebllePayload;
import com.treblle.common.service.AbstractTreblleService;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
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

            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
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

}
