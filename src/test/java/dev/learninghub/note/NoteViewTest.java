package dev.learninghub.note;

import static dev.learninghub.TestUsers.LEARNER_A;
import static dev.learninghub.note.NoteApi.create;
import static dev.learninghub.note.NoteApi.get;
import static dev.learninghub.note.NoteApi.note;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.nullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@QuarkusTest
@Tag("FR-5")
@TestSecurity(user = LEARNER_A)
class NoteViewTest {

    private static final String UTC_MICROS = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,6})?Z";

    @Test
    void fr5_getOwnNoteReturnsAllFields() {
        Response created = create(note("View me", "Some *text*", 7));
        long id = created.jsonPath().getLong("id");

        get(id).then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo((int) id))
                .body("title", equalTo("View me"))
                .body("body", equalTo("Some *text*"))
                .body("bodyHtml", containsString("<em>text</em>"))
                .body("courseWeek", equalTo(7))
                .body("createdAt", equalTo(created.jsonPath().getString("createdAt")))
                .body("updatedAt", equalTo(created.jsonPath().getString("updatedAt")));
    }

    @Test
    void fr5_bodyHtmlRendersMarkdown() {
        long id = NoteApi.createId(note("Markdown", "# Heading\n\nUse `code`, *em* and **strong**.\n\n- one\n- two", null));

        get(id).then()
                .statusCode(200)
                .body("bodyHtml", containsString("<h1>Heading</h1>"))
                .body("bodyHtml", containsString("<code>code</code>"))
                .body("bodyHtml", containsString("<strong>strong</strong>"))
                .body("bodyHtml", containsString("<li>one</li>"));
    }

    @Test
    void fr5_timesAreIsoUtcWithAtMostMicroseconds() {
        long id = NoteApi.createId(note("Times", "body", null));

        get(id).then()
                .statusCode(200)
                .body("createdAt", matchesRegex(UTC_MICROS))
                .body("updatedAt", matchesRegex(UTC_MICROS));
    }

    @Test
    void fr5_noteWithoutCourseWeekShowsNullWeek() {
        long id = NoteApi.createId(note("No week", "body", null));

        get(id).then()
                .statusCode(200)
                .body("courseWeek", nullValue());
    }
}
