package com.vrudenko.kanban_board.e2e.observability;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import com.vrudenko.kanban_board.constant.MetricNames;
import com.vrudenko.kanban_board.event.ActivityEvent;
import com.vrudenko.kanban_board.support.containers.AbstractPostgresContainerTest;

import io.micrometer.core.instrument.MeterRegistry;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Proves every request-path counter exists at zero as soon as the application has started.
 *
 * A counter registered on its first increment is invisible to Prometheus' increase() for that
 * first event, because the series has no earlier sample to subtract. A shared test context cannot
 * show this: another test class may already have incremented the counter. The property below gives
 * this class a Spring context, and so a MeterRegistry, of its own, and extending
 * AbstractPostgresContainerTest instead of AbstractAppTest keeps the fixtures from creating tasks
 * before the first assertion.
 */
@SpringBootTest(properties = "kanban.test.metric-registry=eager-registration")
public class EagerMetricRegistrationTest extends AbstractPostgresContainerTest {

    @Autowired private MeterRegistry meterRegistry;

    private Double count(String name, String... tags) {
        var counter = meterRegistry.find(name).tags(tags).counter();

        return counter == null ? null : counter.count();
    }

    @Test
    void shouldHoldTasksCreatedAtZero_whenApplicationHasJustStarted() {
        // arrange & act
        var value = count(MetricNames.TASKS_CREATED);

        // assert
        Assertions.assertThat(value).isEqualTo(0.0);
    }

    @Test
    void shouldHoldEveryTasksMovedTagValueAtZero_whenApplicationHasJustStarted() {
        // arrange & act
        var sameColumn =
                count(MetricNames.TASKS_MOVED, MetricNames.TAG_MOVE, MetricNames.MOVE_SAME_COLUMN);
        var crossColumn =
                count(MetricNames.TASKS_MOVED, MetricNames.TAG_MOVE, MetricNames.MOVE_CROSS_COLUMN);

        // assert
        Assertions.assertThat(sameColumn).isEqualTo(0.0);
        Assertions.assertThat(crossColumn).isEqualTo(0.0);
    }

    @Test
    void shouldHoldOptimisticLockConflictsAtZero_whenApplicationHasJustStarted() {
        // arrange & act
        var value = count(MetricNames.OPTIMISTIC_LOCK_CONFLICTS);

        // assert
        Assertions.assertThat(value).isEqualTo(0.0);
    }

    @Test
    void shouldHoldOwnershipDeniedAtZero_whenApplicationHasJustStarted() {
        // arrange & act
        var value = count(MetricNames.OWNERSHIP_DENIED);

        // assert
        Assertions.assertThat(value).isEqualTo(0.0);
    }

    @Test
    void shouldHoldDeadLettersWrittenAtZero_whenApplicationHasJustStarted() {
        // arrange & act
        var value = count(MetricNames.ACTIVITY_DLT_WRITTEN);

        // assert
        Assertions.assertThat(value).isEqualTo(0.0);
    }

    @Test
    void shouldHoldPublishFailuresAtZeroForEveryEventType_whenApplicationHasJustStarted() {
        // arrange
        var eventTypes = ActivityEvent.class.getPermittedSubclasses();

        // act
        var valuesByEvent =
                Arrays.stream(eventTypes)
                        .collect(
                                Collectors.toMap(
                                        Class::getSimpleName,
                                        type ->
                                                Optional.ofNullable(
                                                        count(
                                                                MetricNames.KAFKA_PUBLISH_FAILURES,
                                                                MetricNames.TAG_EVENT,
                                                                type.getSimpleName()))));

        // assert
        // The size guard keeps an empty permits list from passing vacuously.
        Assertions.assertThat(valuesByEvent).hasSizeGreaterThan(10).containsKey("TaskMovedEvent");
        Assertions.assertThat(valuesByEvent.values())
                .allSatisfy(value -> Assertions.assertThat(value).contains(0.0));
    }

    @Test
    void shouldHoldEveryAuthFailureReasonAtZero_whenApplicationHasJustStarted() {
        // arrange & act
        var badCredentials =
                count(
                        MetricNames.AUTH_FAILURES,
                        MetricNames.TAG_REASON,
                        MetricNames.REASON_BAD_CREDENTIALS);
        var unauthenticated =
                count(
                        MetricNames.AUTH_FAILURES,
                        MetricNames.TAG_REASON,
                        MetricNames.REASON_UNAUTHENTICATED);

        // assert
        Assertions.assertThat(badCredentials).isEqualTo(0.0);
        Assertions.assertThat(unauthenticated).isEqualTo(0.0);
    }
}
