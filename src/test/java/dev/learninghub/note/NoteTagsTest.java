package dev.learninghub.note;

import static dev.learninghub.TestUsers.LEARNER_A;
import static dev.learninghub.note.NoteApi.PROBLEM_JSON;
import static dev.learninghub.note.NoteApi.create;
import static dev.learninghub.note.NoteApi.get;
import static dev.learninghub.note.NoteApi.noteWithTags;
import static dev.learninghub.note.NoteApi.post;
import static dev.learninghub.note.NoteApi.uniqueTag;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.learninghub.TestDb;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** S2: tags given by name on note create and update (FR-4, ADR 0005, ADR 0006). */
@QuarkusTest
@Tag("FR-4")
@TestSecurity(user = LEARNER_A)
class NoteTagsTest {

    @Inject
    DataSource dataSource;

    @Inject
    NoteService noteService;

    @Test
    void fr4_tagNamesAreTrimmedLowercasedAndDeduplicated() {
        String base = uniqueTag("Java");
        String lower = base.toLowerCase(Locale.ROOT);

        create(noteWithTags("Case", base, lower, "  " + base.toUpperCase(Locale.ROOT) + " "))
                .then()
                .body("tags", contains(lower));
        assertEquals(1, TestDb.countTags(dataSource, LEARNER_A, lower));
    }

    @Test
    void fr4_unknownTagNamesAreCreatedForTheOwner() {
        String name = uniqueTag("new");

        create(noteWithTags("Unknown tag", name)).then().body("tags", contains(name));

        assertEquals(1, TestDb.countTags(dataSource, LEARNER_A, name));
        given().when().get("/api/tags").then().statusCode(200).body("name", hasItem(name));
    }

    @Test
    void fr4_existingTagOfTheOwnerIsReused() {
        String name = uniqueTag("reuse");
        long tagId = TestDb.insertTag(dataSource, LEARNER_A, name);

        create(noteWithTags("First", name));
        create(noteWithTags("Second", name.toUpperCase(Locale.ROOT)));

        assertEquals(1, TestDb.countTags(dataSource, LEARNER_A, name));
        assertEquals(2, TestDb.countTagLinks(dataSource, tagId));
    }

    @Test
    void fr4_moreThan10DistinctTagsReturns422StatingTheLimit() {
        String[] eleven = distinctTags(11);

        post(noteWithTags("Too many", eleven)).then()
                .statusCode(422)
                .contentType(PROBLEM_JSON)
                .body("type", equalTo("/problems/validation"))
                .body("errors.field", contains("tags"))
                .body("errors[0].message", containsString("10"));
        assertEquals(0, TestDb.countTags(dataSource, LEARNER_A, eleven[0]));
    }

    @Test
    void fr4_tenDistinctTagsPlusCaseDuplicatesAreAccepted() {
        String[] ten = distinctTags(10);
        List<String> withDuplicates = new ArrayList<>(List.of(ten));
        withDuplicates.add(ten[0].toUpperCase(Locale.ROOT));
        withDuplicates.add(" " + ten[1] + " ");

        create(noteWithTags("Ten", withDuplicates.toArray(String[]::new))).then().body("tags", hasSize(10));
    }

    @Test
    void fr4_blankTagReturns422() {
        post(noteWithTags("Blank tag", "   ")).then()
                .statusCode(422)
                .contentType(PROBLEM_JSON)
                .body("errors.field", contains("tags[0]"));
    }

    @Test
    void fr4_tagLongerThan50CharactersReturns422() {
        post(noteWithTags("Long tag", "x".repeat(51))).then()
                .statusCode(422)
                .contentType(PROBLEM_JSON)
                .body("errors.field", contains("tags[0]"))
                .body("errors[0].message", containsString("50"));
    }

    @Test
    void fr4_noteWithoutTagsHasEmptyTagList() {
        long id = NoteApi.createId(NoteApi.validNote());

        get(id).then().statusCode(200).body("tags", empty());
    }

    @Test
    void fr4_viewReturnsTagsSortedByName() {
        String b = "b-" + uniqueTag("view");
        String a = "a-" + uniqueTag("view");
        long id = NoteApi.createId(noteWithTags("Sorted", b, a));

        get(id).then().statusCode(200).body("tags", contains(a, b));
    }

    @Test
    void fr4_parallelCreatesOfTheSameNewTagBothSucceedWithOneTagRow() throws Exception {
        String name = uniqueTag("race");
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<CompletableFuture<Integer>> calls = List.of(
                    CompletableFuture.supplyAsync(() -> postAfter(start, name), pool),
                    CompletableFuture.supplyAsync(() -> postAfter(start, name), pool));
            start.countDown();

            for (CompletableFuture<Integer> call : calls) {
                assertEquals(201, call.get(30, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, TestDb.countTags(dataSource, LEARNER_A, name));
    }

    /**
     * A save locks the tags it links, so a parallel DELETE /api/tags/{id} waits for it instead of removing the tag
     * between lookup and link (which ends in a foreign key 500 or a silently dropped tag).
     */
    @Test
    void fr4_tagLinkedByAnOpenSaveCannotBeDeletedUntilTheSaveCommits() throws Exception {
        String name = uniqueTag("locked");
        long tagId = TestDb.insertTag(dataSource, LEARNER_A, name);
        ExecutorService other = Executors.newSingleThreadExecutor();
        try {
            long noteId = QuarkusTransaction.requiringNew().call(() -> {
                long id = noteService.create(UUID.fromString(LEARNER_A),
                        new NoteRequest("Locked tag", "Body", null, List.of(name))).id();
                // Another thread has no transaction, so it gets its own connection.
                String outcome = other.submit(() -> deleteTagWithLockTimeout(tagId)).get(30, TimeUnit.SECONDS);
                // 55P03 lock_not_available: the delete had to wait for the save
                assertEquals("55P03", outcome);
                return id;
            });
            get(noteId).then().statusCode(200).body("tags", contains(name));
        } finally {
            other.shutdownNow();
        }
    }

    private String deleteTagWithLockTimeout(long tagId) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET LOCAL lock_timeout = '1s'");
                statement.executeUpdate("DELETE FROM tag WHERE id = " + tagId);
                connection.commit();
                return "deleted";
            } catch (SQLException e) {
                connection.rollback();
                return e.getSQLState();
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    @Test
    @Tag("FR-5")
    void fr4_updateReplacesTheTags() {
        String keep = uniqueTag("keep");
        String drop = uniqueTag("drop");
        String add = uniqueTag("add");
        long id = NoteApi.createId(noteWithTags("Retag", keep, drop));

        put(id, noteWithTags("Retag", keep, add)).then()
                .statusCode(200)
                .body("tags", hasSize(2))
                .body("tags", hasItem(keep))
                .body("tags", hasItem(add));
        get(id).then().body("tags", hasSize(2)).body("tags", hasItem(add));
    }

    @Test
    @Tag("FR-5")
    void fr4_updateWithTooManyTagsReturns422AndKeepsOldTags() {
        String old = uniqueTag("old");
        long id = NoteApi.createId(noteWithTags("Keep tags", old));

        put(id, noteWithTags("Keep tags", distinctTags(11))).then().statusCode(422);

        get(id).then().body("tags", contains(old));
    }

    private static int postAfter(CountDownLatch start, String tag) {
        try {
            start.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        return post(noteWithTags("Race", tag)).statusCode();
    }

    private static io.restassured.response.Response put(long id, Map<String, Object> body) {
        return given().contentType(ContentType.JSON).body(body).when().put("/api/notes/" + id);
    }

    private static String[] distinctTags(int count) {
        String prefix = uniqueTag("many");
        String[] tags = new String[count];
        for (int i = 0; i < count; i++) {
            tags[i] = prefix + "-" + i;
        }
        return tags;
    }
}
