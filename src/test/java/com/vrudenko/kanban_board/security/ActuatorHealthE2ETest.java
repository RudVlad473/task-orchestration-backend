package com.vrudenko.kanban_board.security;

import com.vrudenko.kanban_board.support.fixtures.AbstractAppE2ETest;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;

/**
 * Proves health is public only at /api/healthz and the actuator surface answers only on the
 * unrouted management port.
 *
 * The exposure allowlist (health and prometheus, nothing else) is asserted here too.
 *
 * Why this is the way it is:
 * It extends AbstractAppE2ETest, not the MockMvc tier, to exercise the embedded servlet
 * container's context-path stripping (server.servlet.context-path=/api) and the second Tomcat
 * connector the management server opens; a matcher that looks right in isolation can pass a
 * logged-in-browser curl while the unauthenticated health check gets a 302/401.
 *
 * Boot disables metric export in tests, so the context turns the Prometheus registry back on;
 * without that the scrape endpoint would answer 404 here and still be live in production.
 *
 * It carries no @Tag("realSocket"): nothing here is concurrent or slow, so it runs in the
 * pre-commit fastTest gate (docs/CODE_STYLE.md rule 4).
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.prometheus.metrics.export.enabled=true")
public class ActuatorHealthE2ETest extends AbstractAppE2ETest {

    private static final String HEALTHZ_PATH = "/healthz";
    private static final String RETIRED_HEALTH_PATH = "/actuator/health";
    private static final String PROMETHEUS_PATH = "/actuator/prometheus";
    private static final String ENV_PATH = "/actuator/env";

    @LocalManagementPort private int managementPort;

    @Nested
    class PublicHealthTest {
        @Test
        void shouldReportStatusUp_whenHealthzCheckedWithoutSession() {
            // arrange & act
            var status =
                    given().when()
                            .get(HEALTHZ_PATH)
                            .then()
                            .extract()
                            .jsonPath()
                            .getString("status");

            // assert
            Assertions.assertThat(status).isEqualTo("UP");
        }

        @Test
        void shouldNotIncludeComponentDetail_whenHealthzCheckedWithoutSession() {
            // arrange & act
            var response = given().when().get(HEALTHZ_PATH).then().extract();
            var body = response.body().asString();

            // assert: show-details=never means no per-component detail block (datasource URL,
            // driver, validation query) ever appears in an unauthenticated response; the status
            // check keeps an error body from satisfying the absence checks vacuously
            Assertions.assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
            Assertions.assertThat(body).doesNotContain("components");
            Assertions.assertThat(body).doesNotContain("\"details\"");
        }
    }

    @Nested
    class RetiredActuatorPathTest {
        @Test
        void shouldReturnUnauthorized_whenRetiredHealthPathRequestedWithoutSession() {
            // arrange & act
            var statusCode = given().when().get(RETIRED_HEALTH_PATH).then().extract().statusCode();

            // assert: a 401, not the 500 the catch-all handler gives a permitted path that no
            // longer exists on this port, which would count toward the 5xx ratio
            Assertions.assertThat(statusCode).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }

        @Test
        void shouldReturnUnauthorized_whenPrometheusPathRequestedOnRoutedPortWithoutSession() {
            // arrange & act
            var statusCode = given().when().get(PROMETHEUS_PATH).then().extract().statusCode();

            // assert
            Assertions.assertThat(statusCode).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }
    }

    @Nested
    class ManagementPortTest {
        @Test
        void shouldReportStatusUp_whenHealthCheckedOnManagementPort() {
            // arrange & act: the management server has no /api context path
            var response =
                    given().port(managementPort)
                            .basePath("")
                            .when()
                            .get(RETIRED_HEALTH_PATH)
                            .then()
                            .extract();

            // assert
            Assertions.assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
            Assertions.assertThat(response.jsonPath().getString("status")).isEqualTo("UP");
        }

        @Test
        void shouldExposeJvmMetrics_whenPrometheusScrapedOnManagementPort() {
            // arrange & act
            var response =
                    given().port(managementPort)
                            .basePath("")
                            .when()
                            .get(PROMETHEUS_PATH)
                            .then()
                            .extract();

            // assert
            Assertions.assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
            Assertions.assertThat(response.body().asString()).contains("jvm_memory_used_bytes");
        }

        @Test
        void shouldReturnUnauthorized_whenEnvEndpointRequestedOnManagementPort() {
            // arrange & act
            var statusCode =
                    given().port(managementPort)
                            .basePath("")
                            .when()
                            .get(ENV_PATH)
                            .then()
                            .extract()
                            .statusCode();

            // assert: the exposure allowlist is exactly health and prometheus, so env is not an
            // exposed endpoint: the management chain (EndpointRequest.toAnyEndpoint()) does not
            // match it, the request falls through to the main chain and is refused as
            // unauthenticated; an exposed env would answer 200 here
            Assertions.assertThat(statusCode).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }
    }
}
