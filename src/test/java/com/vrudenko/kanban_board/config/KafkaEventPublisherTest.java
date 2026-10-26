package com.vrudenko.kanban_board.config;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;

import com.vrudenko.kanban_board.constant.MetricNames;
import com.vrudenko.kanban_board.dto.board_dto.SaveBoardRequestDTO;
import com.vrudenko.kanban_board.event.BoardCreatedEvent;
import com.vrudenko.kanban_board.service.UserService;
import com.vrudenko.kanban_board.support.fixtures.AbstractAppTest;

import io.micrometer.core.instrument.MeterRegistry;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Proves a publish the full kafkaPublishExecutor rejects is counted, not dropped, and never
 * reaches the committing caller.
 *
 * Why this is the way it is:
 *
 * - The pool is filled for real, with tasks that block until released, until it rejects one. Its
 *   size (2 core, 4 max, 200 queued) comes from AsyncConfig, so the test submits until the
 *   rejection rather than assuming a number.
 * - The broker is unreachable in the test profile, so no container is needed and the publishes
 *   that do run fail fast; the baseline is read once the pool is idle, so none of them is still
 *   counting.
 * - The mutation is a real committed one, so the rejected event is the one the AFTER_COMMIT
 *   listener received, not a hand-built one.
 */
@SpringBootTest
class KafkaEventPublisherTest extends AbstractAppTest {

    @Autowired private UserService userService;
    @Autowired private MeterRegistry meterRegistry;

    @Autowired
    @Qualifier("kafkaPublishExecutor") private TaskExecutor kafkaPublishExecutor;

    @Nested
    class OnActivityEventTest {

        @Test
        void shouldCountOneFailureAndNotThrow_whenThePublishExecutorRejects() {
            // arrange
            var pool = (ThreadPoolTaskExecutor) kafkaPublishExecutor;
            awaitIdle(pool);
            var baseline = publishFailures();
            var release = new CountDownLatch(1);

            try {
                fillUntilRejecting(release);

                // act
                var thrown =
                        Assertions.catchException(
                                () ->
                                        userService.addBoardByUserId(
                                                getOwningUser().getId(),
                                                SaveBoardRequestDTO.builder()
                                                        .name("Rejected publish")
                                                        .build()));

                // assert
                Assertions.assertThat(thrown).isNull();
                Assertions.assertThat(publishFailures()).isEqualTo(baseline + 1);
            } finally {
                release.countDown();
            }

            // assert -- once the pool drains, nothing counted the same event a second time.
            awaitIdle(pool);
            Assertions.assertThat(publishFailures()).isEqualTo(baseline + 1);
        }
    }

    private void fillUntilRejecting(CountDownLatch release) {
        try {
            while (true) {
                kafkaPublishExecutor.execute(
                        () -> {
                            try {
                                release.await();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        });
            }
        } catch (TaskRejectedException expected) {
            // The pool is full: this is the state under test.
        }
    }

    private void awaitIdle(ThreadPoolTaskExecutor pool) {
        Awaitility.await()
                .during(Duration.ofMillis(500))
                .atMost(Duration.ofSeconds(60))
                .until(
                        () ->
                                pool.getActiveCount() == 0
                                        && pool.getThreadPoolExecutor().getQueue().isEmpty());
    }

    private double publishFailures() {
        return meterRegistry
                .get(MetricNames.KAFKA_PUBLISH_FAILURES)
                .tag(MetricNames.TAG_EVENT, BoardCreatedEvent.class.getSimpleName())
                .counter()
                .count();
    }
}
