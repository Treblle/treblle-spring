package com.treblle.springboot.collector;

import com.treblle.springboot.core.model.ErrorInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Framework-agnostic carrier for the cheap references captured during a request. The Collector
 * fills this in; the Core reads from it. Keeping this a plain data holder keeps the Core free of
 * any Servlet or Spring dependency.
 */
public class RawRequestData {

    private String method;
    private String url;
    private String routePath;
    private String clientIp;
    private String serverIp;
    private String userAgent;
    private String protocol;
    private String serverSoftware;

    private Map<String, String> requestHeaders = new LinkedHashMap<>();
    private Map<String, String> query = new LinkedHashMap<>();
    private byte[] requestBody;
    private String requestContentType;
    private String requestContentEncoding;
    private List<FilePart> uploadedFiles = new ArrayList<>();

    private int statusCode = 200;
    private Map<String, String> responseHeaders = new LinkedHashMap<>();
    private byte[] responseBody;
    private String responseContentType;
    private String responseContentEncoding;
    private long responseSize;
    private double loadTimeMs;

    /** Errors observed during the request, snapshotted on the request thread. */
    private List<ErrorInfo> errors = Collections.emptyList();

    /** Custom metadata attached during the request, snapshotted on the request thread. */
    private Map<String, Object> metadata = Collections.emptyMap();

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getRoutePath() {
        return routePath;
    }

    public void setRoutePath(String routePath) {
        this.routePath = routePath;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getServerSoftware() {
        return serverSoftware;
    }

    public void setServerSoftware(String serverSoftware) {
        this.serverSoftware = serverSoftware;
    }

    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    public void setRequestHeaders(Map<String, String> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public Map<String, String> getQueryParams() {
        return query;
    }

    public void setQueryParams(Map<String, String> query) {
        this.query = query;
    }

    public byte[] getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(byte[] requestBody) {
        this.requestBody = requestBody;
    }

    public String getRequestContentType() {
        return requestContentType;
    }

    public void setRequestContentType(String requestContentType) {
        this.requestContentType = requestContentType;
    }

    public String getRequestContentEncoding() {
        return requestContentEncoding;
    }

    public void setRequestContentEncoding(String requestContentEncoding) {
        this.requestContentEncoding = requestContentEncoding;
    }

    public List<FilePart> getUploadedFiles() {
        return uploadedFiles;
    }

    public void setUploadedFiles(List<FilePart> uploadedFiles) {
        this.uploadedFiles = uploadedFiles;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public byte[] getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(byte[] responseBody) {
        this.responseBody = responseBody;
    }

    public String getResponseContentType() {
        return responseContentType;
    }

    public void setResponseContentType(String responseContentType) {
        this.responseContentType = responseContentType;
    }

    public String getResponseContentEncoding() {
        return responseContentEncoding;
    }

    public void setResponseContentEncoding(String responseContentEncoding) {
        this.responseContentEncoding = responseContentEncoding;
    }

    public long getResponseSize() {
        return responseSize;
    }

    public void setResponseSize(long responseSize) {
        this.responseSize = responseSize;
    }

    public double getLoadTimeMs() {
        return loadTimeMs;
    }

    public void setLoadTimeMs(double loadTimeMs) {
        this.loadTimeMs = loadTimeMs;
    }

    public List<ErrorInfo> getErrors() {
        return errors;
    }

    public void setErrors(List<ErrorInfo> errors) {
        this.errors = errors == null ? Collections.emptyList() : errors;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata == null ? Collections.emptyMap() : metadata;
    }

    /** Describes a single uploaded file without capturing its contents. */
    public record FilePart(String name, long size, String mime) {
    }
}
