package com.vrudenko.kanban_board.e2e.observability;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import com.vrudenko.kanban_board.event.ActivityEvent;
import com.vrudenko.kanban_board.support.fixtures.AbstractAppE2ETest;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;

/**
 * Pins the exact Prometheus series names the alert rules read, from a real scrape of the
 * management port.
 *
 * Every string below is spelled as PromQL spells it: dots became underscores, a counter gained
 * _total, a timer gained _seconds. A registry-level test cannot see that rendering, so a renamed
 * metric would pass every other test and silently blank the alert built on it.
 *
 * Why this is the way it is:
 * The property below gives this class a Spring context, and so a Prometheus registry, of its own.
 * Another class sharing a context may already have incremented a counter, and a counter that shows
 * 0.0 here is the proof that it exists from startup rather than from its first event.
 *
 * kanban_kafka_publish_failures_total is the one counter the fixtures move: this profile has no
 * broker, so the fixtures' own publishes fail. Its series are asserted present for every event
 * type, and at 0.0 only for an event type no fixture publishes.
 *
 * It carries no @Tag("realSocket"): a scrape and one request are neither concurrent nor slow, so
 * it runs in the pre-commit fastTest gate (docs/CODE_STYLE.md rule 4).
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "management.prometheus.metrics.export.enabled=true",
            "kanban.test.metric-registry=prometheus-scrape"
        })
public class PrometheusScrapeE2ETest extends AbstractAppE2ETest {

    private static final String PROMETHEUS_PATH = "/actuator/prometheus";

    // The fixtures never delete a subtask through a service, so this event type's publish-failure
    // series must still read zero when the scrape happens.
    private static final String UNPUBLISHED_EVENT = "SubtaskDeletedEvent";

    @LocalManagementPort private int managementPort;

    private String scrape() {
        var response =
                given().port(managementPort)
                        .basePath("")
                        .when()
                        .get(PROMETHEUS_PATH)
                        .then()
                        .extract();
        Assertions.assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());

        return response.body().asString();
    }

    private Pattern sample(String series, String valueRegex) {
        return Pattern.compile("(?m)^" + Pattern.quote(series) + " " + valueRegex + "$");
    }

    @Nested
    class SeriesFromStartupTest {

        @Test
        void shouldExposeDeadLettersWrittenAtZero_whenScrapedBeforeAnyEvent() {
            // arrange & act
            var body = scrape();

            // assert
            Assertions.assertThat(body)
                    .containsPattern(sample("kanban_activity_dlt_written_total", "0\\.0"));
        }

        @Test
        void shouldExposeOptimisticLockConflictsAtZero_whenScrapedBeforeAnyEvent() {
            // arrange & act
            var body = scrape();

            // assert
            Assertions.assertThat(body)
                    .containsPattern(sample("kanban_optimistic_lock_conflicts_total", "0\\.0"));
        }

        @Test
        void shouldExposeEveryAuthFailureReasonAtZero_whenScrapedBeforeAnyEvent() {
            // arrange & act
            var body = scrape();

            // assert
            Assertions.assertThat(body)
                    .containsPattern(
                            sample(
                                    "kanban_auth_failures_total{reason=\"bad_credentials\"}",
                                    "0\\.0"))
                    .containsPattern(
                            sample(
                                    "kanban_auth_failures_total{reason=\"unauthenticated\"}",
                                    "0\\.0"));
        }

        @Test
        void shouldExposePublishFailureSeriesForEveryEventType_whenScrapedBeforeAnyFailure() {
            // arrange
            var eventNames =
                    Arrays.stream(ActivityEvent.class.getPermittedSubclasses())
                            .map(Class::getSimpleName)
                            .toList();

            // act
            var body = scrape();

            // assert -- the size guard keeps an empty permits list from passing vacuously.
            Assertions.assertThat(eventNames).hasSizeGreaterThan(10);
            eventNames.forEach(
                    event ->
                            Assertions.assertThat(body)
                                    .containsPattern(
                                            sample(
                                                    "kanban_kafka_publish_failures_total{event=\""
                                                            + event
                                                            + "\"}",
                                                    "[0-9.E+]+")));
            Assertions.assertThat(body)
                    .containsPattern(
                            sample(
                                    "kanban_kafka_publish_failures_total{event=\""
                                            + UNPUBLISHED_EVENT
                                            + "\"}",
                                    "0\\.0"));
        }
    }

    @Nested
    class LatencyBucketsTest {

        @Test
        void shouldExposeEveryFixedBucket_whenOneRequestWasServed() {
            // arrange -- one request on the main port, so the per-endpoint timer has a sample.
            signin();
            var upperBounds = List.of("0.05", "0.1", "0.25", "0.5", "1.0", "2.0", "5.0");

            // act
            var body = scrape();

            // assert -- the 250ms and 5s bounds are the ones the p95 alert and SLO rules read; the
            // others are the rest of the fixed set.
            upperBounds.forEach(
                    bound ->
                            Assertions.assertThat(body)
                                    .containsPattern(
                                            Pattern.compile(
                                                    "(?m)^http_server_requests_seconds_bucket\\{[^}]*le=\""
                                                            + Pattern.quote(bound)
                                                            + "\"[^}]*\\} [0-9.E+]+$")));
        }
    }
}
