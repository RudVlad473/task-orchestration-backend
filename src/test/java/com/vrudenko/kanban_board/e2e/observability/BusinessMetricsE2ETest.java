package com.vrudenko.kanban_board.e2e.observability;

import java.time.Duration;

import com.vrudenko.kanban_board.constant.ApiPaths;
import com.vrudenko.kanban_board.constant.MetricNames;
import com.vrudenko.kanban_board.dto.task_dto.MoveTaskRequestDTO;
import com.vrudenko.kanban_board.dto.task_dto.SaveTaskRequestDTO;
import com.vrudenko.kanban_board.dto.task_dto.UpdateTaskRequestDTO;
import com.vrudenko.kanban_board.dto.user_dto.SigninRequestDTO;
import com.vrudenko.kanban_board.repository.TaskRepository;
import com.vrudenko.kanban_board.service.TaskService;
import com.vrudenko.kanban_board.support.fixtures.AbstractAppE2ETest;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.restassured.http.ContentType;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static io.restassured.RestAssured.given;

/**
 * Proves each request-path decision moves its counter by exactly one per triggering request.
 *
 * Every case drives the real HTTP path and reads the counter from the application's own
 * MeterRegistry as a delta (value after minus value before), because the Spring context and its
 * registry are shared by every test class in the JVM fork. A counter that does not exist yet reads
 * as zero, so a missing increment fails the delta assertion rather than a null check.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BusinessMetricsE2ETest extends AbstractAppE2ETest {

    @Autowired private MeterRegistry meterRegistry;

    @Autowired private TaskRepository taskRepository;

    @Autowired private TaskService taskService;

    @Autowired private PlatformTransactionManager transactionManager;

    private double count(String name, String... tags) {
        var counter = meterRegistry.find(name).tags(tags).counter();

        return counter == null ? 0.0 : counter.count();
    }

    private double movedCount(String move) {
        return count(MetricNames.TASKS_MOVED, MetricNames.TAG_MOVE, move);
    }

    // Sums every timer of the name: Boot adds an error tag, so one observation can span series.
    private long timerCount(String name) {
        return meterRegistry.find(name).timers().stream().mapToLong(Timer::count).sum();
    }

    private long httpCount(String method, String uri) {
        return meterRegistry
                .find("http.server.requests")
                .tag("method", method)
                .tag("uri", uri)
                .timers()
                .stream()
                .mapToLong(Timer::count)
                .sum();
    }

    // http.server.requests is recorded after the response is written, so the client can see the
    // response a moment before the timer moves.
    private void awaitHttpCountAbove(String method, String uri, long before) {
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(
                        () ->
                                Assertions.assertThat(httpCount(method, uri))
                                        .as("http.server.requests count for %s %s", method, uri)
                                        .isGreaterThan(before));
    }

    private String columnPath(String columnId) {
        return ApiPaths.BOARDS
                + "/"
                + mockPopulatedBoard.getId()
                + ApiPaths.COLUMNS
                + "/"
                + columnId;
    }

    private int moveStatus(Pair<String, String> cookie, MoveTaskRequestDTO dto) {
        return given().cookie(cookie.getFirst(), cookie.getSecond())
                .contentType(ContentType.JSON)
                .body(dto)
                .when()
                .patch(ApiPaths.TASKS + "/" + mockPopulatedTask.getId() + ApiPaths.MOVE)
                .then()
                .extract()
                .statusCode();
    }

    private int fullBoardStatus(Pair<String, String> cookie) {
        return given().cookie(cookie.getFirst(), cookie.getSecond())
                .when()
                .get(ApiPaths.BOARDS + "/" + mockPopulatedBoard.getId() + ApiPaths.FULL)
                .then()
                .extract()
                .statusCode();
    }

    private int signinStatus(String email, String password) {
        return given().contentType(ContentType.JSON)
                .body(SigninRequestDTO.builder().email(email).password(password).build())
                .when()
                .post(ApiPaths.SIGNIN)
                .then()
                .extract()
                .statusCode();
    }

    @Nested
    class AuthFailureCounterTest {
        @Test
        void shouldRaiseBadCredentialsCounterByOne_whenSigninPasswordIsWrong() {
            // arrange
            var badCredentialsBefore =
                    count(
                            MetricNames.AUTH_FAILURES,
                            MetricNames.TAG_REASON,
                            MetricNames.REASON_BAD_CREDENTIALS);
            var unauthenticatedBefore =
                    count(
                            MetricNames.AUTH_FAILURES,
                            MetricNames.TAG_REASON,
                            MetricNames.REASON_UNAUTHENTICATED);

            // act
            var statusCode =
                    signinStatus(getOwningUser().getEmail(), getOwningUserPassword() + "x");

            // assert: one request counts once, and on the matching reason only
            Assertions.assertThat(statusCode).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            Assertions.assertThat(
                            count(
                                    MetricNames.AUTH_FAILURES,
                                    MetricNames.TAG_REASON,
                                    MetricNames.REASON_BAD_CREDENTIALS))
                    .isEqualTo(badCredentialsBefore + 1);
            Assertions.assertThat(
                            count(
                                    MetricNames.AUTH_FAILURES,
                                    MetricNames.TAG_REASON,
                                    MetricNames.REASON_UNAUTHENTICATED))
                    .isEqualTo(unauthenticatedBefore);
        }

        @Test
        void shouldRaiseUnauthenticatedCounterByOne_whenBoardsRequestedWithoutSession() {
            // arrange
            var unauthenticatedBefore =
                    count(
                            MetricNames.AUTH_FAILURES,
                            MetricNames.TAG_REASON,
                            MetricNames.REASON_UNAUTHENTICATED);
            var badCredentialsBefore =
                    count(
                            MetricNames.AUTH_FAILURES,
                            MetricNames.TAG_REASON,
                            MetricNames.REASON_BAD_CREDENTIALS);

            // act
            var statusCode = given().when().get(ApiPaths.BOARDS).then().extract().statusCode();

            // assert
            Assertions.assertThat(statusCode).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            Assertions.assertThat(
                            count(
                                    MetricNames.AUTH_FAILURES,
                                    MetricNames.TAG_REASON,
                                    MetricNames.REASON_UNAUTHENTICATED))
                    .isEqualTo(unauthenticatedBefore + 1);
            Assertions.assertThat(
                            count(
                                    MetricNames.AUTH_FAILURES,
                                    MetricNames.TAG_REASON,
                                    MetricNames.REASON_BAD_CREDENTIALS))
                    .isEqualTo(badCredentialsBefore);
        }
    }

    @Nested
    class OwnershipDeniedCounterTest {
        @Test
        void shouldRaiseCounterByOne_whenBoardOfAnotherUserIsRead() {
            // arrange
            var cookie = signin();
            var before = count(MetricNames.OWNERSHIP_DENIED);

            // act
            var statusCode =
                    given().cookie(cookie.getFirst(), cookie.getSecond())
                            .when()
                            .get(
                                    ApiPaths.BOARDS
                                            + "/"
                                            + getForeignUserBoard().getId()
                                            + ApiPaths.FULL)
                            .then()
                            .extract()
                            .statusCode();

            // assert
            Assertions.assertThat(statusCode).isEqualTo(HttpStatus.FORBIDDEN.value());
            Assertions.assertThat(count(MetricNames.OWNERSHIP_DENIED)).isEqualTo(before + 1);
        }
    }

    @Nested
    class OptimisticLockConflictCounterTest {
        @Test
        void shouldRaiseCounterByOne_whenTaskIsUpdatedWithStaleVersion() {
            // arrange
            var cookie = signin();
            var before = count(MetricNames.OPTIMISTIC_LOCK_CONFLICTS);
            var taskPath =
                    ApiPaths.BOARDS
                            + "/"
                            + mockPopulatedBoard.getId()
                            + ApiPaths.COLUMNS
                            + "/"
                            + mockPopulatedColumn.getId()
                            + ApiPaths.TASKS
                            + "/"
                            + mockPopulatedTask.getId();
            var staleUpdate =
                    UpdateTaskRequestDTO.builder()
                            .title("Stale writer")
                            .version(mockPopulatedTask.getVersion() + 1)
                            .build();

            // act
            var statusCode =
                    given().cookie(cookie.getFirst(), cookie.getSecond())
                            .contentType(ContentType.JSON)
                            .body(staleUpdate)
                            .when()
                            .put(taskPath)
                            .then()
                            .extract()
                            .statusCode();

            // assert
            Assertions.assertThat(statusCode).isEqualTo(HttpStatus.CONFLICT.value());
            Assertions.assertThat(count(MetricNames.OPTIMISTIC_LOCK_CONFLICTS))
                    .isEqualTo(before + 1);
        }
    }

    @Nested
    class TaskCreatedTest {
        @Test
        void shouldRaiseCounterAndTimerByOne_whenTaskIsCreated() {
            // arrange
            var cookie = signin();
            var counterBefore = count(MetricNames.TASKS_CREATED);
            var timerBefore = timerCount(MetricNames.OBSERVATION_TASK_CREATE);

            // act
            var statusCode =
                    given().cookie(cookie.getFirst(), cookie.getSecond())
                            .contentType(ContentType.JSON)
                            .body(
                                    SaveTaskRequestDTO.builder()
                                            .title("Counted task")
                                            .description("Created through the real HTTP path")
                                            .build())
                            .when()
                            .post(columnPath(mockPopulatedColumn.getId()))
                            .then()
                            .extract()
                            .statusCode();

            // assert
            Assertions.assertThat(statusCode).isEqualTo(HttpStatus.CREATED.value());
            Assertions.assertThat(count(MetricNames.TASKS_CREATED)).isEqualTo(counterBefore + 1);
            Assertions.assertThat(timerCount(MetricNames.OBSERVATION_TASK_CREATE))
                    .isEqualTo(timerBefore + 1);
        }

        @Test
        void shouldNotRaiseCounter_whenTaskInsertIsRejectedAtCommit() {
            // arrange: PostgreSQL rejects a NUL character in text, which validation lets through
            var cookie = signin();
            var counterBefore = count(MetricNames.TASKS_CREATED);
            var rowsBefore = taskRepository.countByColumnId(mockPopulatedColumn.getId());

            // act
            var statusCode =
                    given().cookie(cookie.getFirst(), cookie.getSecond())
                            .contentType(ContentType.JSON)
                            .body(SaveTaskRequestDTO.builder().title("bad\u0000title").build())
                            .when()
                            .post(columnPath(mockPopulatedColumn.getId()))
                            .then()
                            .extract()
                            .statusCode();

            // assert: a failed create is not a created task
            Assertions.assertThat(HttpStatus.valueOf(statusCode).isError()).isTrue();
            Assertions.assertThat(taskRepository.countByColumnId(mockPopulatedColumn.getId()))
                    .isEqualTo(rowsBefore);
            Assertions.assertThat(count(MetricNames.TASKS_CREATED)).isEqualTo(counterBefore);
        }
    }

    @Nested
    class TaskMovedTest {
        @Test
        void shouldNotRaiseMoveCounter_whenMoveTransactionRollsBackAfterTheMoveRan() {
            // arrange
            var sameBefore = movedCount(MetricNames.MOVE_SAME_COLUMN);
            var crossBefore = movedCount(MetricNames.MOVE_CROSS_COLUMN);
            var dto =
                    MoveTaskRequestDTO.builder()
                            .targetColumnId(mockColumns.get(0).getId())
                            .version(mockPopulatedTask.getVersion())
                            .build();

            // act: the outer transaction makes the service join it, then fails the commit
            new TransactionTemplate(transactionManager)
                    .executeWithoutResult(
                            status -> {
                                taskService.moveToColumn(
                                        getOwningUser().getId(), mockPopulatedTask.getId(), dto);
                                status.setRollbackOnly();
                            });

            // assert: the move was undone, so it is not a move
            Assertions.assertThat(movedCount(MetricNames.MOVE_CROSS_COLUMN)).isEqualTo(crossBefore);
            Assertions.assertThat(movedCount(MetricNames.MOVE_SAME_COLUMN)).isEqualTo(sameBefore);
        }

        @Test
        void shouldRaiseSameColumnCounterAndTimerByOne_whenTaskIsMovedWithinItsColumn() {
            // arrange
            var cookie = signin();
            var sameBefore = movedCount(MetricNames.MOVE_SAME_COLUMN);
            var crossBefore = movedCount(MetricNames.MOVE_CROSS_COLUMN);
            var timerBefore = timerCount(MetricNames.OBSERVATION_TASK_MOVE);

            // act
            var statusCode =
                    moveStatus(
                            cookie,
                            MoveTaskRequestDTO.builder()
                                    .targetColumnId(mockPopulatedColumn.getId())
                                    .version(mockPopulatedTask.getVersion())
                                    .targetPosition(0)
                                    .build());

            // assert
            Assertions.assertThat(statusCode).isEqualTo(HttpStatus.OK.value());
            Assertions.assertThat(movedCount(MetricNames.MOVE_SAME_COLUMN))
                    .isEqualTo(sameBefore + 1);
            Assertions.assertThat(movedCount(MetricNames.MOVE_CROSS_COLUMN)).isEqualTo(crossBefore);
            Assertions.assertThat(timerCount(MetricNames.OBSERVATION_TASK_MOVE))
                    .isEqualTo(timerBefore + 1);
        }

        @Test
        void shouldRaiseCrossColumnCounterAndTimerByOne_whenTaskIsMovedToAnotherColumn() {
            // arrange
            var cookie = signin();
            var sameBefore = movedCount(MetricNames.MOVE_SAME_COLUMN);
            var crossBefore = movedCount(MetricNames.MOVE_CROSS_COLUMN);
            var timerBefore = timerCount(MetricNames.OBSERVATION_TASK_MOVE);

            // act
            var statusCode =
                    moveStatus(
                            cookie,
                            MoveTaskRequestDTO.builder()
                                    .targetColumnId(mockColumns.get(0).getId())
                                    .version(mockPopulatedTask.getVersion())
                                    .build());

            // assert
            Assertions.assertThat(statusCode).isEqualTo(HttpStatus.OK.value());
            Assertions.assertThat(movedCount(MetricNames.MOVE_CROSS_COLUMN))
                    .isEqualTo(crossBefore + 1);
            Assertions.assertThat(movedCount(MetricNames.MOVE_SAME_COLUMN)).isEqualTo(sameBefore);
            Assertions.assertThat(timerCount(MetricNames.OBSERVATION_TASK_MOVE))
                    .isEqualTo(timerBefore + 1);
        }

        @Test
        void shouldRaiseNeitherMoveCounter_whenMoveVersionIsStale() {
            // arrange
            var cookie = signin();
            var sameBefore = movedCount(MetricNames.MOVE_SAME_COLUMN);
            var crossBefore = movedCount(MetricNames.MOVE_CROSS_COLUMN);
            var conflictsBefore = count(MetricNames.OPTIMISTIC_LOCK_CONFLICTS);

            // act
            var statusCode =
                    moveStatus(
                            cookie,
                            MoveTaskRequestDTO.builder()
                                    .targetColumnId(mockColumns.get(0).getId())
                                    .version(mockPopulatedTask.getVersion() + 1)
                                    .build());

            // assert: a rejected move is a conflict, not a move
            Assertions.assertThat(statusCode).isEqualTo(HttpStatus.CONFLICT.value());
            Assertions.assertThat(movedCount(MetricNames.MOVE_SAME_COLUMN)).isEqualTo(sameBefore);
            Assertions.assertThat(movedCount(MetricNames.MOVE_CROSS_COLUMN)).isEqualTo(crossBefore);
            Assertions.assertThat(count(MetricNames.OPTIMISTIC_LOCK_CONFLICTS))
                    .isEqualTo(conflictsBefore + 1);
        }
    }

    @Nested
    class BoardFullReadTest {
        @Test
        void shouldRaiseTimerByOne_whenFullBoardIsRead() {
            // arrange
            var cookie = signin();
            var timerBefore = timerCount(MetricNames.OBSERVATION_BOARD_FULL_READ);

            // act
            var statusCode = fullBoardStatus(cookie);

            // assert
            Assertions.assertThat(statusCode).isEqualTo(HttpStatus.OK.value());
            Assertions.assertThat(timerCount(MetricNames.OBSERVATION_BOARD_FULL_READ))
                    .isEqualTo(timerBefore + 1);
        }
    }

    @Nested
    class PerEndpointTimerTest {
        @Test
        void shouldTimeFullBoardRead_underItsTemplatedUri() {
            // arrange
            var cookie = signin();
            var before = httpCount("GET", "/boards/{boardId}/full");

            // act
            fullBoardStatus(cookie);

            // assert
            awaitHttpCountAbove("GET", "/boards/{boardId}/full", before);
        }

        @Test
        void shouldTimeTaskCreate_underItsTemplatedUri() {
            // arrange
            var cookie = signin();
            var before = httpCount("POST", "/boards/{boardId}/columns/{columnId}");

            // act
            given().cookie(cookie.getFirst(), cookie.getSecond())
                    .contentType(ContentType.JSON)
                    .body(SaveTaskRequestDTO.builder().title("Timed task").description("d").build())
                    .when()
                    .post(columnPath(mockPopulatedColumn.getId()))
                    .then()
                    .extract()
                    .statusCode();

            // assert
            awaitHttpCountAbove("POST", "/boards/{boardId}/columns/{columnId}", before);
        }

        @Test
        void shouldTimeTaskMove_underItsTemplatedUri() {
            // arrange
            var cookie = signin();
            var before = httpCount("PATCH", "/tasks/{taskId}/move");

            // act
            moveStatus(
                    cookie,
                    MoveTaskRequestDTO.builder()
                            .targetColumnId(mockColumns.get(0).getId())
                            .version(mockPopulatedTask.getVersion())
                            .build());

            // assert
            awaitHttpCountAbove("PATCH", "/tasks/{taskId}/move", before);
        }

        @Test
        void shouldTimeSignin_underItsTemplatedUri() {
            // arrange
            var before = httpCount("POST", "/signin");

            // act
            signinStatus(getOwningUser().getEmail(), getOwningUserPassword());

            // assert
            awaitHttpCountAbove("POST", "/signin", before);
        }
    }

    @Nested
    class ScannerProbeTest {
        @Test
        void shouldAnswerBelow500_whenWrongMethodIsUsedOnSignin() {
            // arrange & act
            var statusCode = given().when().get(ApiPaths.SIGNIN).then().extract().statusCode();

            // assert: a 5xx here would count toward the error-ratio alert for a scanner's probe
            Assertions.assertThat(statusCode).isLessThan(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        @Test
        void shouldAnswerBelow500_whenSigninIsPostedWithUnsupportedMediaType() {
            // arrange & act
            var statusCode =
                    given().contentType(ContentType.TEXT)
                            .body("not json")
                            .when()
                            .post(ApiPaths.SIGNIN)
                            .then()
                            .extract()
                            .statusCode();

            // assert
            Assertions.assertThat(statusCode).isLessThan(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        @Test
        void shouldAnswerBelow500_whenUnknownPathIsRequestedWithSession() {
            // arrange
            var cookie = signin();

            // act
            var statusCode =
                    given().cookie(cookie.getFirst(), cookie.getSecond())
                            .when()
                            .get("/no-such-path")
                            .then()
                            .extract()
                            .statusCode();

            // assert
            Assertions.assertThat(statusCode).isLessThan(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
}
