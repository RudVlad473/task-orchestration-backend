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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
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
 * Dispatch is a submit to the kafkaPublishExecutor pool (AsyncConfig), made by the AFTER_COMMIT
 * listener itself:
 * KafkaTemplate.send() blocks its caller inside KafkaProducer.doSend ->
 * waitOnMetadata for up to max.block.ms before returning its future, so the AFTER_COMMIT
 * thread (the request thread in production, the fixture-setup thread in tests) stalled for that
 * bound on every mutation. Without the pool this was a real 20 to 25 minute full-suite hang.
 *
 * The listener is not @Async because a full pool then rejected the call inside Spring's proxy,
 * before this class ran, so the event was dropped with no count and no log naming it. Submitting
 * here keeps the event in hand when that happens. A rejected submit is never run on the caller's
 * thread: that would put the broker wait back on the request.
 *
 * A failed publish reaches this class by one of three paths, and each is counted once in
 * kanban.kafka.publish.failures (tagged with the event's class name) through
 * countPublishFailure:
 *
 * - A failure after the record reached the producer, such as a delivery timeout, completes the
 *   future KafkaTemplate.send returned. The whenComplete callback logs it and counts it.
 * - A failure before any future exists makes send throw synchronously: Avro serialization or a
 *   schema registry lookup runs inside KafkaProducer.doSend, so an unreachable registry throws a
 *   SerializationException instead of returning a failed future (SchemaRegistryOutageE2ETest
 *   proves it). The catch around the mapping and the send counts it and logs it, naming the
 *   event.
 * - A full pool rejects the submit with TaskRejectedException, so the send never starts. The
 *   catch around the submit counts it and logs it, naming the event.
 *
 * The paths are exclusive for one publish: a rejected submit never reaches send, and a
 * synchronous throw returns no future, so no callback is attached and the failure is counted
 * exactly once.
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

    @Autowired
    @Qualifier("kafkaPublishExecutor") private TaskExecutor kafkaPublishExecutor;

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

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onActivityEvent(ActivityEvent event) {
        try {
            kafkaPublishExecutor.execute(() -> send(event));
        } catch (TaskRejectedException e) {
            countPublishFailure(event);
            log.error(
                    "Dropped {} (eventId={}, boardId={}): the publish pool rejected it",
                    event.getClass().getSimpleName(),
                    event.eventId(),
                    event.boardId(),
                    e);
        }
    }

    private void send(ActivityEvent event) {
        CompletableFuture<SendResult<String, Object>> sent;
        try {
            sent =
                    kafkaTemplate.send(
                            KafkaTopics.ACTIVITY,
                            event.eventId().toString(),
                            activityEventAvroMapper.toAvro(event));
        } catch (RuntimeException e) {
            countPublishFailure(event);
            log.error(
                    "Failed to publish {} (eventId={}, boardId={}) to {}",
                    event.getClass().getSimpleName(),
                    event.eventId(),
                    event.boardId(),
                    KafkaTopics.ACTIVITY,
                    e);
            return;
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
