package com.treblle.springboot.filter;

import com.treblle.springboot.collector.ErrorCollector;
import com.treblle.springboot.collector.RawRequestData;
import com.treblle.springboot.config.TreblleProperties;
import com.treblle.springboot.core.PathMatcher;
import com.treblle.springboot.core.PayloadFactory;
import com.treblle.springboot.core.TreblleLogger;
import com.treblle.springboot.core.model.TrebllePayload;
import com.treblle.springboot.metadata.TreblleMetadata;
import com.treblle.springboot.transport.TreblleClient;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Collector. A {@link OncePerRequestFilter} that wraps each request/response, lets the chain
 * run, flushes the response body to the client first, then normalizes → masks → sends off-thread.
 *
 * <p>Hot-path discipline: during the request we only capture cheap references. All expensive work
 * (payload building, masking, serialization, compression, HTTP) happens after the response is
 * flushed, on the transport's worker threads.</p>
 *
 * <p>The filter never swallows or alters host errors. Exceptions from the chain are recorded then
 * rethrown unchanged so the framework's own error handling behaves exactly as normal.</p>
 */
public class TreblleFilter extends OncePerRequestFilter {

    private final TreblleProperties properties;
    private final PayloadFactory payloadFactory;
    private final TreblleClient client;
    private final PathMatcher pathMatcher;
    private final TreblleLogger logger;
    private final boolean active;

    public TreblleFilter(TreblleProperties properties,
                         PayloadFactory payloadFactory,
                         TreblleClient client,
                         PathMatcher pathMatcher,
                         TreblleLogger logger,
                         boolean active) {
        this.properties = properties;
        this.payloadFactory = payloadFactory;
        this.client = client;
        this.pathMatcher = pathMatcher;
        this.logger = logger;
        this.active = active;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // When disabled or missing credentials, act as a pass-through with zero overhead.
        if (!active || !properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        if (!pathMatcher.shouldTrack(path)) {
            logger.info("Skipping untracked path: " + path);
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long start = System.nanoTime();
        Throwable captured = null;
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } catch (Throwable t) {
            // Observe, never interfere: record then rethrow unchanged.
            ErrorCollector.record("onException", t);
            captured = t;
            throw t;
        } finally {
            double loadTimeMs = (System.nanoTime() - start) / 1_000_000.0;
            try {
                // When an exception propagated out of the chain, the servlet container turns it into
                // a 5xx during a later ERROR dispatch that this filter does not observe. Reflect that
                // in the captured status so the payload matches what the client ultimately receives.
                int effectiveStatus = wrappedResponse.getStatus();
                if (captured != null && effectiveStatus < 500) {
                    effectiveStatus = 500;
                }
                // If a 5xx slipped through without an exception, record it as onError.
                if (captured == null && effectiveStatus >= 500) {
                    ErrorCollector.record("onError",
                            new RuntimeException("Server responded with status " + effectiveStatus));
                }
                process(wrappedRequest, wrappedResponse, loadTimeMs, effectiveStatus);
            } catch (Throwable sdkError) {
                // The SDK must never break the host application.
                logger.error("Treblle capture failed; request unaffected.", sdkError);
            } finally {
                // CRITICAL: flush cached response body back to the real client.
                try {
                    wrappedResponse.copyBodyToResponse();
                } catch (Throwable t) {
                    logger.error("Failed to copy response body.", t);
                }
                ErrorCollector.clear();
                TreblleMetadata.clear();
            }
        }
    }

    private void process(ContentCachingRequestWrapper request,
                         ContentCachingResponseWrapper response,
                         double loadTimeMs,
                         int effectiveStatus) {
        RawRequestData raw = new RawRequestData();

        raw.setMethod(request.getMethod());
        raw.setUrl(buildFullUrl(request));
        raw.setRoutePath(resolveRoutePath(request));
        raw.setClientIp(resolveClientIp(request));
        raw.setServerIp(request.getLocalAddr());
        raw.setUserAgent(request.getHeader("User-Agent"));
        raw.setProtocol(request.getProtocol());
        raw.setServerSoftware(request.getServletContext() != null
                ? request.getServletContext().getServerInfo() : null);

        raw.setRequestHeaders(collectRequestHeaders(request));
        raw.setQueryParams(collectQueryParams(request));
        raw.setRequestContentType(request.getContentType());
        raw.setRequestBody(request.getContentAsByteArray());

        raw.setStatusCode(effectiveStatus);
        raw.setResponseHeaders(collectResponseHeaders(response));
        raw.setResponseContentType(response.getContentType());
        byte[] responseBody = response.getContentAsByteArray();
        raw.setResponseBody(responseBody);
        raw.setResponseSize(responseBody == null ? 0 : responseBody.length);
        raw.setLoadTimeMs(loadTimeMs);

        TrebllePayload payload = payloadFactory.build(raw);
        client.send(payload);
    }

    private String buildFullUrl(HttpServletRequest request) {
        StringBuffer url = request.getRequestURL();
        String query = request.getQueryString();
        if (query != null && !query.isEmpty()) {
            url.append('?').append(query);
        }
        return url.toString();
    }

    private String resolveRoutePath(HttpServletRequest request) {
        Object pattern = request.getAttribute(
                "org.springframework.web.servlet.HandlerMapping.bestMatchingPattern");
        if (pattern != null) {
            return pattern.toString();
        }
        return request.getRequestURI();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // Last value wins per spec; but for client IP the first hop is the real client.
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private Map<String, String> collectRequestHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        var names = request.getHeaderNames();
        if (names != null) {
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                var values = request.getHeaders(name);
                String last = null;
                while (values != null && values.hasMoreElements()) {
                    last = values.nextElement(); // last value wins
                }
                if (last != null) {
                    headers.put(name.toLowerCase(), last);
                }
            }
        }
        return headers;
    }

    private Map<String, String> collectResponseHeaders(ContentCachingResponseWrapper response) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (String name : response.getHeaderNames()) {
            List<String> values = new ArrayList<>(response.getHeaders(name));
            if (!values.isEmpty()) {
                headers.put(name.toLowerCase(), values.get(values.size() - 1)); // last value wins
            }
        }
        return headers;
    }

    /**
     * Parses the raw query string so the {@code query} map contains only URL query parameters (not
     * form-body parameters, which {@code getParameterMap()} would merge in). Repeated keys: last
     * value wins.
     */
    private Map<String, String> collectQueryParams(HttpServletRequest request) {
        Map<String, String> params = new LinkedHashMap<>();
        String queryString = request.getQueryString();
        if (queryString == null || queryString.isEmpty()) {
            return params;
        }
        for (String pair : queryString.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            String key = eq >= 0 ? pair.substring(0, eq) : pair;
            String value = eq >= 0 ? pair.substring(eq + 1) : "";
            params.put(urlDecode(key), urlDecode(value)); // last value wins
        }
        return params;
    }

    private String urlDecode(String s) {
        try {
            return java.net.URLDecoder.decode(s, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }
}
