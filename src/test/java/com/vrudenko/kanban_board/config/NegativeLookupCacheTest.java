package com.vrudenko.kanban_board.config;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Proves NegativeLookupCache skips a lookup after an empty result and only until its window ends.
 *
 * No Spring context and no Kafka: the class is pure logic over an injected clock, so the clock is
 * an AtomicLong the test moves by hand and the delegate is a counter.
 */
class NegativeLookupCacheTest {

    private static final Duration WINDOW = Duration.ofSeconds(30);

    private final AtomicLong nowNanos = new AtomicLong(1_000L);
    private final AtomicInteger delegateCalls = new AtomicInteger();
    private final NegativeLookupCache cache = new NegativeLookupCache(nowNanos::get, WINDOW);

    @Nested
    class LookupTest {

        @Test
        void shouldNotCallTheDelegateAgain_whenTheLastResultWasNullInsideTheWindow() {
            // arrange
            cache.lookup(() -> failedLookup());
            nowNanos.addAndGet(WINDOW.toNanos() - 1);

            // act
            var result = cache.lookup(() -> failedLookup());

            // assert
            Assertions.assertThat(result).isNull();
            Assertions.assertThat(delegateCalls).hasValue(1);
        }

        @Test
        void shouldCallTheDelegateAgain_whenTheWindowHasPassed() {
            // arrange
            cache.lookup(() -> failedLookup());
            nowNanos.addAndGet(WINDOW.toNanos());

            // act
            var result = cache.lookup(() -> failedLookup());

            // assert
            Assertions.assertThat(result).isNull();
            Assertions.assertThat(delegateCalls).hasValue(2);
        }

        @Test
        void shouldReturnTheValueAndKeepCallingTheDelegate_whenTheResultIsNotNull() {
            // act
            var first = cache.lookup(() -> successfulLookup("cluster-1"));
            var second = cache.lookup(() -> successfulLookup("cluster-1"));

            // assert
            Assertions.assertThat(first).isEqualTo("cluster-1");
            Assertions.assertThat(second).isEqualTo("cluster-1");
            Assertions.assertThat(delegateCalls).hasValue(2);
        }

        @Test
        void shouldReturnTheValue_whenALookupSucceedsAfterTheWindowOfAFailure() {
            // arrange
            cache.lookup(() -> failedLookup());
            nowNanos.addAndGet(WINDOW.toNanos());

            // act
            var result = cache.lookup(() -> successfulLookup("cluster-1"));
            var next = cache.lookup(() -> successfulLookup("cluster-1"));

            // assert
            Assertions.assertThat(result).isEqualTo("cluster-1");
            Assertions.assertThat(next).isEqualTo("cluster-1");
            Assertions.assertThat(delegateCalls).hasValue(3);
        }
    }

    private String failedLookup() {
        delegateCalls.incrementAndGet();
        return null;
    }

    private String successfulLookup(String clusterId) {
        delegateCalls.incrementAndGet();
        return clusterId;
    }
}
