package com.treblle.jakarta.infrastructure;

import com.treblle.common.infrastructure.RequestWrapper;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.*;

public class ContentCachingRequestWrapper extends HttpServletRequestWrapper implements RequestWrapper {

    private static final String POST_REQUEST = "POST";

    private final ByteArrayOutputStream cachedContent;
    private final Integer contentCacheLimit;
    private ServletInputStream inputStream;
    private BufferedReader reader;

    public ContentCachingRequestWrapper(HttpServletRequest request) {
        super(request);
        int contentLength = request.getContentLength();
        this.cachedContent = new ByteArrayOutputStream(contentLength >= 0 ? contentLength : 1024);
        this.contentCacheLimit = null;
    }

    public ContentCachingRequestWrapper(HttpServletRequest request, int contentCacheLimit) {
        super(request);
        this.cachedContent = new ByteArrayOutputStream(contentCacheLimit);
        this.contentCacheLimit = contentCacheLimit;
    }

    public ServletInputStream getInputStream() throws IOException {
        if (this.inputStream == null) {
            this.inputStream = new ContentCachingInputStream(this.getRequest().getInputStream());
        }

        return this.inputStream;
    }

    public String getCharacterEncoding() {
        String enc = super.getCharacterEncoding();
        return enc != null ? enc : "ISO-8859-1";
    }

    public BufferedReader getReader() throws IOException {
        if (this.reader == null) {
            this.reader = new BufferedReader(new InputStreamReader(this.getInputStream(), this.getCharacterEncoding()));
        }

        return this.reader;
    }

    public String getParameter(String name) {
        if (this.cachedContent.size() == 0 && this.isFormPost()) {
            this.writeRequestParametersToCachedContent();
        }

        return super.getParameter(name);
    }

    public Map<String, String[]> getParameterMap() {
        if (this.cachedContent.size() == 0 && this.isFormPost()) {
            this.writeRequestParametersToCachedContent();
        }

        return super.getParameterMap();
    }

    public Enumeration<String> getParameterNames() {
        if (this.cachedContent.size() == 0 && this.isFormPost()) {
            this.writeRequestParametersToCachedContent();
        }

        return super.getParameterNames();
    }

    public String[] getParameterValues(String name) {
        if (this.cachedContent.size() == 0 && this.isFormPost()) {
            this.writeRequestParametersToCachedContent();
        }

        return super.getParameterValues(name);
    }

    private boolean isFormPost() {
        String contentType = this.getContentType();
        return contentType != null && contentType.contains("application/x-www-form-urlencoded") && POST_REQUEST.equalsIgnoreCase(this.getMethod());
    }

    private void writeRequestParametersToCachedContent() {
        try {
            if (this.cachedContent.size() == 0) {
                String requestEncoding = this.getCharacterEncoding();
                Map<String, String[]> form = super.getParameterMap();
                Iterator<String> nameIterator = form.keySet().iterator();

                while(nameIterator.hasNext()) {
                    String name = (String)nameIterator.next();
                    List<String> values = Arrays.asList((String[])form.get(name));
                    Iterator<String> valueIterator = values.iterator();

                    while(valueIterator.hasNext()) {
                        String value = (String)valueIterator.next();
                        this.cachedContent.write(URLEncoder.encode(name, requestEncoding).getBytes());
                        if (value != null) {
                            this.cachedContent.write(61);
                            this.cachedContent.write(URLEncoder.encode(value, requestEncoding).getBytes());
                            if (valueIterator.hasNext()) {
                                this.cachedContent.write(38);
                            }
                        }
                    }

                    if (nameIterator.hasNext()) {
                        this.cachedContent.write(38);
                    }
                }
            }

        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write request parameters to cached content", ex);
        }
    }

    public byte[] getContentAsByteArray() {
        return this.cachedContent.toByteArray();
    }

    protected void handleContentOverflow(int contentCacheLimit) {
    }

    @Override
    public String getUrl() {
        return getScheme() + "://" + getServerName() + ":" + getServerPort() + getRequestURI();
    }

    @Override
    public String getServerAddr() {
        return null;
    }

    private class ContentCachingInputStream extends ServletInputStream {

        private final ServletInputStream is;
        private boolean overflow = false;

        public ContentCachingInputStream(ServletInputStream is) {
            this.is = is;
        }

        public int read() throws IOException {
            int ch = this.is.read();
            if (ch != -1 && !this.overflow) {
                if (ContentCachingRequestWrapper.this.contentCacheLimit != null && ContentCachingRequestWrapper.this.cachedContent.size() == ContentCachingRequestWrapper.this.contentCacheLimit) {
                    this.overflow = true;
                    ContentCachingRequestWrapper.this.handleContentOverflow(ContentCachingRequestWrapper.this.contentCacheLimit);
                } else {
                    ContentCachingRequestWrapper.this.cachedContent.write(ch);
                }
            }

            return ch;
        }

        public int read(byte[] b) throws IOException {
            int count = this.is.read(b);
            this.writeToCache(b, 0, count);
            return count;
        }

        private void writeToCache(final byte[] b, final int off, int count) {
            if (!this.overflow && count > 0) {
                if (ContentCachingRequestWrapper.this.contentCacheLimit != null && count + ContentCachingRequestWrapper.this.cachedContent.size() > ContentCachingRequestWrapper.this.contentCacheLimit) {
                    this.overflow = true;
                    ContentCachingRequestWrapper.this.cachedContent.write(b, off, ContentCachingRequestWrapper.this.contentCacheLimit - ContentCachingRequestWrapper.this.cachedContent.size());
                    ContentCachingRequestWrapper.this.handleContentOverflow(ContentCachingRequestWrapper.this.contentCacheLimit);
                    return;
                }

                ContentCachingRequestWrapper.this.cachedContent.write(b, off, count);
            }

        }

        public int read(final byte[] b, final int off, final int len) throws IOException {
            int count = this.is.read(b, off, len);
            this.writeToCache(b, off, count);
            return count;
        }

        public int readLine(final byte[] b, final int off, final int len) throws IOException {
            int count = this.is.readLine(b, off, len);
            this.writeToCache(b, off, count);
            return count;
        }

        public boolean isFinished() {
            return this.is.isFinished();
        }

        public boolean isReady() {
            return this.is.isReady();
        }

        public void setReadListener(ReadListener readListener) {
            this.is.setReadListener(readListener);
        }
    }

}
