package dev.learninghub.note;

import java.time.Instant;

public record NoteResponse(
        long id,
        String title,
        String body,
        String bodyHtml,
        Integer courseWeek,
        Instant createdAt,
        Instant updatedAt) {
}
