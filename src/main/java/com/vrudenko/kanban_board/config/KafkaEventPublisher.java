package com.vrudenko.kanban_board.config;

import java.util.concurrent.CompletableFuture;

import com.vrudenko.kanban_board.constant.KafkaTopics;
import com.vrudenko.kanban_board.constant.MetricNames;
import com.vrudenko.kanban_board.event.ActivityEvent;
import com.vrudenko.kanban_board.event.avro.ActivityEventAvroMapper;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Send each ActivityEvent published inside a transaction to KafkaTopics.ACTIVITY
 * strictly after commit.
 *
 * A committed mutation's HTTP outcome therefore never depends on Kafka reachability. This is the
 * only place in src/main that touches the Kafka client API. A failed send is logged, never
 * swallowed; the mutation has already succeeded and returned by then.
 *
 * Decisions:
 *
 * Dispatch is @Async onto the kafkaPublishExecutor pool (AsyncConfig):
 * KafkaTemplate.send() blocks its caller inside KafkaProducer.doSend ->
 * waitOnMetadata for up to max.block.ms before returning its future, so the AFTER_COMMIT
 * thread (the request thread in production, the fixture-setup thread in tests) stalled for that
 * bound on every mutation. Without @Async this was a real 20 to 25 minute full-suite hang.
 *
 * A failed publish reaches this class by one of two paths, and each is counted once in
 * kanban.kafka.publish.failures (tagged with the event's class name) through
 * countPublishFailure:
 *
 * - A failure after the record reached the producer, such as a delivery timeout, completes the
 *   future KafkaTemplate.send returned. The whenComplete callback logs it and counts it.
 * - A failure before any future exists makes send throw synchronously: Avro serialization or a
 *   schema registry lookup runs inside KafkaProducer.doSend, so an unreachable registry throws a
 *   SerializationException instead of returning a failed future (SchemaRegistryOutageE2ETest
 *   proves it). The catch around the mapping and the send counts it and rethrows, so Spring's
 *   uncaught-exception handler for Async methods still logs the failure as it did before the
 *   counter existed.
 *
 * The paths are exclusive for one publish: a synchronous throw returns no future, so no callback
 * is attached and the failure is counted exactly once.
 *
 * Every event type's counter is registered at startup, so a pod's first failed publish is
 * already an increase from 0 that Prometheus' increase() can see.
 */
@Component
public class KafkaEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    @Autowired private KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired private ActivityEventAvroMapper activityEventAvroMapper;
    @Autowired private MeterRegistry meterRegistry;

    // Register one series per event type at startup, not on its first failure.
    //
    // Prometheus' increase() needs an earlier sample to subtract, so a counter first created by its
    // first failure would show that failure as no change at all. The tag values are the permitted
    // ActivityEvent record names, a fixed set that never comes from request or record data.
    @PostConstruct
    void registerPublishFailureCounters() {
        for (var eventType : ActivityEvent.class.getPermittedSubclasses()) {
            publishFailureCounter(eventType.getSimpleName());
        }
    }

    private Counter publishFailureCounter(String eventName) {
        return Counter.builder(MetricNames.KAFKA_PUBLISH_FAILURES)
                .tag(MetricNames.TAG_EVENT, eventName)
                .description("Activity events whose publish to Kafka failed")
                .register(meterRegistry);
    }

    private void countPublishFailure(ActivityEvent event) {
        publishFailureCounter(event.getClass().getSimpleName()).increment();
    }

    @Async("kafkaPublishExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onActivityEvent(ActivityEvent event) {
        CompletableFuture<SendResult<String, Object>> sent;
        try {
            sent =
                    kafkaTemplate.send(
                            KafkaTopics.ACTIVITY,
                            event.eventId().toString(),
                            activityEventAvroMapper.toAvro(event));
        } catch (RuntimeException e) {
            countPublishFailure(event);
            throw e;
        }

        // The chained future from whenComplete() is deliberately unused; assigning it to `unused`
        // tells ErrorProne's FutureReturnValueIgnored check that dropping it is intended.
        var unused =
                sent.whenComplete(
                        (result, ex) -> {
                            if (ex != null) {
                                countPublishFailure(event);
                                log.error(
                                        "Failed to publish {} (eventId={}, boardId={}) to {}",
                                        event.getClass().getSimpleName(),
                                        event.eventId(),
                                        event.boardId(),
                                        KafkaTopics.ACTIVITY,
                                        ex);
                            }
                        });
    }
}
