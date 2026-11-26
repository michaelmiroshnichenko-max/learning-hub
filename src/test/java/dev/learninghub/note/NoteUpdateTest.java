package dev.learninghub.note;

import static dev.learninghub.TestUsers.LEARNER_A;
import static dev.learninghub.TestUsers.LEARNER_B;
import static dev.learninghub.note.NoteApi.PROBLEM_JSON;
import static dev.learninghub.note.NoteApi.create;
import static dev.learninghub.note.NoteApi.get;
import static dev.learninghub.note.NoteApi.note;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.learninghub.TestDb;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@QuarkusTest
@Tag("FR-5")
@TestSecurity(user = LEARNER_A)
class NoteUpdateTest {

    @Inject
    DataSource dataSource;

    @Test
    void fr5_updateOwnNoteReturns200AndSetsUpdatedAt() {
        Response created = create(note("Before", "old body", 1));
        long id = created.jsonPath().getLong("id");
        String createdAt = created.jsonPath().getString("createdAt");

        Response updated = put(id, note("After", "new body", 2));

        updated.then()
                .statusCode(200)
                .body("id", equalTo((int) id))
                .body("title", equalTo("After"))
                .body("body", equalTo("new body"))
                .body("courseWeek", equalTo(2))
                .body("createdAt", equalTo(createdAt));
        Instant updatedAt = Instant.parse(updated.jsonPath().getString("updatedAt"));
        assertTrue(updatedAt.isAfter(Instant.parse(createdAt)));
        get(id).then().statusCode(200).body("title", equalTo("After"));
    }

    @Test
    @Tag("FR-3")
    void fr5_updateAppliesCreateValidationAndKeepsTheNote() {
        long id = NoteApi.createId(note("Keep me", "body", null));

        put(id, note(" ", "body", 27)).then()
                .statusCode(422)
                .contentType(PROBLEM_JSON)
                .body("errors", hasSize(2));
        get(id).then().statusCode(200).body("title", equalTo("Keep me"));
    }

    @Test
    @Tag("FR-9")
    void fr5_fr9_ownerIdInUpdateBodyIsIgnored() {
        long id = NoteApi.createId(note("Mine", "body", null));
        Map<String, Object> body = note("Still mine", "body", null);
        body.put("ownerId", LEARNER_B);

        put(id, body).then().statusCode(200);

        assertEquals(Optional.of(LEARNER_A), TestDb.noteOwner(dataSource, id));
    }

    @Test
    @Tag("FR-9")
    void fr9_updateOtherOwnersNoteReturns404AndChangesNothing() {
        long notOwned = TestDb.insertNote(dataSource, LEARNER_B, "B's note");

        put(notOwned, note("Hijacked", "body", null)).then()
                .statusCode(404)
                .contentType(PROBLEM_JSON)
                .body("type", equalTo("/problems/not-found"));
        assertEquals(Optional.of("B's note"), TestDb.noteTitle(dataSource, notOwned));
    }

    @Test
    void fr5_updateUnknownNoteReturns404() {
        put(Long.MAX_VALUE, note("title", "body", null)).then()
                .statusCode(404)
                .contentType(PROBLEM_JSON);
    }

    private static Response put(long id, Object body) {
        return given().contentType(ContentType.JSON).body(body).when().put("/api/notes/" + id);
    }
}
