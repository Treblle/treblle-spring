package com.treblle.springboot.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrebllePropertiesTest {

    @Test
    void hasRequiredCredentialsFalseWhenMissing() {
        TreblleProperties p = new TreblleProperties();
        assertFalse(p.hasRequiredCredentials());
        p.setSdkToken("token");
        assertFalse(p.hasRequiredCredentials());
        p.setApiKey("  ");
        assertFalse(p.hasRequiredCredentials());
    }

    @Test
    void hasRequiredCredentialsTrueWhenBothPresent() {
        TreblleProperties p = new TreblleProperties();
        p.setSdkToken("token");
        p.setApiKey("key");
        assertTrue(p.hasRequiredCredentials());
    }

    @Test
    void sensibleDefaults() {
        TreblleProperties p = new TreblleProperties();
        assertTrue(p.isEnabled());
        assertFalse(p.isDebug());
        assertEquals("https://ingress.treblle.com", p.getIngressEndpoint());
        assertTrue(p.getMaskedKeywords().isEmpty());
        assertTrue(p.getExcludedPaths().isEmpty());
    }

    @Test
    void nullListsAreCoercedToEmpty() {
        TreblleProperties p = new TreblleProperties();
        p.setMaskedKeywords(null);
        p.setExcludedPaths(null);
        assertEquals(List.of(), p.getMaskedKeywords());
        assertEquals(List.of(), p.getExcludedPaths());
    }
}
