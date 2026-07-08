package com.treblle.springboot.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Root Treblle Ingress payload. Serializes to the Treblle Payload Schema.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrebllePayload {

    @JsonProperty("sdk_token")
    private String sdkToken;

    @JsonProperty("api_key")
    private String apiKey;

    private String sdk;

    private int version;

    @JsonProperty("internal_id")
    private String internalId;

    @JsonProperty("internal_name")
    private String internalName;

    private Data data;

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

    public String getSdk() {
        return sdk;
    }

    public void setSdk(String sdk) {
        this.sdk = sdk;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getInternalId() {
        return internalId;
    }

    public void setInternalId(String internalId) {
        this.internalId = internalId;
    }

    public String getInternalName() {
        return internalName;
    }

    public void setInternalName(String internalName) {
        this.internalName = internalName;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }
}
