package com.vrudenko.kanban_board.e2e.observability;

import com.vrudenko.kanban_board.constant.ApiPaths;
import com.vrudenko.kanban_board.constant.MetricNames;
import com.vrudenko.kanban_board.dto.task_dto.UpdateTaskRequestDTO;
import com.vrudenko.kanban_board.dto.user_dto.SigninRequestDTO;
import com.vrudenko.kanban_board.support.fixtures.AbstractAppE2ETest;

import io.micrometer.core.instrument.MeterRegistry;
import io.restassured.http.ContentType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

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

    private double count(String name, String... tags) {
        var counter = meterRegistry.find(name).tags(tags).counter();

        return counter == null ? 0.0 : counter.count();
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
