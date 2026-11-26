package dev.learninghub.shared;

import static dev.learninghub.TestUsers.LEARNER_A;
import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** S1.11: RFC 9457 problem details as in architecture section 5. */
@QuarkusTest
@TestSecurity(user = LEARNER_A)
class ProblemDetailsTest {

    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String MARKER = "LEAK-MARKER-7f3a";

    @Test
    @Tag("FR-3")
    void fr3_validationErrorIsProblemDetails() {
        given().contentType(ContentType.JSON)
                .body(Map.of("title", " ", "body", "body"))
                .when().post("/api/notes")
                .then()
                .statusCode(422)
                .contentType(PROBLEM_JSON)
                .body("type", equalTo("/problems/validation"))
                .body("title", equalTo("Validation failed"))
                .body("status", equalTo(422))
                .body("detail", equalTo("1 field is invalid"))
                .body("instance", equalTo("/api/notes"))
                .body("errors", hasSize(1))
                .body("errors[0].field", equalTo("title"))
                .body("errors[0].message", notNullValue());
    }

    @Test
    @Tag("FR-3")
    void fr3_errorFieldIsJsonPropertyPathNotJavaPath() {
        given().contentType(ContentType.JSON)
                .body(Map.of("courseWeek", 99))
                .when().post("/api/notes")
                .then()
                .statusCode(422)
                .body("errors.field", everyItem(matchesRegex("[a-z][A-Za-z]*(\\[\\d+])?")));
    }

    @Test
    @Tag("FR-3")
    void fr3_malformedJsonIsProblemDetails400() {
        given().contentType(ContentType.JSON)
                .body("{not json")
                .when().post("/api/notes")
                .then()
                .statusCode(400)
                .contentType(PROBLEM_JSON)
                .body("type", equalTo("/problems/malformed-request"))
                .body("status", equalTo(400))
                .body("instance", equalTo("/api/notes"));
    }

    static Stream<String> leakingRequests() {
        return Stream.of(
                "{\"title\":\"" + MARKER + "-" + "x".repeat(200) + "\",\"body\":\"" + MARKER + "\"}",
                "{\"title\":\"t\",\"body\":\"b\",\"courseWeek\":\"" + MARKER + "\"}",
                "{\"title\":\"" + MARKER + "\",\"body\": " + MARKER,
                "{\"title\":\"t\",\"body\":\"b\",\"courseWeek\":12345678901234567890" + MARKER + "}");
    }

    @ParameterizedTest
    @Tag("NFR-9")
    @MethodSource("leakingRequests")
    void nfr9_errorBodyHasNoStackTraceOrSubmittedValue(String json) {
        Response response = given().contentType(ContentType.JSON)
                .body(json)
                .when().post("/api/notes");

        String body = response.asString();
        assertThat(response.contentType(), containsString(PROBLEM_JSON));
        assertThat(body, not(containsString(MARKER)));
        assertThat(body, not(containsString("Exception")));
        assertThat(body, not(containsString("dev.learninghub")));
        assertThat(body, not(containsString("\tat ")));
    }

    @Test
    @Tag("FR-9")
    void fr9_unknownApiPathIsProblemDetails404() {
        given().when().get("/api/does-not-exist")
                .then()
                .statusCode(404)
                .contentType(PROBLEM_JSON)
                .body("type", equalTo("/problems/not-found"))
                .body("status", equalTo(404));
    }
}
