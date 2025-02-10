package com.treblle.common.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.treblle.common.configuration.TreblleProperties;
import com.treblle.common.dto.*;
import com.treblle.common.infrastructure.RequestWrapper;
import com.treblle.common.infrastructure.ResponseWrapper;
import com.treblle.common.utils.DataMasker;
import com.treblle.common.utils.DataMaskerImpl;
import com.treblle.common.utils.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public abstract class AbstractTreblleService implements TreblleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTreblleService.class);

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String[] TREBLLE_API_ENDPOINT = {
            "https://rocknrolla.treblle.com",
            "https://punisher.treblle.com",
            "https://sicario.treblle.com"
    };

    private static final Random RANDOM = new SecureRandom();

    private static final String USER_AGENT_HEADER = "User-Agent";

    protected static final String TREBLLE_API_KEY_HEADER = "X-API-Key";

    private final String sdkName;

    protected final TreblleProperties treblleProperties;

    protected final DataMasker dataMasker;

    protected final ObjectMapper objectMapper;

    public AbstractTreblleService(String sdkName, TreblleProperties treblleProperties, ObjectMapper objectMapper) {
        this.sdkName = sdkName;
        this.treblleProperties = treblleProperties;
        this.dataMasker = new DataMaskerImpl(treblleProperties);
        this.objectMapper = objectMapper;

        if (treblleProperties.getApiKey() == null || treblleProperties.getApiKey().isEmpty()) {
            throw new IllegalStateException("Treblle API key is required.");
        }

        if (treblleProperties.getProjectId() == null || treblleProperties.getProjectId().isEmpty()) {
            throw new IllegalStateException("Treblle Project ID is required.");
        }
    }

    @Override
    public TrebllePayload createPayload(RequestWrapper httpRequest, ResponseWrapper httpResponse, Exception chainException, long responseTimeInMillis) {
        final Language language = new Language();
        language.setName("java");
        language.setVersion(System.getProperty("java.version"));

        final OperatingSystem os = new OperatingSystem();
        os.setName(System.getProperty("os.name"));
        os.setArchitecture(System.getProperty("os.arch"));
        os.setRelease(System.getProperty("os.version"));

        final Server server = new Server();
        server.setIp(httpRequest.getServerAddr());
        server.setTimezone(TimeZone.getDefault().getID());
        server.setProtocol(httpRequest.getProtocol());
        server.setOs(os);

        final Request request = new Request();
        request.setTimestamp(ZonedDateTime.now(ZoneOffset.UTC).format(DATE_TIME_FORMATTER));
        request.setIp(HttpUtils.getClientAddress(httpRequest));
        request.setUserAgent(httpRequest.getHeader(USER_AGENT_HEADER));
        request.setMethod(httpRequest.getMethod());
        request.setUrl(httpRequest.getUrl());
        final Map<String, String> requestHeaders = readHeaders(Collections.list(httpRequest.getHeaderNames()), httpRequest::getHeader);
        if (!requestHeaders.isEmpty()) {
            request.setHeaders(requestHeaders);
        }

        final Response response = new Response();
        response.setCode(chainException != null ? 500 : httpResponse.getStatus());
        response.setLoadTime((double) (responseTimeInMillis / 1000f));
        final Map<String, String> responseHeaders =
                readHeaders(
                        Optional.ofNullable(httpResponse.getHeaderNames()).orElseGet(Collections::emptyList),
                        httpResponse::getHeader);
        if (!responseHeaders.isEmpty()) {
            response.setHeaders(responseHeaders);
        }

        final Data data = new Data();
        data.setServer(server);
        data.setLanguage(language);
        data.setRequest(request);
        data.setResponse(response);

        final TrebllePayload payload = new TrebllePayload();
        payload.setApiKey(treblleProperties.getApiKey());
        payload.setProjectId(treblleProperties.getProjectId());
        payload.setSdk(sdkName);
        payload.setData(data);

        return payload;
    }

    private Map<String, String> readHeaders(Collection<String> headers, UnaryOperator<String> extractor) {
        return dataMasker.mask(
                headers.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(
                                name -> name,
                                name -> Optional.ofNullable(extractor.apply(name)).orElse("EMPTY"),
                                (first, second) -> first)));
    }

    protected String getRandomAPIEndpoint() {
        int randomIndex = RANDOM.nextInt(TREBLLE_API_ENDPOINT.length);
        return TREBLLE_API_ENDPOINT[randomIndex];
    }

    private JsonNode readBody(byte[] body, Consumer<RuntimeError> errorConsumer) {
        if (body.length > 0) {
            try {
                return objectMapper.readTree(body, 0, body.length);
            } catch (IOException exception) {
                RuntimeError error = new RuntimeError();
                error.setSource("onShutdown");
                error.setType("INVALID_JSON");
                error.setMessage("Invalid JSON format");
                errorConsumer.accept(error);
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public void maskAndSendPayload(TrebllePayload payload, byte[] requestBody, byte[] responseBody, Exception chainException) {
        try {
            final List<RuntimeError> errors = new ArrayList<>(2);
            Request request = payload.getData().getRequest();
            request.setBody(
                    Optional.ofNullable(readBody(requestBody, errors::add))
                            .map(dataMasker::mask)
                            .orElse(null)
            );

            Response response = payload.getData().getResponse();
            response.setSize((long) responseBody.length);
            response.setBody(
                    Optional.ofNullable(readBody(responseBody, errors::add))
                            .map(dataMasker::mask)
                            .orElse(null)
            );

            if (chainException != null) {
                StackTraceElement[] stackTrace = chainException.fillInStackTrace().getStackTrace();
                final RuntimeError error = new RuntimeError();
                error.setSource("onError");
                error.setType(chainException.getClass().getName());
                error.setMessage(chainException.getMessage());
                error.setFile(Arrays.stream(stackTrace).findFirst().map(StackTraceElement::getFileName).orElse(null));
                error.setLine(
                        Arrays.stream(stackTrace)
                                .findFirst()
                                .map(StackTraceElement::getLineNumber)
                                .orElse(null));

                errors.add(error);
            }

            if (!errors.isEmpty()) {
                payload.getData().setErrors(errors);
            }

            sendPayload(payload);
        } catch (Exception exception) {
            LOGGER.error("An error occurred while preparing data for Treblle.", exception);
        }
    }

    protected abstract void sendPayload(TrebllePayload payload);

}
