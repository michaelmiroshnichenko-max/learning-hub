package dev.learninghub.note;

import static dev.learninghub.TestUsers.LEARNER_A;
import static dev.learninghub.note.NoteApi.uniqueTag;
import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import dev.learninghub.TestDb;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import javax.sql.DataSource;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * N+1 guard: a page of tagged notes costs a fixed number of statements (page, count, tags of the page),
 * whatever the page size. Needs {@code %test.quarkus.hibernate-orm.statistics=true}.
 */
@QuarkusTest
@Tag("FR-6")
@Tag("FR-7")
@TestSecurity(user = LEARNER_A)
class NoteListQueryCountTest {

    @Inject
    DataSource dataSource;

    @Inject
    SessionFactory sessionFactory;

    @Test
    void fr6_listOfTaggedNotesUsesAtMostThreeStatements() {
        long[] tagIds = {
                TestDb.insertTag(dataSource, LEARNER_A, uniqueTag("n1")),
                TestDb.insertTag(dataSource, LEARNER_A, uniqueTag("n2")),
                TestDb.insertTag(dataSource, LEARNER_A, uniqueTag("n3"))};
        for (int i = 0; i < 25; i++) {
            long noteId = TestDb.insertNote(dataSource, LEARNER_A, "Counted " + i);
            for (long tagId : tagIds) {
                TestDb.linkTag(dataSource, noteId, tagId);
            }
        }
        Statistics statistics = sessionFactory.getStatistics();
        assertThat("hibernate statistics enabled in %test", statistics.isStatisticsEnabled(), equalTo(true));
        statistics.clear();

        given().when().get("/api/notes?size=20")
                .then()
                .statusCode(200)
                .body("items", hasSize(20))
                .body("items.tags", everyItem(hasSize(3)))
                .body("totalItems", greaterThanOrEqualTo(25));

        assertThat("prepared statements for one list page", statistics.getPrepareStatementCount(),
                lessThanOrEqualTo(3L));
    }
}
