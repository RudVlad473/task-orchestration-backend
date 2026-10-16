package com.vrudenko.kanban_board.activitylog;

import java.time.Duration;
import java.util.Locale;

import com.vrudenko.kanban_board.config.KafkaEventPublisher;
import com.vrudenko.kanban_board.constant.MetricNames;
import com.vrudenko.kanban_board.constant.ValidationConstants;
import com.vrudenko.kanban_board.dto.board_dto.BoardResponseDTO;
import com.vrudenko.kanban_board.dto.board_dto.SaveBoardRequestDTO;
import com.vrudenko.kanban_board.dto.user_dto.SignupRequestDTO;
import com.vrudenko.kanban_board.dto.user_dto.UserResponseDTO;
import com.vrudenko.kanban_board.event.BoardCreatedEvent;
import com.vrudenko.kanban_board.repository.ActivityLogRepository;
import com.vrudenko.kanban_board.service.UserService;
import com.vrudenko.kanban_board.support.containers.AbstractKafkaContainerTest;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.fluttercode.datafactory.impl.DataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Proves a publish whose send already returned a future, and whose future later completes
 * exceptionally, is counted once in kanban.kafka.publish.failures.
 *
 * Why this is the way it is:
 *
 * - SchemaRegistryOutageE2ETest covers the other path, a send that throws before any future
 *   exists. Neither class reaches this one: an unreachable registry fails every send
 *   synchronously at serialization, and the shared Kafka context has 30-second producer bounds, so
 *   a delivery failure would take half a minute to show.
 * - The failure is real and mocked nowhere. The first board is published normally, so the producer
 *   has its metadata and the serializer has its schema id cached. Then the broker container is
 *   paused: the next send is accepted into the producer's buffer and returns a future, the
 *   request gets no answer, and the producer expires the batch after delivery.timeout.ms.
 * - The class-level TestPropertySource lowers the producer's request and delivery timeouts so the
 *   expiry takes seconds. Subclass values override the superclass's, and the different property
 *   set gives this class a Spring context of its own.
 * - The broker container is shared by every Kafka class in the JVM, so it is unpaused in an
 *   AfterEach whatever the test did.
 */
@SpringBootTest
@Tag("kafka")
@TestPropertySource(
        properties = {
            "spring.kafka.producer.properties.request.timeout.ms=2000",
            "spring.kafka.producer.properties.delivery.timeout.ms=4000"
        })
class KafkaPublishFailureE2ETest extends AbstractKafkaContainerTest {

    @Autowired private UserService userService;
    @Autowired private ActivityLogRepository activityLogRepository;

    private final DataFactory dataFactory = new DataFactory();

    private Logger publisherLogger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void attachPublisherLogAppender() {
        publisherLogger = (Logger) LoggerFactory.getLogger(KafkaEventPublisher.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        publisherLogger.addAppender(logAppender);
    }

    @AfterEach
    void restoreBrokerAndDetachLogAppender() {
        unpauseBroker();
        publisherLogger.detachAppender(logAppender);
        logAppender.stop();
    }

    private String generateValidEmail() {
        return RandomStringUtils.randomAlphabetic(10).toLowerCase(Locale.ROOT) + "@example.com";
    }

    private BoardResponseDTO createBoard(UserResponseDTO owner) {
        return userService.addBoardByUserId(
                owner.getId(),
                SaveBoardRequestDTO.builder().name(RandomStringUtils.randomAlphabetic(16)).build());
    }

    @Test
    void shouldRaisePublishFailureCounterByExactlyOne_whenSendFutureCompletesExceptionally() {
        // arrange -- one publish that succeeds end to end, proven by its activity_log row, so the
        // producer's metadata and the serializer's schema id are cached before the broker freezes.
        UserResponseDTO owner =
                userService.save(
                        SignupRequestDTO.builder()
                                .email(generateValidEmail())
                                .displayName(
                                        dataFactory.getRandomWord(
                                                ValidationConstants.MIN_USER_DISPLAY_NAME_LENGTH))
                                .password(
                                        dataFactory.getRandomWord(
                                                ValidationConstants.MIN_PASSWORD_LENGTH))
                                .build());
        var warmUpBoard = createBoard(owner);
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .until(
                        () ->
                                activityLogRepository.findAll().stream()
                                        .anyMatch(
                                                row ->
                                                        row.getBoardId()
                                                                .equals(warmUpBoard.getId())));
        var eventTag = BoardCreatedEvent.class.getSimpleName();
        var publishFailures =
                counterReading(MetricNames.KAFKA_PUBLISH_FAILURES, MetricNames.TAG_EVENT, eventTag);
        var baseline = awaitSettled(publishFailures);

        // act -- the broker stops answering, then the mutation under test commits.
        pauseBroker();
        var failedBoard = createBoard(owner);

        // assert -- one failed delivery is one increment, tagged with the event's class name.
        assertRisesByExactlyOne(publishFailures, baseline);

        // assert -- the publisher's own ERROR line names the failed event, so the log still says
        // which record was lost.
        var loggedFailure =
                logAppender.list.stream()
                        .filter(event -> event.getLevel() == Level.ERROR)
                        .map(ILoggingEvent::getFormattedMessage)
                        .anyMatch(
                                message ->
                                        message.contains("Failed to publish " + eventTag)
                                                && message.contains(
                                                        "boardId=" + failedBoard.getId()));
        Assertions.assertThat(loggedFailure).isTrue();
    }
}
