package com.vrudenko.kanban_board.security;

import java.io.IOException;
import java.net.URI;

import com.vrudenko.kanban_board.constant.ErrorCode;
import com.vrudenko.kanban_board.constant.MetricNames;
import com.vrudenko.kanban_board.constant.SecurityEventLog;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Emit the RFC 7807 ProblemDetail envelope for a genuinely unauthenticated request, the one
 * rejection GlobalExceptionHandler structurally cannot reach.
 *
 * This fires inside Spring Security's ExceptionTranslationFilter, before
 * DispatcherServlet runs, so no @ExceptionHandler is ever invoked. That is why this is a
 * second, independent producer rather than a shared method: the two run at structurally different
 * points in the request lifecycle and only converge on the JSON shape. Check the other producer for
 * drift when either envelope changes.
 */
@Component
@RequiredArgsConstructor
public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log =
            LoggerFactory.getLogger(ProblemDetailAuthenticationEntryPoint.class);

    private final ObjectMapper objectMapper;

    private final MeterRegistry meterRegistry;

    // Registered at startup so Prometheus' increase() sees the first event.
    @PostConstruct
    void registerCounters() {
        Counter.builder(MetricNames.AUTH_FAILURES)
                .tag(MetricNames.TAG_REASON, MetricNames.REASON_UNAUTHENTICATED)
                .description("Requests rejected for missing or wrong credentials")
                .register(meterRegistry);
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException {
        var problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.UNAUTHORIZED, "Authentication is required");
        problem.setProperty(ErrorCode.CODE_PROPERTY, ErrorCode.UNAUTHENTICATED.name());

        meterRegistry
                .counter(
                        MetricNames.AUTH_FAILURES,
                        MetricNames.TAG_REASON,
                        MetricNames.REASON_UNAUTHENTICATED)
                .increment();
        log.atWarn()
                .addKeyValue(SecurityEventLog.EVENT, SecurityEventLog.EVENT_AUTH_FAILURE)
                .addKeyValue(SecurityEventLog.REASON, MetricNames.REASON_UNAUTHENTICATED)
                .addKeyValue(SecurityEventLog.HTTP_METHOD, request.getMethod())
                .addKeyValue(SecurityEventLog.PATH, request.getServletPath())
                .log(SecurityEventLog.MESSAGE);

        // GlobalExceptionHandler's arms get "instance" for free from HttpEntityMethodProcessor,
        // which only runs for a request reaching a HandlerMethod. This entry point runs earlier,
        // so set it explicitly to keep both producers' key sets identical.
        problem.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), problem);
    }
}
