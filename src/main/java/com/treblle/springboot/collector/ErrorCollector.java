package com.treblle.springboot.collector;

import com.treblle.springboot.core.model.ErrorInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Thread-bound collector for errors that occur on the host API during a request.
 *
 * <p>The SDK observes errors but never swallows, rethrows, or alters them — the framework's own
 * error handling behaves exactly as it would without Treblle installed.</p>
 */
public final class ErrorCollector {

    private static final int MAX_ERRORS = 100;

    private static final ThreadLocal<List<ErrorInfo>> HOLDER = new ThreadLocal<>();

    /** Tracks Throwable identities already recorded this request to avoid duplicates. */
    private static final ThreadLocal<Set<Throwable>> SEEN =
            ThreadLocal.withInitial(() -> Collections.newSetFromMap(new java.util.IdentityHashMap<>()));

    private ErrorCollector() {
    }

    /**
     * Records a Throwable raised during the request. {@code source} is one of
     * {@code onException}, {@code onError}, {@code onShutdown}.
     */
    public static void record(String source, Throwable throwable) {
        try {
            if (throwable == null) {
                return;
            }
            if (!SEEN.get().add(throwable)) {
                return; // already recorded this exact throwable during this request
            }
            List<ErrorInfo> list = HOLDER.get();
            if (list == null) {
                list = new ArrayList<>();
                HOLDER.set(list);
            }
            if (list.size() >= MAX_ERRORS) {
                return;
            }

            String file = "";
            int line = 0;
            StackTraceElement[] stack = throwable.getStackTrace();
            if (stack != null && stack.length > 0) {
                file = stack[0].getFileName() != null ? stack[0].getFileName() : stack[0].getClassName();
                line = Math.max(stack[0].getLineNumber(), 0);
            }

            String message = throwable.getMessage();
            if (message == null) {
                message = throwable.getClass().getName();
            }

            list.add(new ErrorInfo(source, "UNHANDLED_EXCEPTION", message, file, line));
        } catch (Throwable ignored) {
            // Never surface SDK errors to the host application.
        }
    }

    /** @return an immutable snapshot of collected errors, or an empty list. */
    public static List<ErrorInfo> snapshot() {
        List<ErrorInfo> list = HOLDER.get();
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(list));
    }

    /** Clears errors for the current thread. Called by the SDK when the request completes. */
    public static void clear() {
        HOLDER.remove();
        SEEN.remove();
    }
}
