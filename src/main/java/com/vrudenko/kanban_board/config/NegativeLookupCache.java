package com.vrudenko.kanban_board.config;

import java.time.Duration;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import org.springframework.lang.Nullable;

/**
 * Remember that a lookup came back empty, and skip it for a fixed window afterwards.
 *
 * KafkaConsumerConfig uses it around the cluster id lookup every observed send makes: with the
 * broker down that lookup waits out the admin client's 2 second bound on every send, on top of the
 * producer's own max.block.ms.
 *
 * Why this is the way it is:
 *
 * - Only a null result is remembered. A real value is returned as is and never held here, because
 *   the lookup's owner already caches those.
 * - The lookup is synchronized so that sends arriving together while the broker is down pay for
 *   one failed lookup, not one each: the others wait for it and then see the window.
 * - The clock is injected so a test can move time without sleeping.
 */
final class NegativeLookupCache {
    private final LongSupplier nanoTime;
    private final long windowNanos;

    // Not volatile: every read and write happens inside the synchronized lookup.
    private Long suppressedUntilNanos;

    NegativeLookupCache(LongSupplier nanoTime, Duration window) {
        this.nanoTime = nanoTime;
        this.windowNanos = window.toNanos();
    }

    @Nullable synchronized String lookup(Supplier<String> delegate) {
        var now = nanoTime.getAsLong();
        if (suppressedUntilNanos != null && now - suppressedUntilNanos < 0) {
            return null;
        }

        var result = delegate.get();
        suppressedUntilNanos = result == null ? now + windowNanos : null;
        return result;
    }
}
