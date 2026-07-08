package com.treblle.springboot.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * The {@code data} object of the Treblle payload.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Data {

    private ServerInfo server;
    private LanguageInfo language;
    private RequestInfo request;
    private ResponseInfo response;
    private List<ErrorInfo> errors;
    private List<QueryInfo> queries;
    private Map<String, Object> metadata;

    public ServerInfo getServer() {
        return server;
    }

    public void setServer(ServerInfo server) {
        this.server = server;
    }

    public LanguageInfo getLanguage() {
        return language;
    }

    public void setLanguage(LanguageInfo language) {
        this.language = language;
    }

    public RequestInfo getRequest() {
        return request;
    }

    public void setRequest(RequestInfo request) {
        this.request = request;
    }

    public ResponseInfo getResponse() {
        return response;
    }

    public void setResponse(ResponseInfo response) {
        this.response = response;
    }

    public List<ErrorInfo> getErrors() {
        return errors;
    }

    public void setErrors(List<ErrorInfo> errors) {
        this.errors = errors;
    }

    public List<QueryInfo> getQueries() {
        return queries;
    }

    public void setQueries(List<QueryInfo> queries) {
        this.queries = queries;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
