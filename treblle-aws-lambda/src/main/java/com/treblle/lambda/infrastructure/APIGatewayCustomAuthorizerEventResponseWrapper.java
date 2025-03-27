package com.treblle.lambda.infrastructure;

import com.treblle.common.infrastructure.ResponseWrapper;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class APIGatewayCustomAuthorizerEventResponseWrapper implements ResponseWrapper {

    private static final String CONTEXT = "context";

    private static final String STATUS_CODE = "statusCode";

    private final Map<String, Object> response;

    public APIGatewayCustomAuthorizerEventResponseWrapper(Map<String, Object> response) {
        this.response = response;
    }

    @Override
    public int getStatus() {
        return Optional.ofNullable(response.get(CONTEXT))
                .flatMap(APIGatewayCustomAuthorizerEventResponseWrapper::getMapIfValid)
                .flatMap(it -> safeParseInteger(it.get(STATUS_CODE)))
                .orElse(200);
    }

    @Override
    public Collection<String> getHeaderNames() {
        return Collections.emptySet();
    }

    @Override
    public String getHeader(String header) {
        return null;
    }

    private static Optional<Integer> safeParseInteger(Object obj) {
        if (obj == null) {
            return Optional.empty();
        }

        if (obj instanceof Integer) {
            return Optional.of((Integer) obj);
        } else if (obj instanceof String) {
            try {
                return Optional.of(Integer.parseInt((String) obj));
            } catch (NumberFormatException e) {
                // Ignore
            }
        } else {
            String str = obj.toString();
            try {
                return Optional.of(Integer.parseInt(str));
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        return Optional.empty();
    }

    private static Optional<Map<String, Object>> getMapIfValid(Object obj) {
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String)) {
                    return Optional.empty();
                }
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> validMap = (Map<String, Object>) map;
            return Optional.of(validMap);
        }
        return Optional.empty();
    }

}
