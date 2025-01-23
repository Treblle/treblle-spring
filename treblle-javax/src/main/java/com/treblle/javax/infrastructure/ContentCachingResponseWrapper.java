package com.treblle.javax.infrastructure;

import com.treblle.common.infrastructure.ResponseWrapper;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ContentCachingResponseWrapper extends HttpServletResponseWrapper implements ResponseWrapper {

    private final ByteArrayOutputStream content = new ByteArrayOutputStream(1024);
    private ServletOutputStream outputStream;
    private PrintWriter writer;
    private Integer contentLength;
    private String contentType;

    public ContentCachingResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    public void sendError(int sc) throws IOException {
        this.copyBodyToResponse(false);

        try {
            super.sendError(sc);
        } catch (IllegalStateException var3) {
            super.setStatus(sc);
        }

    }

    public void sendError(int sc, String msg) throws IOException {
        this.copyBodyToResponse(false);

        try {
            super.sendError(sc, msg);
        } catch (IllegalStateException var4) {
            super.setStatus(sc);
        }

    }

    public void sendRedirect(String location) throws IOException {
        this.copyBodyToResponse(false);
        super.sendRedirect(location);
    }

    public ServletOutputStream getOutputStream() throws IOException {
        if (this.outputStream == null) {
            this.outputStream = new ResponseServletOutputStream(this.getResponse().getOutputStream());
        }

        return this.outputStream;
    }

    public PrintWriter getWriter() throws IOException {
        if (this.writer == null) {
            String characterEncoding = this.getCharacterEncoding();
            this.writer = characterEncoding != null ? new ResponsePrintWriter(characterEncoding) : new ResponsePrintWriter("ISO-8859-1");
        }

        return this.writer;
    }

    public void flushBuffer() throws IOException {
    }

    public void setContentType(String type) {
        this.contentType = type;
    }

    public String getContentType() {
        return this.contentType;
    }

    public boolean containsHeader(String name) {
        if ("Content-Length".equalsIgnoreCase(name)) {
            return this.contentLength != null;
        } else if ("Content-Type".equalsIgnoreCase(name)) {
            return this.contentType != null;
        } else {
            return super.containsHeader(name);
        }
    }

    public void setHeader(String name, String value) {
        if ("Content-Length".equalsIgnoreCase(name)) {
            this.contentLength = Integer.valueOf(value);
        } else if ("Content-Type".equalsIgnoreCase(name)) {
            this.contentType = value;
        } else {
            super.setHeader(name, value);
        }

    }

    public void addHeader(String name, String value) {
        if ("Content-Length".equalsIgnoreCase(name)) {
            this.contentLength = Integer.valueOf(value);
        } else if ("Content-Type".equalsIgnoreCase(name)) {
            this.contentType = value;
        } else {
            super.addHeader(name, value);
        }

    }

    public void setIntHeader(String name, int value) {
        if ("Content-Length".equalsIgnoreCase(name)) {
            this.contentLength = value;
        } else {
            super.setIntHeader(name, value);
        }

    }

    public void addIntHeader(String name, int value) {
        if ("Content-Length".equalsIgnoreCase(name)) {
            this.contentLength = value;
        } else {
            super.addIntHeader(name, value);
        }

    }

    public String getHeader(String name) {
        if ("Content-Length".equalsIgnoreCase(name)) {
            return this.contentLength != null ? this.contentLength.toString() : null;
        } else {
            return "Content-Type".equalsIgnoreCase(name) ? this.contentType : super.getHeader(name);
        }
    }

    public Collection<String> getHeaders(String name) {
        if ("Content-Length".equalsIgnoreCase(name)) {
            return this.contentLength != null ? Collections.singleton(this.contentLength.toString()) : Collections.emptySet();
        } else if ("Content-Type".equalsIgnoreCase(name)) {
            return this.contentType != null ? Collections.singleton(this.contentType) : Collections.emptySet();
        } else {
            return super.getHeaders(name);
        }
    }

    public Collection<String> getHeaderNames() {
        Collection<String> headerNames = super.getHeaderNames();
        if (this.contentLength == null && this.contentType == null) {
            return headerNames;
        } else {
            List<String> result = new ArrayList(headerNames);
            if (this.contentLength != null) {
                result.add("Content-Length");
            }

            if (this.contentType != null) {
                result.add("Content-Type");
            }

            return result;
        }
    }

    public void resetBuffer() {
        this.content.reset();
    }

    public void reset() {
        super.reset();
        this.content.reset();
    }

    public byte[] getContentAsByteArray() {
        return this.content.toByteArray();
    }

    public int getContentSize() {
        return this.content.size();
    }

    public void copyBodyToResponse() throws IOException {
        this.copyBodyToResponse(true);
    }

    protected void copyBodyToResponse(boolean complete) throws IOException {
        if (this.content.size() > 0) {
            HttpServletResponse rawResponse = (HttpServletResponse)this.getResponse();
            if (!rawResponse.isCommitted()) {
                if (complete || this.contentLength != null) {
                    if (rawResponse.getHeader("Transfer-Encoding") == null) {
                        rawResponse.setContentLength(complete ? this.content.size() : this.contentLength);
                    }

                    this.contentLength = null;
                }

                if (complete || this.contentType != null) {
                    rawResponse.setContentType(this.contentType);
                    this.contentType = null;
                }
            }

            this.content.writeTo(rawResponse.getOutputStream());
            this.content.reset();
            if (complete) {
                super.flushBuffer();
            }
        }

    }

    private class ResponseServletOutputStream extends ServletOutputStream {

        private final ServletOutputStream os;

        public ResponseServletOutputStream(ServletOutputStream os) {
            this.os = os;
        }

        public void write(int b) throws IOException {
            ContentCachingResponseWrapper.this.content.write(b);
        }

        public void write(byte[] b, int off, int len) throws IOException {
            ContentCachingResponseWrapper.this.content.write(b, off, len);
        }

        public boolean isReady() {
            return this.os.isReady();
        }

        public void setWriteListener(WriteListener writeListener) {
            this.os.setWriteListener(writeListener);
        }
    }

    private class ResponsePrintWriter extends PrintWriter {
        public ResponsePrintWriter(String characterEncoding) throws UnsupportedEncodingException {
            super(new OutputStreamWriter(ContentCachingResponseWrapper.this.content, characterEncoding));
        }

        public void write(char[] buf, int off, int len) {
            super.write(buf, off, len);
            super.flush();
        }

        public void write(String s, int off, int len) {
            super.write(s, off, len);
            super.flush();
        }

        public void write(int c) {
            super.write(c);
            super.flush();
        }
    }

}
