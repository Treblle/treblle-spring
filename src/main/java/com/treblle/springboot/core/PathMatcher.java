package com.treblle.springboot.core;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Decides whether a request path should be tracked.
 *
 * <p>Skips user-configured {@code excludedPaths} (exact and {@code /prefix/*} wildcard,
 * case-sensitive) and automatically skips static/infrastructure resources (assets, well-known
 * paths, favicons, etc.).</p>
 */
public class PathMatcher {

    private static final Set<String> STATIC_EXTENSIONS = Set.of(
            ".css", ".js", ".mjs", ".map", ".ico", ".png", ".jpg", ".jpeg", ".gif", ".svg",
            ".webp", ".bmp", ".woff", ".woff2", ".ttf", ".eot", ".otf", ".mp4", ".webm",
            ".mp3", ".wav", ".pdf", ".zip", ".gz", ".txt", ".xml", ".html", ".htm", ".env"
    );

    private final List<String> excludedPaths;

    public PathMatcher(List<String> excludedPaths) {
        this.excludedPaths = excludedPaths == null ? List.of() : excludedPaths;
    }

    /**
     * @return {@code true} if the request at {@code path} should be captured and sent.
     */
    public boolean shouldTrack(String path) {
        if (path == null || path.isEmpty()) {
            return true;
        }
        if (isStaticResource(path)) {
            return false;
        }
        return !isExcluded(path);
    }

    public boolean isExcluded(String path) {
        for (String pattern : excludedPaths) {
            if (pattern == null || pattern.isEmpty()) {
                continue;
            }
            if (pattern.endsWith("/*")) {
                String prefix = pattern.substring(0, pattern.length() - 1); // keep trailing slash
                if (path.startsWith(prefix)) {
                    return true;
                }
            } else if (pattern.equals(path)) {
                return true;
            }
        }
        return false;
    }

    public boolean isStaticResource(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.contains("/.well-known/") || lower.startsWith("/.well-known")) {
            return true;
        }
        if (lower.equals("/favicon.ico")) {
            return true;
        }
        int query = lower.indexOf('?');
        if (query >= 0) {
            lower = lower.substring(0, query);
        }
        int dot = lower.lastIndexOf('.');
        if (dot >= 0) {
            String ext = lower.substring(dot);
            return STATIC_EXTENSIONS.contains(ext);
        }
        return false;
    }
}
