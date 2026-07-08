package com.treblle.springboot.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The {@code data.language} object.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public class LanguageInfo {

    private String name;
    private String version;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
