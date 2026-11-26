package dev.learninghub.note;

import java.time.Instant;
import java.util.List;

/** {@code tags} are sorted by name. */
public record NoteResponse(
        long id,
        String title,
        String body,
        String bodyHtml,
        Integer courseWeek,
        List<String> tags,
        Instant createdAt,
        Instant updatedAt) {
}
