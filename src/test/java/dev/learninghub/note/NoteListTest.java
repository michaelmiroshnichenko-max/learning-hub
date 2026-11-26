package dev.learninghub.note;

import static dev.learninghub.TestUsers.LEARNER_A;
import static dev.learninghub.TestUsers.LEARNER_B;
import static dev.learninghub.note.NoteApi.PROBLEM_JSON;
import static dev.learninghub.note.NoteApi.note;
import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.learninghub.TestDb;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.path.json.JsonPath;
import jakarta.inject.Inject;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Other tests also create notes for learner A, so assertions only rely on the notes created here being
 * the newest ones (tests do not run in parallel).
 */
@QuarkusTest
@Tag("FR-6")
@TestSecurity(user = LEARNER_A)
class NoteListTest {

    @Inject
    DataSource dataSource;

    @Test
    void fr6_listReturnsNewestUpdatedFirstInPageEnvelope() {
        List<Long> created = createThree();

        JsonPath page = list("size=100").extract().jsonPath();

        assertEquals(List.of(created.get(2), created.get(1), created.get(0)),
                page.getList("items.id", Long.class).subList(0, 3));
        assertEquals(0, page.getInt("page"));
        assertEquals(100, page.getInt("size"));
        long total = page.getLong("totalItems");
        assertThat(total, greaterThanOrEqualTo(3L));
        assertEquals((total + 99) / 100, page.getLong("totalPages"));
    }

    @Test
    void fr6_defaultPageIsZeroWithSize20() {
        createThree();

        list("").body("page", equalTo(0))
                .body("size", equalTo(20))
                .body("items.size()", lessThanOrEqualTo(20));
    }

    @Test
    void fr6_pagesSplitWithoutDuplicatesOrGaps() {
        List<Long> created = createThree();

        List<Long> first = list("page=0&size=2").extract().jsonPath().getList("items.id", Long.class);
        List<Long> second = list("page=1&size=2").extract().jsonPath().getList("items.id", Long.class);

        assertEquals(List.of(created.get(2), created.get(1)), first);
        assertEquals(created.get(0), second.get(0));
    }

    @Test
    void fr6_itemsAreSummariesWithoutBody() {
        createThree();

        list("size=1").body("items", hasSize(1))
                .body("items[0]", hasKey("title"))
                .body("items[0]", hasKey("updatedAt"))
                .body("items[0]", not(hasKey("body")))
                .body("items[0]", not(hasKey("bodyHtml")));
    }

    @ParameterizedTest
    @CsvSource({
            "size=101, size",
            "size=0, size",
            "page=-1, page",
            // page * size must fit the int offset Hibernate uses; past it the offset wraps (500 or wrong page)
            "page=21474837&size=100, page",
            "page=2147483647, page"
    })
    void fr6_invalidPagingReturns422(String query, String field) {
        given().when().get("/api/notes?" + query)
                .then()
                .statusCode(422)
                .contentType(PROBLEM_JSON)
                .body("type", equalTo("/problems/validation"))
                .body("errors.field", hasItem(field));
    }

    @Test
    @Tag("FR-9")
    void fr9_listNeverContainsOtherOwnersNotes() {
        long notOwned = TestDb.insertNote(dataSource, LEARNER_B, "B's newest note");

        list("size=100").body("items.id", not(hasItem((int) notOwned)));
    }

    private List<Long> createThree() {
        return List.of(
                NoteApi.createId(note("List 1", "body", null)),
                NoteApi.createId(note("List 2", "body", null)),
                NoteApi.createId(note("List 3", "body", null)));
    }

    private static io.restassured.response.ValidatableResponse list(String query) {
        return given().when().get("/api/notes" + (query.isEmpty() ? "" : "?" + query))
                .then()
                .statusCode(200);
    }
}
