package com.vrudenko.kanban_board.support.containers;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.DoubleSupplier;

import com.vrudenko.kanban_board.config.AvroSchemaRegistrar;
import com.vrudenko.kanban_board.constant.KafkaTopics;
import com.vrudenko.kanban_board.event.ActivityEvent;
import com.vrudenko.kanban_board.event.avro.ActivityEventAvroMapper;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.redpanda.RedpandaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared broker-and-registry harness for the activitylog integration tests: one
 * docker.redpanda.com/redpandadata/redpanda:v26.2.1 container.
 *
 * The container exposes a Kafka broker and a Confluent-compatible Schema Registry.
 *
 * Why this is the way it is:
 *
 * - Redpanda replaced apache/kafka-native:4.3.1 in place, rather than adding a second
 *   Avro-specific harness: the pre-existing activitylog E2E classes never touch a
 *   registry, so they pass unchanged because Redpanda is a Kafka-protocol superset of what they
 *   exercised.
 * - Producer bounds (max.block.ms, request.timeout.ms,
 *   delivery.timeout.ms) are raised to 30 seconds for this context only. The test profile
 *   bounds them at 50ms so a missing broker cannot slow the suite, but against a real broker
 *   50ms aborts the first send before the container finishes announcing itself. 30 seconds, not
 *   the original 10, matches the Awaitility ceiling: with several Testcontainers-backed
 *   classes sharing one broker and one activity-log consumer group, cumulative load in
 *   a full-suite run occasionally expired a 10-second bound (TimeoutException: Expiring
 *   1 record(s)...) though every class passes in isolation. It is headroom for real load, not
 *   a retry loop or a softened assertion, and does not affect what any test asserts.
 * - It does not extend AbstractAppTest: the consumer path needs no user, board, column
 *   or task fixture, and AbstractAppTest's setup creates roughly twenty entities
 *   through the real services, each publishing an event into the broker under test and turning
 *   every test into a race against unrelated traffic. It shares only
 *   AbstractPostgresContainerTest, the common ancestor, so one PostgreSQL container backs the
 *   whole suite.
 * - The container starts imperatively in a static initializer (kafka.start()), not via
 *   the @Testcontainers/@Container extension. Observed on Windows with Docker
 *   Desktop and testcontainers-java 1.21.0: the extension's singleton-container pattern did not
 *   hold across this package's classes. A second container (different ID and mapped port)
 *   started for a later class while Spring's cached ApplicationContext kept its
 *   KafkaTemplate/@KafkaListener beans bound to the first container's stale port, and
 *   raw clients built via getBootstrapServers() (evaluated on every call) reached the
 *   current container. Spring-mediated sends hung until timeout while raw clients worked. A
 *   static initializer runs exactly once per classloader, independent of JUnit extension
 *   bookkeeping.
 * - @ServiceConnection on kafka wires spring.kafka.bootstrap-servers
 *   (Spring Boot 3.5.16 ships a RedpandaContainerConnectionDetailsFactory). No
 *   connection-details mechanism exists for the Schema Registry, so schema.registry.url
 *   is wired through @DynamicPropertySource, not the test property source block: an
 *   annotation attribute must be a compile-time constant and the registry's mapped port is
 *   known only after the container starts.
 */
@SpringBootTest
@TestPropertySource(
        properties = {
            "spring.kafka.producer.properties.max.block.ms=30000",
            "spring.kafka.producer.properties.request.timeout.ms=30000",
            "spring.kafka.producer.properties.delivery.timeout.ms=30000",
            // Raises the registry REST client's retry/timeout bounds back up, as the producer
            // bounds above are.
            //
            // application-test.properties fail-fasts them (max.retries=0, 50ms timeouts), correct
            // for the 17 fixture-heavy classes whose registry has nothing listening; here the
            // registry is real, so those bounds would turn a slow lookup into a flaky failure.
            "spring.kafka.producer.properties.max.retries=3",
            "spring.kafka.producer.properties.retries.wait.ms=1000",
            "spring.kafka.producer.properties.http.connect.timeout.ms=30000",
            "spring.kafka.producer.properties.http.read.timeout.ms=30000",
            // Restores the admin client's API timeout, which application.properties bounds at 2s.
            //
            // A real broker under suite load needs the headroom, as the producer bounds above do.
            "spring.kafka.admin.properties.default.api.timeout.ms=30000",
            "spring.kafka.admin.properties.request.timeout.ms=30000"
        })
public abstract class AbstractKafkaContainerTest extends AbstractPostgresContainerTest {
    // The docker-java api.version=1.44 pin (testcontainers-java#11212) lives in
    // AbstractPostgresContainerTest's static initializer; JVM class initialization runs superclass
    // statics first, so it is in place before either container type starts.

    @ServiceConnection
    static final RedpandaContainer kafka =
            new RedpandaContainer(
                    DockerImageName.parse("docker.redpanda.com/redpandadata/redpanda:v26.2.1"));

    // Imperative, exactly-once start (see the class Javadoc). Schemas register here too, through
    // the same AvroSchemaRegistrar.registerAll() the registerSchemas Gradle task invokes, so
    // auto.register.schemas=false needs no manual step (docs/CODE_STYLE.md rule 8).
    static {
        kafka.start();
        AvroSchemaRegistrar.registerAll(kafka.getSchemaRegistryAddress());
    }

    /**
     * Test-scoped hook that lets SchemaRegistryOutageE2ETest make the producer, and only
     * the producer, see an unreachable registry.
     *
     * Why this is the way it is: a subclass-local @DynamicPropertySource overriding the
     * same key does not work. Spring invokes all such methods into one shared property source and
     * (confirmed empirically) invokes subclass-local methods before superclass ones, the opposite
     * of @BeforeAll, so the superclass method runs last and overwrites the subclass value.
     * This mutable field is the override point instead: it defaults to null (the real
     * container address), and the one test that needs it sets it in a static initializer
     * and resets it in an @AfterAll. Test classes in this package run sequentially in one
     * JVM (no parallel execution is configured), so no two contexts are built concurrently against
     * a transiently wrong value.
     */
    protected static volatile String producerSchemaRegistryUrlOverride;

    @DynamicPropertySource
    static void registerSchemaRegistryProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.kafka.producer.properties.schema.registry.url",
                () ->
                        producerSchemaRegistryUrlOverride != null
                                ? producerSchemaRegistryUrlOverride
                                : kafka.getSchemaRegistryAddress());
        registry.add(
                "spring.kafka.consumer.properties.schema.registry.url",
                kafka::getSchemaRegistryAddress);
    }

    @Autowired protected KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired protected ActivityEventAvroMapper activityEventAvroMapper;
    @Autowired protected MeterRegistry meterRegistry;

    protected String getBootstrapServers() {
        return kafka.getBootstrapServers();
    }

    protected String getSchemaRegistryAddress() {
        return kafka.getSchemaRegistryAddress();
    }

    /**
     * Freezes the shared broker container, schema registry included, until unpauseBroker().
     *
     * A paused container keeps its sockets open and answers nothing, which is how a producer
     * with a cached schema and metadata is made to fail after the send has returned a future: the
     * request times out in flight rather than failing at connect. Always pair with unpauseBroker()
     * in an @AfterEach, because the container is shared by every later class in the JVM.
     */
    protected void pauseBroker() {
        kafka.getDockerClient().pauseContainerCmd(kafka.getContainerId()).exec();
    }

    /** Thaws the broker container if it is frozen; safe to call when it is not. */
    protected void unpauseBroker() {
        var state = kafka.getDockerClient().inspectContainerCmd(kafka.getContainerId()).exec();
        if (Boolean.TRUE.equals(state.getState().getPaused())) {
            kafka.getDockerClient().unpauseContainerCmd(kafka.getContainerId()).exec();
        }
    }

    /** Reads a counter in this test's context; 0 when it is not registered there. */
    protected DoubleSupplier counterReading(String name, String... tags) {
        return () -> {
            var counter = meterRegistry.find(name).tags(tags).counter();

            return counter == null ? 0.0 : counter.count();
        };
    }

    /**
     * Reads a counter summed over every Spring context alive in this JVM; 0 where it is not
     * registered.
     *
     * Every cached Kafka context runs the same activity-log consumer group, and the topic has one
     * partition, so which context consumes a record depends on the order the classes ran in. A
     * counter incremented by the consumer side can therefore land in a registry other than the one
     * the test is autowired with. Each context's registry joins Metrics.globalRegistry
     * (management.metrics.use-global-registry defaults to true), so summing over it counts the
     * increment wherever it happened.
     */
    protected DoubleSupplier counterReadingAcrossContexts(String name, String... tags) {
        return () ->
                Metrics.globalRegistry.getRegistries().stream()
                        .map(registry -> registry.find(name).tags(tags).counter())
                        .filter(Objects::nonNull)
                        .mapToDouble(Counter::count)
                        .sum();
    }

    /**
     * Waits for the reading to stop moving for two seconds, then returns it.
     *
     * A baseline read while an earlier test's publish or dead-lettering is still in flight would
     * let that late increment pass as the one under test.
     */
    protected double awaitSettled(DoubleSupplier reading) {
        var last = new AtomicReference<>(reading.getAsDouble());
        Awaitility.await()
                .during(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(60))
                .until(
                        () -> {
                            var now = reading.getAsDouble();
                            var unchanged = now == last.get();
                            last.set(now);
                            return unchanged;
                        });

        return last.get();
    }

    /**
     * Asserts the reading reaches baseline + 1 and then stays there for three seconds.
     *
     * Reaching the value proves the hook ran; holding it proves the hook did not run twice for
     * one failure, which a plain "at least" check would miss.
     */
    protected void assertRisesByExactlyOne(DoubleSupplier reading, double baseline) {
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .until(() -> reading.getAsDouble() >= baseline + 1);
        Awaitility.await()
                .during(Duration.ofSeconds(3))
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () -> Assertions.assertThat(reading.getAsDouble()).isEqualTo(baseline + 1));
    }

    /**
     * Publishes event to KafkaTopics.ACTIVITY, keyed by its eventId, and
     * blocks until the broker acknowledges, timing out at 30 seconds.
     *
     * Awaiting the ack makes a broker-side send rejection surface as this method's own
     * exception, not as a misleading 30-second Awaitility timeout that blames the consumer. The
     * event is mapped through ActivityEventAvroMapper first, because the wire format is
     * Avro, so callers keep handing it domain events.
     */
    protected void sendAndAwaitAck(ActivityEvent event)
            throws InterruptedException, ExecutionException, TimeoutException {
        kafkaTemplate
                .send(
                        KafkaTopics.ACTIVITY,
                        event.eventId().toString(),
                        activityEventAvroMapper.toAvro(event))
                .get(30, TimeUnit.SECONDS);
    }
}
