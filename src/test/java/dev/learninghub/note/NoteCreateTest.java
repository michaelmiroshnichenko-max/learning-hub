package dev.learninghub.note;

import static dev.learninghub.TestUsers.LEARNER_A;
import static dev.learninghub.note.NoteApi.PROBLEM_JSON;
import static dev.learninghub.note.NoteApi.create;
import static dev.learninghub.note.NoteApi.get;
import static dev.learninghub.note.NoteApi.note;
import static dev.learninghub.note.NoteApi.post;
import static dev.learninghub.note.NoteApi.validNote;
import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.learninghub.TestDb;
import dev.learninghub.TestUsers;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@QuarkusTest
@Tag("FR-3")
@TestSecurity(user = LEARNER_A)
class NoteCreateTest {

    static final int TITLE_MAX = 200;
    static final int BODY_MAX_BYTES = 102_400;

    @Inject
    DataSource dataSource;

    // S1.4

    @Test
    void fr3_createValidNoteReturns201WithLocationAndNote() {
        Response response = create(note("Panache paging", "Use `Page.of`.", 5));

        long id = response.jsonPath().getLong("id");
        assertThat(response.contentType(), containsString("application/json"));
        assertThat(response.header("Location"), endsWith("/api/notes/" + id));
        response.then()
                .body("title", equalTo("Panache paging"))
                .body("body", equalTo("Use `Page.of`."))
                .body("courseWeek", equalTo(5))
                .body("createdAt", notNullValue())
                .body("updatedAt", equalTo(response.jsonPath().getString("createdAt")));
    }

    @Test
    void fr3_locationHeaderPointsToTheCreatedNote() {
        Response created = create(validNote());

        given().accept(ContentType.JSON)
                .when().get(created.header("Location"))
                .then()
                .statusCode(200)
                .body("id", equalTo(created.jsonPath().getInt("id")));
    }

    @Test
    void fr3_createdNoteIsOwnedByCurrentUser() {
        long id = create(validNote()).jsonPath().getLong("id");

        assertEquals(Optional.of(LEARNER_A), TestDb.noteOwner(dataSource, id));
    }

    @Test
    @Tag("FR-9")
    void fr3_fr9_ownerIdInRequestBodyIsIgnored() {
        Map<String, Object> body = validNote();
        body.put("ownerId", TestUsers.LEARNER_B);
        body.put("id", 1);

        long id = create(body).jsonPath().getLong("id");

        assertEquals(Optional.of(LEARNER_A), TestDb.noteOwner(dataSource, id));
        get(id).then().statusCode(200);
    }

    // S1.5

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   \t\n"})
    void fr3_blankTitleIsRejected(String title) {
        assertSingleFieldError(post(note(title, "body", null)), "title");
    }

    @Test
    void fr3_missingTitleIsRejected() {
        assertSingleFieldError(post(Map.of("body", "body")), "title");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   \t\n"})
    void fr3_blankBodyIsRejected(String body) {
        assertSingleFieldError(post(note("title", body, null)), "body");
    }

    @Test
    void fr3_missingBodyIsRejected() {
        assertSingleFieldError(post(Map.of("title", "title")), "body");
    }

    @Test
    void fr3_missingTitleAndBodyNamesBothFields() {
        post(Map.of()).then()
                .statusCode(422)
                .contentType(PROBLEM_JSON)
                .body("errors", hasSize(2))
                .body("errors.field", hasItems("title", "body"))
                .body("detail", equalTo("2 fields are invalid"));
    }

    // S1.6

    @Test
    void fr3_titleAtLimitIsAccepted() {
        create(note("t".repeat(TITLE_MAX), "body", null));
    }

    @Test
    void fr3_titleOverLimitIsRejectedStatingTheLimit() {
        Response response = post(note("t".repeat(TITLE_MAX + 1), "body", null));

        assertSingleFieldError(response, "title");
        response.then().body("errors[0].message", containsString(String.valueOf(TITLE_MAX)));
    }

    @Test
    void fr3_bodyAtByteLimitIsAccepted() {
        create(note("title", "b".repeat(BODY_MAX_BYTES), null));
    }

    @Test
    void fr3_bodyOverByteLimitIsRejectedStatingTheLimit() {
        Response response = post(note("title", "b".repeat(BODY_MAX_BYTES + 1), null));

        assertSingleFieldError(response, "body");
        response.then().body("errors[0].message",
                anyOf(containsString("102400"), containsString("100 KB")));
    }

    @Test
    void fr3_bodyLimitCountsUtf8BytesNotCharacters() {
        // 51,201 characters, 102,402 bytes
        assertSingleFieldError(post(note("title", "é".repeat(BODY_MAX_BYTES / 2 + 1), null)), "body");
    }

    // S1.7

    @ParameterizedTest
    @ValueSource(ints = {1, 26})
    void fr3_courseWeekInRangeIsAccepted(int week) {
        create(note("title", "body", week)).then().body("courseWeek", equalTo(week));
    }

    @Test
    void fr3_missingCourseWeekIsAccepted() {
        create(note("title", "body", null)).then().body("courseWeek", nullValue());
    }

    @Test
    void fr3_nullCourseWeekIsAccepted() {
        given().contentType(ContentType.JSON)
                .body("{\"title\":\"title\",\"body\":\"body\",\"courseWeek\":null}")
                .when().post("/api/notes")
                .then()
                .statusCode(201)
                .body("courseWeek", nullValue());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 27, -1})
    void fr3_courseWeekOutOfRangeIsRejectedStatingTheRange(int week) {
        Response response = post(note("title", "body", week));

        assertSingleFieldError(response, "courseWeek");
        response.then().body("errors[0].message", allOf(containsString("1"), containsString("26")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.5", "\"five\"", "\"5\"", "true", "[5]"})
    void fr3_courseWeekOfWrongTypeIsRejected(String rawWeek) {
        Response response = given().contentType(ContentType.JSON)
                .body("{\"title\":\"title\",\"body\":\"body\",\"courseWeek\":" + rawWeek + "}")
                .when().post("/api/notes");

        assertSingleFieldError(response, "courseWeek");
    }

    @Test
    void fr3_courseWeekBeyondIntegerRangeIsRejectedAsField() {
        Response response = given().contentType(ContentType.JSON)
                .body("{\"title\":\"title\",\"body\":\"body\",\"courseWeek\":99999999999}")
                .when().post("/api/notes");

        assertSingleFieldError(response, "courseWeek");
    }

    @Test
    void fr3_malformedJsonReturns400() {
        given().contentType(ContentType.JSON)
                .body("{\"title\": \"title\", \"body\": ")
                .when().post("/api/notes")
                .then()
                .statusCode(400)
                .contentType(PROBLEM_JSON);
    }

    // S1.2: schema created by Flyway; app start proves Hibernate validate passes

    @Test
    void fr3_noteSequenceIncrementsBy50() {
        assertEquals(Optional.of("50"), TestDb.queryString(dataSource,
                "SELECT increment_by::text FROM pg_sequences WHERE sequencename = 'note_seq'"));
    }

    @Test
    void fr3_databaseRejectsCourseWeekOutOfRange() {
        SQLException e = assertThrows(SQLException.class, () -> TestDb.execute(dataSource, """
                INSERT INTO note (id, owner_id, title, body, course_week, created_at, updated_at)
                VALUES (nextval('note_seq'), ?, 'title', 'body', 27, now(), now())""",
                UUID.fromString(LEARNER_A)));

        assertEquals("23514", e.getSQLState(), "expected check_violation, got: " + e.getMessage());
    }

    @Test
    void fr3_ownerSortIndexExists() {
        Optional<String> indexDef = TestDb.queryString(dataSource, """
                SELECT indexdef FROM pg_indexes
                WHERE tablename = 'note' AND indexdef LIKE '%(owner_id, updated_at DESC, id DESC)%'""");

        assertThat(indexDef.orElse(null), notNullValue());
    }

    private static void assertSingleFieldError(Response response, String field) {
        response.then()
                .statusCode(422)
                .contentType(PROBLEM_JSON)
                .body("type", equalTo("/problems/validation"))
                .body("errors", hasSize(1))
                .body("errors[0].field", equalTo(field))
                .body("errors[0].message", notNullValue());
    }
}
