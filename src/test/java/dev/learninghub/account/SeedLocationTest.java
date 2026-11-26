package dev.learninghub.account;

import static dev.learninghub.TestUsers.LEARNER_A;
import static dev.learninghub.TestUsers.LEARNER_B;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.learninghub.TestDb;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

/** S1.3: two learners are seeded from db/seed, and that location is never enabled in production. */
@QuarkusTest
class SeedLocationTest {

    @Inject
    DataSource dataSource;

    @Test
    void seedLearnersExistInTestProfile() {
        assertEquals(Optional.of("2"), TestDb.queryString(dataSource,
                "SELECT count(*)::text FROM account WHERE id IN (?, ?)",
                UUID.fromString(LEARNER_A), UUID.fromString(LEARNER_B)));
    }

    @Test
    void seedLocationIsEnabledOnlyInDevAndTestProfiles() throws IOException {
        List<String> lines = Files.readAllLines(Path.of("src/main/resources/application.properties")).stream()
                .map(String::strip)
                .toList();

        assertTrue(lines.contains("quarkus.flyway.locations=db/migration"));
        List<String> seedLines = lines.stream().filter(l -> l.contains("db/seed")).toList();
        assertTrue(!seedLines.isEmpty());
        assertTrue(seedLines.stream().allMatch(l -> l.startsWith("%dev.") || l.startsWith("%test.")),
                () -> "db/seed outside %dev/%test: " + seedLines);
    }

    @Test
    void devSeedLocationIsEnabledOnlyInDevProfile() throws IOException {
        List<String> devSeedLines = Files.readAllLines(Path.of("src/main/resources/application.properties")).stream()
                .map(String::strip)
                .filter(l -> l.contains("db/dev-seed"))
                .toList();

        assertEquals(List.of("%dev.quarkus.flyway.locations=db/migration,db/dev-seed"), devSeedLines);
    }

    @Test
    void devSeedIsNotAppliedInTestProfile() {
        assertEquals(Optional.of("0"), TestDb.queryString(dataSource,
                "SELECT count(*)::text FROM flyway_schema_history WHERE script LIKE '%dev_seed%'"));
        assertEquals(Optional.of("0"), TestDb.queryString(dataSource,
                "SELECT count(*)::text FROM account WHERE login IN ('dev-user', 'other-user')"));
    }
}
