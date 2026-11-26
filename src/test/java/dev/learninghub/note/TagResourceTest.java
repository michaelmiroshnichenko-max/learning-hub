package dev.learninghub.note;

import static dev.learninghub.TestUsers.LEARNER_A;
import static dev.learninghub.TestUsers.LEARNER_B;
import static dev.learninghub.note.NoteApi.PROBLEM_JSON;
import static dev.learninghub.note.NoteApi.create;
import static dev.learninghub.note.NoteApi.get;
import static dev.learninghub.note.NoteApi.noteWithTags;
import static dev.learninghub.note.NoteApi.uniqueTag;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.learninghub.TestDb;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** S2: GET and DELETE /api/tags (FR-4, FR-9, ADR 0005). */
@QuarkusTest
@Tag("FR-4")
@TestSecurity(user = LEARNER_A)
class TagResourceTest {

    @Inject
    DataSource dataSource;

    @Test
    void fr4_listReturnsOwnTagsAsIdAndNameSortedByName() {
        String b = "b-" + uniqueTag("list");
        String a = "a-" + uniqueTag("list");
        create(noteWithTags("Tags", b, a));

        JsonPath tags = given().accept(ContentType.JSON).when().get("/api/tags")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("name", hasItem(a))
                .body("name", hasItem(b))
                .body("find { it.name == '" + a + "' }.id", org.hamcrest.Matchers.notNullValue())
                .extract().jsonPath();
        List<String> names = tags.getList("name", String.class);
        List<String> sorted = new ArrayList<>(names);
        sorted.sort(null);
        assertEquals(sorted, names);
    }

    @Test
    void fr4_deleteTagReturns204AndUnlinksItButKeepsNotesAndUpdatedAt() {
        String gone = uniqueTag("gone");
        String stays = uniqueTag("stays");
        Response created = create(noteWithTags("Tagged", gone, stays));
        long noteId = created.jsonPath().getLong("id");
        String updatedAt = created.jsonPath().getString("updatedAt");
        long tagId = tagId(gone);

        given().when().delete("/api/tags/" + tagId).then().statusCode(204);

        get(noteId).then()
                .statusCode(200)
                .body("tags", contains(stays))
                .body("updatedAt", equalTo(updatedAt));
        assertEquals(0, TestDb.countTags(dataSource, LEARNER_A, gone));
        assertEquals(0, TestDb.countTagLinks(dataSource, tagId));
        given().when().get("/api/tags").then().body("name", not(hasItem(gone)));
    }

    @Test
    void fr4_deleteUnknownTagReturns404() {
        given().when().delete("/api/tags/" + Long.MAX_VALUE)
                .then()
                .statusCode(404)
                .contentType(PROBLEM_JSON)
                .body("type", equalTo("/problems/not-found"));
    }

    @Test
    @Tag("FR-9")
    void fr9_deleteOtherOwnersTagReturns404AndKeepsIt() {
        String name = uniqueTag("b-only");
        long notOwned = TestDb.insertTag(dataSource, LEARNER_B, name);

        Response notOwnedResponse = given().when().delete("/api/tags/" + notOwned);
        Response unknownResponse = given().when().delete("/api/tags/" + Long.MAX_VALUE);

        notOwnedResponse.then().statusCode(404).contentType(PROBLEM_JSON);
        assertEquals(unknownResponse.jsonPath().getString("type"), notOwnedResponse.jsonPath().getString("type"));
        assertEquals(unknownResponse.jsonPath().getString("title"), notOwnedResponse.jsonPath().getString("title"));
        assertEquals(1, TestDb.countTags(dataSource, LEARNER_B, name));
    }

    private static long tagId(String name) {
        return given().when().get("/api/tags").then().statusCode(200)
                .extract().jsonPath().getLong("find { it.name == '" + name + "' }.id");
    }
}
