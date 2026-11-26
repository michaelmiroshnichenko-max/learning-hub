package dev.learninghub.note;

import static dev.learninghub.TestUsers.LEARNER_A;
import static dev.learninghub.TestUsers.LEARNER_B;
import static dev.learninghub.note.NoteApi.noteWithTags;
import static dev.learninghub.note.NoteApi.uniqueTag;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.learninghub.TestDb;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.path.json.JsonPath;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.sql.DataSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * S2: GET /api/notes?tag= (FR-7, ADR 0006). Every test filters by its own unique tags, so notes created
 * by other tests never appear and page counts are exact.
 */
@QuarkusTest
@Tag("FR-7")
@TestSecurity(user = LEARNER_A)
class NoteTagFilterTest {

    @Inject
    DataSource dataSource;

    @Test
    void fr7_tagFilterReturnsOnlyNotesWithThatTag() {
        String java = uniqueTag("java");
        String other = uniqueTag("other");
        long tagged = NoteApi.createId(noteWithTags("Tagged", java));
        NoteApi.createId(noteWithTags("Other", other));

        list("tag=" + java).body("items.id", contains((int) tagged))
                .body("items[0].tags", contains(java))
                .body("totalItems", equalTo(1));
    }

    @Test
    void fr7_repeatedTagRequiresAllTags() {
        String a = uniqueTag("a");
        String b = uniqueTag("b");
        long both = NoteApi.createId(noteWithTags("Both", a, b));
        NoteApi.createId(noteWithTags("Only a", a));
        NoteApi.createId(noteWithTags("Only b", b));

        list("tag=" + a + "&tag=" + b).body("items.id", contains((int) both)).body("totalItems", equalTo(1));
    }

    @Test
    void fr7_tagFilterIgnoresCaseAndSpaces() {
        String name = uniqueTag("case");
        long tagged = NoteApi.createId(noteWithTags("Case", name));

        list("tag=" + name.toUpperCase(Locale.ROOT)).body("items.id", contains((int) tagged));
    }

    @Test
    void fr7_unknownTagGivesEmptyPageNotError() {
        list("tag=" + uniqueTag("missing"))
                .body("items", empty())
                .body("page", equalTo(0))
                .body("size", equalTo(20))
                .body("totalItems", equalTo(0))
                .body("totalPages", equalTo(0));
    }

    @Test
    @Tag("FR-9")
    void fr9_otherOwnersTagWithSameNameNeverMatches() {
        String name = uniqueTag("b-tag");
        long notOwned = TestDb.insertNote(dataSource, LEARNER_B, "B's note");
        TestDb.linkTag(dataSource, notOwned, TestDb.insertTag(dataSource, LEARNER_B, name));

        list("tag=" + name).body("items", empty()).body("totalItems", equalTo(0));
    }

    @Test
    @Tag("FR-6")
    void fr7_pagingMetadataCountsOnlyFilteredNotes() {
        String name = uniqueTag("paged");
        for (int i = 0; i < 5; i++) {
            NoteApi.createId(noteWithTags("Paged " + i, name));
        }

        list("tag=" + name + "&page=0&size=2").body("items.size()", equalTo(2))
                .body("page", equalTo(0))
                .body("size", equalTo(2))
                .body("totalItems", equalTo(5))
                .body("totalPages", equalTo(3));
        list("tag=" + name + "&page=2&size=2").body("items.size()", equalTo(1));
        list("tag=" + name + "&page=3&size=2").body("items", empty()).body("totalItems", equalTo(5));
    }

    @Test
    @Tag("FR-6")
    void fr7_equalUpdatedAtIsOrderedByIdDescWithoutRepeatsOrGapsAcrossPages() {
        String name = uniqueTag("stable");
        long tagId = TestDb.insertTag(dataSource, LEARNER_A, name);
        Instant sameTime = Instant.parse("2020-01-01T00:00:00Z");
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            long id = TestDb.insertNote(dataSource, LEARNER_A, "Same time " + i, sameTime);
            TestDb.linkTag(dataSource, id, tagId);
            ids.add(id);
        }
        List<Long> expected = ids.stream().sorted((x, y) -> Long.compare(y, x)).toList();

        List<Long> seen = new ArrayList<>();
        for (int page = 0; page < 3; page++) {
            JsonPath json = list("tag=" + name + "&size=2&page=" + page).extract().jsonPath();
            seen.addAll(json.getList("items.id", Long.class));
        }

        assertEquals(expected, seen);
    }

    private static io.restassured.response.ValidatableResponse list(String query) {
        return given().when().get("/api/notes?" + query).then().statusCode(200);
    }
}
