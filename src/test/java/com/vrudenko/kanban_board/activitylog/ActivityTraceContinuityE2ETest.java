package com.vrudenko.kanban_board.activitylog;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

import com.vrudenko.kanban_board.constant.ApiPaths;
import com.vrudenko.kanban_board.constant.KafkaTopics;
import com.vrudenko.kanban_board.constant.ValidationConstants;
import com.vrudenko.kanban_board.dto.board_dto.SaveBoardRequestDTO;
import com.vrudenko.kanban_board.dto.column_dto.SaveColumnRequestDTO;
import com.vrudenko.kanban_board.dto.task_dto.SaveTaskRequestDTO;
import com.vrudenko.kanban_board.dto.user_dto.SigninRequestDTO;
import com.vrudenko.kanban_board.dto.user_dto.SignupRequestDTO;
import com.vrudenko.kanban_board.service.UserService;
import com.vrudenko.kanban_board.support.containers.AbstractKafkaContainerTest;
import com.vrudenko.kanban_board.support.observability.InMemorySpanExporterConfiguration;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.apache.avro.specific.SpecificRecord;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.fluttercode.datafactory.impl.DataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Proves one task creation is one trace from the HTTP request through the async publish and
 * Redpanda to the activity consumer.
 *
 * Why this is the way it is:
 *
 * - The assertions pick the spans of the request under test by the trace of its SERVER span. The
 *   span processor exports in batches and the activity consumer handles the setup requests'
 *   records too, so spans from earlier requests can still arrive after the exporter is reset.
 * - AutoConfigureObservability is required: Spring Boot disables tracing propagation in a
 *   SpringBootTest unless the class carries it, and the traceparent header on the record would
 *   stay empty.
 * - Each of the two code changes is pinned by one assertion. Without the TaskDecorator on
 *   kafkaPublishExecutor, the send span starts a new trace instead of joining the request's.
 *   Without setObservationEnabled on the primary KafkaTemplate, no send span exists at all and the
 *   receive span is an unlinked root.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureObservability
@Import(InMemorySpanExporterConfiguration.class)
@Tag("kafka")
class ActivityTraceContinuityE2ETest extends AbstractKafkaContainerTest {

    /**
     * Gives this test's own context a consumer in a group of its own, so the context always
     * receives a copy of every activity record.
     *
     * The real listener's group is shared by every cached Spring context in a full run, and the
     * broker hands each partition to only one of them, so the receive span would otherwise land
     * in a context whose exporter this test never reads.
     */
    @TestConfiguration
    static class TraceProbeListenerConfiguration {
        /**
         * A factory of its own with an error handler that only logs: the application's handler
         * dead-letters, so the probe group would write a second dead letter for every poison
         * payload another test publishes.
         */
        @Bean
        ConcurrentKafkaListenerContainerFactory<Object, Object> traceProbeContainerFactory(
                ConsumerFactory<Object, Object> consumerFactory) {
            var factory = new ConcurrentKafkaListenerContainerFactory<Object, Object>();
            factory.setConsumerFactory(consumerFactory);
            factory.getContainerProperties().setObservationEnabled(true);
            factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(0L, 0L)));
            return factory;
        }
    }

    /** Separate from the factory's configuration: one class holding both is a creation cycle. */
    @TestConfiguration
    static class TraceProbeListener {
        @KafkaListener(
                topics = KafkaTopics.ACTIVITY,
                groupId = "trace-continuity-probe",
                containerFactory = "traceProbeContainerFactory")
        void probe(SpecificRecord ignored) {}
    }

    private static final Duration SPAN_WAIT = Duration.ofSeconds(30);
    private static final String SEND_SPAN = KafkaTopics.ACTIVITY + " send";
    private static final String RECEIVE_SPAN = KafkaTopics.ACTIVITY + " receive";
    private static final String TASK_REQUEST_SPAN = "/boards/{boardId}/columns/{columnId}";

    @LocalServerPort private int port;

    @Value("${server.servlet.session.cookie.name}")
    private String cookieName;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired private UserService userService;
    @Autowired private InMemorySpanExporter spanExporter;

    private final DataFactory dataFactory = new DataFactory();

    @BeforeEach
    void resetSpans() {
        spanExporter.reset();
    }

    private RequestSpecification request() {
        return RestAssured.given()
                .baseUri("http://localhost")
                .port(port)
                .basePath(contextPath)
                .contentType(ContentType.JSON);
    }

    private List<SpanData> taskCreationTraceSpans() {
        var traceIds =
                spanExporter.getFinishedSpanItems().stream()
                        .filter(
                                span ->
                                        span.getKind() == SpanKind.SERVER
                                                && span.getName().endsWith(TASK_REQUEST_SPAN))
                        .map(SpanData::getTraceId)
                        .toList();

        return spanExporter.getFinishedSpanItems().stream()
                .filter(span -> traceIds.contains(span.getTraceId()))
                .toList();
    }

    private String describe() {
        return spanExporter.getFinishedSpanItems().stream()
                .map(
                        span ->
                                "%s %s trace=%s span=%s parent=%s"
                                        .formatted(
                                                span.getKind(),
                                                span.getName(),
                                                span.getTraceId(),
                                                span.getSpanId(),
                                                span.getParentSpanId()))
                .toList()
                .toString();
    }

    @Test
    void shouldKeepRequestSendAndReceiveInOneTrace_whenATaskIsCreatedOverHttp() {
        // arrange
        var email =
                RandomStringUtils.randomAlphabetic(10).toLowerCase(Locale.ROOT) + "@example.com";
        // The suffix satisfies every password class, because the signin route validates its body.
        var password =
                dataFactory
                                .getRandomWord(ValidationConstants.MIN_PASSWORD_LENGTH)
                                .toLowerCase(Locale.ROOT)
                        + "Aa1!";
        userService.save(
                SignupRequestDTO.builder()
                        .email(email)
                        .displayName(
                                dataFactory.getRandomWord(
                                        ValidationConstants.MIN_USER_DISPLAY_NAME_LENGTH))
                        .password(password)
                        .build());
        var sessionId =
                request()
                        .body(SigninRequestDTO.builder().email(email).password(password).build())
                        .post(ApiPaths.SIGNIN)
                        .then()
                        .extract()
                        .cookie(cookieName);
        String boardId =
                request()
                        .cookie(cookieName, sessionId)
                        .body(
                                SaveBoardRequestDTO.builder()
                                        .name(RandomStringUtils.randomAlphabetic(16))
                                        .build())
                        .post(ApiPaths.BOARDS)
                        .then()
                        .extract()
                        .path("id");
        String columnId =
                request()
                        .cookie(cookieName, sessionId)
                        .body(
                                SaveColumnRequestDTO.builder()
                                        .name(dataFactory.getRandomWord(5))
                                        .build())
                        .post(ApiPaths.BOARDS + "/" + boardId + ApiPaths.COLUMNS)
                        .then()
                        .extract()
                        .path("id");
        spanExporter.reset();

        // act
        var statusCode =
                request()
                        .cookie(cookieName, sessionId)
                        .body(
                                SaveTaskRequestDTO.builder()
                                        .title(
                                                dataFactory.getRandomWord(
                                                        ValidationConstants.MIN_TASK_TITLE_LENGTH
                                                                + 2))
                                        .build())
                        .post(ApiPaths.BOARDS + "/" + boardId + ApiPaths.COLUMNS + "/" + columnId)
                        .then()
                        .extract()
                        .statusCode();

        // assert
        Assertions.assertThat(statusCode).isEqualTo(201);
        Awaitility.await()
                .atMost(SPAN_WAIT)
                .untilAsserted(
                        () ->
                                Assertions.assertThat(taskCreationTraceSpans())
                                        .anyMatch(span -> span.getKind() == SpanKind.CONSUMER));
        var traceSpans = taskCreationTraceSpans();
        var producer =
                traceSpans.stream()
                        .filter(span -> span.getKind() == SpanKind.PRODUCER)
                        .findFirst()
                        .orElse(null);
        var consumer =
                traceSpans.stream()
                        .filter(span -> span.getKind() == SpanKind.CONSUMER)
                        .findFirst()
                        .orElse(null);
        Assertions.assertThat(spanExporter.getFinishedSpanItems())
                .as("a send span is exported at all")
                .anyMatch(span -> span.getKind() == SpanKind.PRODUCER);
        Assertions.assertThat(producer)
                .as("PRODUCER span in the request's trace; exported: " + describe())
                .isNotNull();
        Assertions.assertThat(producer.getName()).isEqualTo(SEND_SPAN);
        Assertions.assertThat(traceSpans)
                .as("the send span's parent belongs to the request's trace")
                .anyMatch(span -> span.getSpanId().equals(producer.getParentSpanId()));
        Assertions.assertThat(consumer).as("CONSUMER span in the request's trace").isNotNull();
        Assertions.assertThat(consumer.getName()).isEqualTo(RECEIVE_SPAN);
        Assertions.assertThat(consumer.getParentSpanId()).isEqualTo(producer.getSpanId());
    }
}
