package dev.learninghub.note;

import static dev.learninghub.TestUsers.LEARNER_A;
import static dev.learninghub.TestUsers.LEARNER_B;
import static dev.learninghub.note.NoteApi.create;
import static dev.learninghub.note.NoteApi.noteWithTags;
import static dev.learninghub.note.NoteApi.uniqueTag;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.learninghub.TestDb;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import javax.sql.DataSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** S2.7: tags are per owner and never leak between users. */
@QuarkusTest
@Tag("FR-9")
@TestSecurity(user = LEARNER_A)
class TagPrivacyTest {

    @Inject
    DataSource dataSource;

    @Test
    void fr9_sameTagNameForTwoOwnersGivesSeparateRows() {
        String name = uniqueTag("shared");
        TestDb.insertTag(dataSource, LEARNER_B, name);

        create(noteWithTags("Mine", name));

        assertEquals(1, TestDb.countTags(dataSource, LEARNER_A, name));
        assertEquals(1, TestDb.countTags(dataSource, LEARNER_B, name));
    }

    @Test
    void fr9_tagListNeverShowsOtherOwnersTags() {
        String name = uniqueTag("b-secret");
        TestDb.insertTag(dataSource, LEARNER_B, name);

        given().when().get("/api/tags").then().statusCode(200).body("name", not(hasItem(name)));
    }
}
