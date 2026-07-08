package com.treblle.springboot.filter;

import com.treblle.springboot.collector.ErrorCollector;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

/**
 * A {@link HandlerExceptionResolver} that records exceptions raised while handling a request into
 * the {@link ErrorCollector}, so they land in the payload's {@code errors} array.
 *
 * <p>It observes only: it always returns {@code null} so Spring's own exception handling proceeds
 * exactly as it would without Treblle installed. It never swallows or alters the exception.</p>
 *
 * <p>Runs at the lowest precedence so it sees the exception after (or alongside) the application's
 * own resolvers without preventing them from producing the response.</p>
 */
public class TreblleExceptionResolver implements HandlerExceptionResolver, Ordered {

    @Override
    public ModelAndView resolveException(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Object handler,
                                         Exception ex) {
        ErrorCollector.record("onException", ex);
        return null; // never handle; let the framework resolve as normal
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
