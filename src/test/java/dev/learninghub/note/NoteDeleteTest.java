package dev.learninghub.note;

import static dev.learninghub.TestUsers.LEARNER_A;
import static dev.learninghub.TestUsers.LEARNER_B;
import static dev.learninghub.note.NoteApi.PROBLEM_JSON;
import static dev.learninghub.note.NoteApi.get;
import static dev.learninghub.note.NoteApi.validNote;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.learninghub.TestDb;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Disabled("S3: DELETE /api/notes/{id} is not implemented yet")
@QuarkusTest
@Tag("FR-5")
@TestSecurity(user = LEARNER_A)
class NoteDeleteTest {

    @Inject
    DataSource dataSource;

    @Test
    void fr5_deleteOwnNoteReturns204AndRemovesIt() {
        long id = NoteApi.createId(validNote());

        given().when().delete("/api/notes/" + id).then().statusCode(204);

        get(id).then().statusCode(404);
        assertEquals(Optional.empty(), TestDb.noteTitle(dataSource, id));
    }

    @Test
    @Tag("FR-9")
    void fr9_deleteOtherOwnersNoteReturns404AndKeepsIt() {
        long notOwned = TestDb.insertNote(dataSource, LEARNER_B, "B's note");

        given().when().delete("/api/notes/" + notOwned)
                .then()
                .statusCode(404)
                .contentType(PROBLEM_JSON)
                .body("type", equalTo("/problems/not-found"));
        assertEquals(Optional.of("B's note"), TestDb.noteTitle(dataSource, notOwned));
    }

    @Test
    void fr5_deleteUnknownNoteReturns404() {
        given().when().delete("/api/notes/" + Long.MAX_VALUE)
                .then()
                .statusCode(404)
                .contentType(PROBLEM_JSON);
    }
}
