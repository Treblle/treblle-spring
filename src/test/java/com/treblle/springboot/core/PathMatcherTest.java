package com.treblle.springboot.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathMatcherTest {

    @Test
    void exactExclusionMatches() {
        PathMatcher m = new PathMatcher(List.of("/health"));
        assertFalse(m.shouldTrack("/health"));
        assertTrue(m.shouldTrack("/healthy"));
        assertTrue(m.shouldTrack("/api/health"));
    }

    @Test
    void wildcardPrefixExcludesEverythingBelow() {
        PathMatcher m = new PathMatcher(List.of("/admin/*"));
        assertFalse(m.shouldTrack("/admin/users"));
        assertFalse(m.shouldTrack("/admin/settings/deep"));
        assertTrue(m.shouldTrack("/administrator"));
    }

    @Test
    void matchingIsCaseSensitive() {
        PathMatcher m = new PathMatcher(List.of("/Health"));
        assertTrue(m.shouldTrack("/health"));
        assertFalse(m.shouldTrack("/Health"));
    }

    @Test
    void skipsStaticAssetsAutomatically() {
        PathMatcher m = new PathMatcher(List.of());
        assertFalse(m.shouldTrack("/styles/app.css"));
        assertFalse(m.shouldTrack("/app.js"));
        assertFalse(m.shouldTrack("/logo.png"));
        assertFalse(m.shouldTrack("/favicon.ico"));
        assertFalse(m.shouldTrack("/.well-known/acme-challenge/x"));
        assertFalse(m.shouldTrack("/config/.env"));
    }

    @Test
    void tracksNormalApiPaths() {
        PathMatcher m = new PathMatcher(List.of());
        assertTrue(m.shouldTrack("/v1/users"));
        assertTrue(m.shouldTrack("/api/orders/123"));
    }

    @Test
    void ignoresQueryStringWhenCheckingExtension() {
        PathMatcher m = new PathMatcher(List.of());
        assertTrue(m.shouldTrack("/v1/users"));
        assertFalse(m.shouldTrack("/app.js?v=2"));
    }
}
