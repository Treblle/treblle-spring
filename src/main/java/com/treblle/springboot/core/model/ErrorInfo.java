package com.treblle.springboot.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * An item of the {@code data.errors} array.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public class ErrorInfo {

    private String source = "onError";
    private String type = "UNHANDLED_EXCEPTION";
    private String message = "";
    private String file = "";
    private int line = 0;

    public ErrorInfo() {
    }

    public ErrorInfo(String source, String type, String message, String file, int line) {
        this.source = source;
        this.type = type;
        this.message = message;
        this.file = file;
        this.line = line;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }
}
