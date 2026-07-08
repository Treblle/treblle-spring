package com.treblle.springboot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for the Treblle SDK, bound from the {@code treblle.*} property namespace.
 *
 * <p>Every option can also be supplied through a {@code TREBLLE_*} environment variable
 * thanks to Spring Boot relaxed binding (for example {@code TREBLLE_SDK_TOKEN},
 * {@code TREBLLE_API_KEY}, {@code TREBLLE_DEBUG}). Explicitly declared properties take
 * precedence over environment variables, following the standard Spring property ordering.</p>
 */
@ConfigurationProperties(prefix = "treblle")
public class TreblleProperties {

    /** SDK Token from the Treblle dashboard. Sent as {@code sdk_token} and the {@code x-api-key} header. Required. */
    private String sdkToken;

    /** API Key from the Treblle dashboard. Sent as {@code api_key}. Required. */
    private String apiKey;

    /** Enables verbose local logging of all SDK activity. */
    private boolean debug = false;

    /**
     * Default keys masked out of the box. Secure-by-default: sensitive values never leave the server
     * unless the caller explicitly clears this list. Override {@code treblle.masked-keywords} to
     * customize; set it to empty to disable masking entirely.
     */
    public static final List<String> DEFAULT_MASKED_KEYWORDS = List.of(
            "password", "pwd", "secret", "password_confirmation", "cc", "card_number",
            "ccv", "ssn", "authorization", "api_key");

    /** Keys whose values are masked before sending. If empty, masking is skipped entirely. */
    private List<String> maskedKeywords = new ArrayList<>(DEFAULT_MASKED_KEYWORDS);

    /** Route paths the SDK must not track. Supports exact paths and {@code /prefix/*} wildcards. Case-sensitive. */
    private List<String> excludedPaths = new ArrayList<>();

    /** Ingestion URL. Point at a regional or self-hosted endpoint if needed. */
    private String ingressEndpoint = "https://ingress.treblle.com";

    /** Master switch. When {@code false}, the SDK sends nothing. */
    private boolean enabled = true;

    public String getSdkToken() {
        return sdkToken;
    }

    public void setSdkToken(String sdkToken) {
        this.sdkToken = sdkToken;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public List<String> getMaskedKeywords() {
        return maskedKeywords;
    }

    public void setMaskedKeywords(List<String> maskedKeywords) {
        this.maskedKeywords = maskedKeywords == null ? new ArrayList<>() : maskedKeywords;
    }

    public List<String> getExcludedPaths() {
        return excludedPaths;
    }

    public void setExcludedPaths(List<String> excludedPaths) {
        this.excludedPaths = excludedPaths == null ? new ArrayList<>() : excludedPaths;
    }

    public String getIngressEndpoint() {
        return ingressEndpoint;
    }

    public void setIngressEndpoint(String ingressEndpoint) {
        this.ingressEndpoint = ingressEndpoint;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return {@code true} when both required credentials are present.
     */
    public boolean hasRequiredCredentials() {
        return sdkToken != null && !sdkToken.isBlank()
                && apiKey != null && !apiKey.isBlank();
    }
}
