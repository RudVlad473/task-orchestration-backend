package com.vrudenko.kanban_board.e2e.observability;

import java.util.List;
import java.util.Objects;

import com.vrudenko.kanban_board.constant.ApiPaths;
import com.vrudenko.kanban_board.dto.user_dto.SigninRequestDTO;
import com.vrudenko.kanban_board.support.fixtures.AbstractAppE2ETest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.ContentType;
import io.vavr.control.Try;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;

/**
 * Proves a failed signin, an unauthenticated request and an ownership denial each leave one
 * structured WARN line on the console, and that the line never carries the submitted credentials.
 *
 * Each case drives the real HTTP path and parses the captured console output as JSON, asserting on
 * fields rather than substrings. Only lines logged after the request starts are inspected, because
 * the fixtures in AbstractAppTest log while they are created.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(OutputCaptureExtension.class)
public class SecurityEventLogE2ETest extends AbstractAppE2ETest {

    private static final String SECURITY_EVENT = "security_event";

    @Autowired private ObjectMapper objectMapper;

    private List<JsonNode> jsonLines(String output) {
        return output.lines()
                .filter(line -> line.startsWith("{"))
                .map(line -> Try.of(() -> objectMapper.readTree(line)).getOrNull())
                .filter(Objects::nonNull)
                .toList();
    }

    private List<JsonNode> securityEvents(String output) {
        return jsonLines(output).stream().filter(line -> line.has(SECURITY_EVENT)).toList();
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
    class AuthFailureTest {
        @Test
        void shouldLogOneAuthFailureEvent_whenSigninPasswordIsWrong(CapturedOutput output) {
            // arrange
            var offset = output.getAll().length();

            // act
            var statusCode =
                    signinStatus(getOwningUser().getEmail(), getOwningUserPassword() + "x");

            // assert
            Assertions.assertThat(statusCode).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            var events = securityEvents(output.getAll().substring(offset));
            Assertions.assertThat(events).hasSize(1);
            var event = events.get(0);
            Assertions.assertThat(event.get(SECURITY_EVENT).asText()).isEqualTo("auth_failure");
            Assertions.assertThat(event.get("reason").asText()).isEqualTo("bad_credentials");
            Assertions.assertThat(event.get("http_method").asText()).isEqualTo("POST");
            Assertions.assertThat(event.get("path").asText()).isEqualTo(ApiPaths.SIGNIN);
            Assertions.assertThat(event.path("log").path("level").asText()).isEqualTo("WARN");
        }

        @Test
        void shouldLogOneAuthFailureEvent_whenSigninEmailIsUnknown(CapturedOutput output) {
            // arrange
            var offset = output.getAll().length();

            // act
            var statusCode = signinStatus(generateValidEmail(), generateValidPassword());

            // assert
            Assertions.assertThat(statusCode).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            var events = securityEvents(output.getAll().substring(offset));
            Assertions.assertThat(events).hasSize(1);
            Assertions.assertThat(events.get(0).get("reason").asText())
                    .isEqualTo("bad_credentials");
        }

        @Test
        void shouldNotLogSubmittedCredentials_whenSigninFails(CapturedOutput output) {
            // arrange
            var email = getOwningUser().getEmail();
            var wrongPassword = getOwningUserPassword() + "x";
            var offset = output.getAll().length();

            // act
            var statusCode = signinStatus(email, wrongPassword);

            // assert: the event exists, so the absence checks below are not vacuous
            Assertions.assertThat(statusCode).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            var logged = output.getAll().substring(offset);
            Assertions.assertThat(securityEvents(logged)).hasSize(1);
            Assertions.assertThat(logged).doesNotContain(email);
            Assertions.assertThat(logged).doesNotContain(wrongPassword);
        }

        @Test
        void shouldLogOneAuthFailureEvent_whenBoardsRequestedWithoutSession(CapturedOutput output) {
            // arrange
            var offset = output.getAll().length();

            // act
            var statusCode = given().when().get(ApiPaths.BOARDS).then().extract().statusCode();

            // assert
            Assertions.assertThat(statusCode).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            var events = securityEvents(output.getAll().substring(offset));
            Assertions.assertThat(events).hasSize(1);
            var event = events.get(0);
            Assertions.assertThat(event.get(SECURITY_EVENT).asText()).isEqualTo("auth_failure");
            Assertions.assertThat(event.get("reason").asText()).isEqualTo("unauthenticated");
            Assertions.assertThat(event.get("http_method").asText()).isEqualTo("GET");
            Assertions.assertThat(event.get("path").asText()).isEqualTo(ApiPaths.BOARDS);
        }
    }

    @Nested
    class AccessDeniedTest {
        @Test
        void shouldLogOneAccessDeniedEvent_whenBoardOfAnotherUserIsRead(CapturedOutput output) {
            // arrange
            var cookie = signin();
            var foreignBoardPath =
                    ApiPaths.BOARDS + "/" + getForeignUserBoard().getId() + ApiPaths.FULL;
            var offset = output.getAll().length();

            // act
            var statusCode =
                    given().cookie(cookie.getFirst(), cookie.getSecond())
                            .when()
                            .get(foreignBoardPath)
                            .then()
                            .extract()
                            .statusCode();

            // assert
            Assertions.assertThat(statusCode).isEqualTo(HttpStatus.FORBIDDEN.value());
            var events = securityEvents(output.getAll().substring(offset));
            Assertions.assertThat(events).hasSize(1);
            var event = events.get(0);
            Assertions.assertThat(event.get(SECURITY_EVENT).asText()).isEqualTo("access_denied");
            Assertions.assertThat(event.get("reason").asText()).isEqualTo("not_owner");
            Assertions.assertThat(event.get("http_method").asText()).isEqualTo("GET");
            Assertions.assertThat(event.get("path").asText()).isEqualTo(foreignBoardPath);
            Assertions.assertThat(event.get("user_id").asText()).isEqualTo(getOwningUser().getId());
        }
    }

    @Nested
    class ConsoleFormatTest {
        @Test
        void shouldWriteUtcTimestampOnEveryJsonLine_whenRequestIsServed(CapturedOutput output) {
            // arrange
            var offset = output.getAll().length();

            // act
            given().when().get(ApiPaths.BOARDS).then().extract().statusCode();

            // assert: JSON lines exist at all, and each carries a UTC instant
            var lines = jsonLines(output.getAll().substring(offset));
            Assertions.assertThat(lines).isNotEmpty();
            Assertions.assertThat(lines)
                    .allSatisfy(
                            line ->
                                    Assertions.assertThat(line.get("@timestamp").asText())
                                            .endsWith("Z"));
        }
    }
}
