package dev.learninghub.note;

import static dev.learninghub.TestUsers.LEARNER_A;
import static dev.learninghub.TestUsers.LEARNER_B;
import static dev.learninghub.note.NoteApi.PROBLEM_JSON;
import static dev.learninghub.note.NoteApi.get;
import static dev.learninghub.note.NoteApi.post;
import static dev.learninghub.note.NoteApi.validNote;
import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.learninghub.TestDb;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** S1.10: other owners' notes behave exactly like unknown ids. */
@QuarkusTest
@Tag("FR-9")
class NotePrivacyTest {

    @Inject
    DataSource dataSource;

    @Test
    @TestSecurity(user = LEARNER_B)
    void fr9_otherOwnersNoteReturns404ProblemWithoutItsContent() {
        String secretTitle = "secret-" + UUID.randomUUID();
        long notOwned = TestDb.insertNote(dataSource, LEARNER_A, secretTitle);

        Response response = get(notOwned);

        response.then()
                .statusCode(404)
                .contentType(PROBLEM_JSON)
                .body("type", equalTo("/problems/not-found"))
                .body("status", equalTo(404))
                .body("instance", equalTo("/api/notes/" + notOwned));
        assertThat(response.asString(), not(containsString(secretTitle)));
    }

    @Test
    @TestSecurity(user = LEARNER_B)
    void fr9_otherOwnersNoteAndUnknownIdGiveTheSameProblem() {
        long notOwned = TestDb.insertNote(dataSource, LEARNER_A, "A's note");
        long unknown = Long.MAX_VALUE;

        JsonPath forNotOwned = get(notOwned).then().statusCode(404).extract().jsonPath();
        JsonPath forUnknown = get(unknown).then().statusCode(404).extract().jsonPath();

        Map<String, Object> notOwnedBody = new HashMap<>(forNotOwned.getMap("$"));
        Map<String, Object> unknownBody = new HashMap<>(forUnknown.getMap("$"));
        notOwnedBody.remove("instance");
        unknownBody.remove("instance");
        assertEquals(unknownBody, notOwnedBody);
    }

    @Test
    @TestSecurity(user = LEARNER_A)
    void fr9_nonNumericIdReturns404Problem() {
        given().when().get("/api/notes/abc")
                .then()
                .statusCode(404)
                .contentType(PROBLEM_JSON)
                .body("type", equalTo("/problems/not-found"));
    }

    @Test
    @TestSecurity(user = LEARNER_B)
    void fr9_noteCreatedByBIsOwnedByB() {
        long id = NoteApi.createId(validNote());

        assertEquals(Optional.of(LEARNER_B), TestDb.noteOwner(dataSource, id));
    }

    @Test
    void fr9_anonymousCreateIsRejectedWith401() {
        post(validNote()).then()
                .statusCode(401)
                .contentType(PROBLEM_JSON);
    }

    @Test
    void fr9_anonymousGetIsRejectedWith401() {
        long id = TestDb.insertNote(dataSource, LEARNER_A, "A's note");

        get(id).then()
                .statusCode(401)
                .contentType(PROBLEM_JSON);
    }
}
