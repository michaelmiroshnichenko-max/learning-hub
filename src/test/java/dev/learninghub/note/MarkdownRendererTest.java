package dev.learninghub.note;

import static dev.learninghub.TestUsers.LEARNER_A;
import static dev.learninghub.note.NoteApi.createId;
import static dev.learninghub.note.NoteApi.get;
import static dev.learninghub.note.NoteApi.note;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import java.util.Locale;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** S1.9: the rendered body is inert. Runs through the API so the whole render path is covered. */
@QuarkusTest
@Tag("FR-5")
@TestSecurity(user = LEARNER_A)
class MarkdownRendererTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "<script>alert(1)</script>                    | <script",
            "<img src=x onerror=alert(1)>                 | <img",
            "<b>bold</b>                                  | <b>",
            "<a href=\"https://example.com\">x</a>        | <a href",
            "<iframe src=\"https://example.com\"></iframe> | <iframe",
            "[x](javascript:alert(1))                     | javascript:",
            "[x](JaVaScRiPt:alert(1))                     | javascript:",
            "![i](data:text/html;base64,PHNjcmlwdD4=)     | data:",
            "[x](vbscript:msgbox(1))                      | vbscript:"
    })
    void fr5_unsafeContentIsInert(String markdown, String forbidden) {
        long id = createId(note("Unsafe", markdown, null));

        String html = get(id).then().statusCode(200).extract().path("bodyHtml");

        assertThat(html.toLowerCase(Locale.ROOT), not(containsString(forbidden)));
    }

    @Test
    void fr5_rawBodyIsReturnedUnchanged() {
        String markdown = "<script>alert(1)</script>";
        long id = createId(note("Raw", markdown, null));

        get(id).then().statusCode(200).body("body", equalTo(markdown));
    }

    @Test
    void fr5_safeHttpsLinkIsKeptWithRelNofollow() {
        long id = createId(note("Link", "[Quarkus](https://quarkus.io/guides/)", null));

        get(id).then()
                .statusCode(200)
                .body("bodyHtml", containsString("href=\"https://quarkus.io/guides/\""))
                .body("bodyHtml", containsString("nofollow"));
    }

    @Test
    void fr5_fencedCodeBlockIsKeptAsCode() {
        long id = createId(note("Code", "```java\nvar x = 1 < 2;\n```", null));

        get(id).then()
                .statusCode(200)
                .body("bodyHtml", containsString("<pre>"))
                .body("bodyHtml", containsString("var x = 1 &lt; 2;"));
    }
}
