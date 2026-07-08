package com.treblle.springboot.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thin wrapper around SLF4J that only emits when debug mode is enabled.
 *
 * <p>Debug mode exists to help developers diagnose SDK issues. When disabled, the SDK stays
 * completely silent. This helper never throws, so logging can never disrupt the host application.</p>
 */
public final class TreblleLogger {

    private static final Logger LOG = LoggerFactory.getLogger("com.treblle");
    private static final String PREFIX = "[Treblle] ";

    private final boolean debug;

    public TreblleLogger(boolean debug) {
        this.debug = debug;
    }

    public boolean isDebug() {
        return debug;
    }

    public void info(String message) {
        if (debug) {
            safe(() -> LOG.info(PREFIX + message));
        }
    }

    public void warn(String message) {
        if (debug) {
            safe(() -> LOG.warn(PREFIX + message));
        }
    }

    public void error(String message, Throwable t) {
        if (debug) {
            safe(() -> LOG.error(PREFIX + message, t));
        }
    }

    private void safe(Runnable r) {
        try {
            r.run();
        } catch (Throwable ignored) {
            // Logging must never surface as an error in the host application.
        }
    }
}
