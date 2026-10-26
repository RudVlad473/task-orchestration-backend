package com.vrudenko.kanban_board.activitylog;

import java.time.Duration;

import com.vrudenko.kanban_board.support.containers.AbstractPostgresContainerTest;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaAdmin;

/**
 * Proves a send with the broker unreachable does not wait on the cluster id for longer than the
 * producer's own bounds.
 *
 * Why this is the way it is:
 *
 * - With observation on, every send makes this same KafkaAdmin call, and while the broker is down
 *   it is retried on every send. The wait is the admin client's default.api.timeout.ms of 60
 *   seconds, because closing the client waits for its pending request, against 2 seconds for the
 *   producer's max.block.ms, so an outage would drain the bounded publish pool about thirty times
 *   slower than before observation was enabled.
 * - The test profile leaves the broker address unreachable, which is the case under test, so no
 *   container is paused and nothing is mocked.
 * - The ceiling is a few seconds above the configured bound, not equal to it, because the lookup
 *   also pays a client start-up cost. The failure it guards against is a minute.
 * - The floor is there because Kafka refuses to build an admin client whose default.api.timeout.ms
 *   is below its request.timeout.ms. That failure returns instantly, so a ceiling alone passed
 *   against a client that could not start.
 */
@SpringBootTest
class KafkaClusterIdLookupBoundE2ETest extends AbstractPostgresContainerTest {

    private static final Duration CEILING = Duration.ofSeconds(8);
    private static final Duration FLOOR = Duration.ofSeconds(1);

    @Autowired private KafkaAdmin kafkaAdmin;

    @Test
    void shouldGiveUpOnTheClusterId_whenTheBrokerIsUnreachable() {
        // act
        var started = System.nanoTime();
        var clusterId = kafkaAdmin.clusterId();
        var elapsed = Duration.ofNanos(System.nanoTime() - started);

        // assert
        Assertions.assertThat(clusterId).isNull();
        Assertions.assertThat(elapsed).isLessThan(CEILING);
        Assertions.assertThat(elapsed)
                .as("the admin client was built and waited, rather than failing to start")
                .isGreaterThan(FLOOR);
    }
}
