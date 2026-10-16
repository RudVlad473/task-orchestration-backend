package com.vrudenko.kanban_board.e2e.observability;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import com.vrudenko.kanban_board.constant.ApiPaths;
import com.vrudenko.kanban_board.dto.user_dto.SigninRequestDTO;
import com.vrudenko.kanban_board.support.fixtures.AbstractAppE2ETest;
import com.vrudenko.kanban_board.support.observability.InMemorySpanExporterConfiguration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.restassured.http.ContentType;
import io.vavr.control.Try;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;

/**
 * Proves a request writes its trace and span ids into the JSON log, that Spring Security's own
 * observations are not exported as spans, and that management-port requests produce no span.
 *
 * Why this is the way it is:
 *
 * - AutoConfigureObservability is required: Spring Boot disables tracing in a SpringBootTest
 *   unless the class carries it, and without tracing the ids never appear and no span is
 *   recorded, so every case would chase a ghost.
 * - The management-port case characterises Spring Boot, not application code. Boot does not
 *   observe the management child context, so scrapes and probes already produce no span and no
 *   http_server_requests series, and no ObservationPredicate is needed. The case sits beside a
 *   main-port request that does export a SERVER span, so an empty exporter cannot pass it, and it
 *   fails loudly if a Spring Boot upgrade starts observing the management port.
 * - The batch span processor exports asynchronously, so the span assertions wait with Awaitility.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureObservability
@Import(InMemorySpanExporterConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
public class TraceLogCorrelationE2ETest extends AbstractAppE2ETest {

    private static final String TRACE_ID_PATTERN = "^[0-9a-f]{32}$";
    private static final String SPAN_ID_PATTERN = "^[0-9a-f]{16}$";
    private static final Duration SPAN_WAIT = Duration.ofSeconds(30);

    @LocalManagementPort private int managementPort;

    @Autowired private ObjectMapper objectMapper;
    @Autowired private InMemorySpanExporter spanExporter;

    @BeforeEach
    void resetSpans() {
        spanExporter.reset();
    }

    private List<JsonNode> jsonLines(String output) {
        return output.lines()
                .filter(line -> line.startsWith("{"))
                .map(line -> Try.of(() -> objectMapper.readTree(line)).getOrNull())
                .filter(Objects::nonNull)
                .toList();
    }

    private List<SpanData> finishedSpans() {
        return spanExporter.getFinishedSpanItems();
    }

    private void getBoardsWithSession() {
        var cookie = signin();
        var statusCode =
                given().cookie(cookie.getFirst(), cookie.getSecond())
                        .when()
                        .get(ApiPaths.BOARDS)
                        .then()
                        .extract()
                        .statusCode();
        Assertions.assertThat(statusCode).isEqualTo(HttpStatus.OK.value());
    }

    private void awaitServerSpan() {
        Awaitility.await()
                .atMost(SPAN_WAIT)
                .untilAsserted(
                        () ->
                                Assertions.assertThat(finishedSpans())
                                        .anyMatch(span -> span.getKind() == SpanKind.SERVER));
    }

    @Nested
    class LogCorrelationTest {
        @Test
        void shouldWriteTraceAndSpanIds_whenSigninFailsInsideATracedRequest(CapturedOutput output) {
            // arrange
            var offset = output.getAll().length();

            // act
            var statusCode =
                    given().contentType(ContentType.JSON)
                            .body(
                                    SigninRequestDTO.builder()
                                            .email(getOwningUser().getEmail())
                                            .password(getOwningUserPassword() + "x")
                                            .build())
                            .when()
                            .post(ApiPaths.SIGNIN)
                            .then()
                            .extract()
                            .statusCode();

            // assert
            Assertions.assertThat(statusCode).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            var warnings =
                    jsonLines(output.getAll().substring(offset)).stream()
                            .filter(line -> line.has("security_event"))
                            .toList();
            Assertions.assertThat(warnings).hasSize(1);
            var warning = warnings.get(0);
            Assertions.assertThat(warning.path("log").path("level").asText()).isEqualTo("WARN");
            Assertions.assertThat(warning.path("traceId").asText()).matches(TRACE_ID_PATTERN);
            Assertions.assertThat(warning.path("spanId").asText()).matches(SPAN_ID_PATTERN);
        }
    }

    @Nested
    class ServerSpanTest {
        @Test
        void shouldExportServerSpan_whenBoardsAreRequestedWithASession() {
            // act
            getBoardsWithSession();

            // assert
            awaitServerSpan();
            Assertions.assertThat(finishedSpans())
                    .anyMatch(
                            span ->
                                    span.getKind() == SpanKind.SERVER
                                            && span.getName().contains(ApiPaths.BOARDS));
        }
    }

    @Nested
    class SecuritySpanTest {
        @Test
        void shouldExportNoSecuritySpan_whenARequestIsServed() {
            // act
            getBoardsWithSession();

            // assert: the SERVER span exists, so an empty exporter cannot pass the absence check
            awaitServerSpan();
            Assertions.assertThat(finishedSpans())
                    .noneMatch(
                            span ->
                                    span.getName().startsWith("security")
                                            || span.getName().startsWith("spring.security")
                                            || span.getName().startsWith("authorize"));
        }
    }

    @Nested
    class ManagementPortTest {
        @Test
        void shouldExportNoSpan_whenTheManagementPortIsScrapedBesideAMainPortControl() {
            // act: the management request goes first, so the exporter cannot receive its span after
            // the control span has already arrived
            var healthStatus =
                    given().baseUri("http://localhost")
                            .port(managementPort)
                            .basePath("")
                            .when()
                            .get("/actuator/health")
                            .then()
                            .extract()
                            .statusCode();
            getBoardsWithSession();

            // assert
            Assertions.assertThat(healthStatus).isEqualTo(HttpStatus.OK.value());
            awaitServerSpan();
            Assertions.assertThat(finishedSpans())
                    .noneMatch(span -> span.getAttributes().toString().contains("/actuator/health"))
                    .noneMatch(span -> span.getName().contains("actuator"));
        }
    }
}
