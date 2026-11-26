package dev.learninghub.note;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class NoteService {

    private final MarkdownRenderer markdownRenderer;

    public NoteService(MarkdownRenderer markdownRenderer) {
        this.markdownRenderer = markdownRenderer;
    }

    @Transactional
    public NoteResponse create(UUID ownerId, NoteRequest request) {
        // PostgreSQL keeps microseconds; truncate so the response equals what is stored (ADR 0005).
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        Note note = new Note();
        note.ownerId = ownerId;
        note.title = request.title();
        note.body = request.body();
        note.courseWeek = request.courseWeek() == null ? null : request.courseWeek().shortValue();
        note.createdAt = now;
        note.updatedAt = now;
        note.persist();
        return toResponse(note);
    }

    public Optional<NoteResponse> find(long id, UUID ownerId) {
        return Note.findByIdAndOwner(id, ownerId).map(this::toResponse);
    }

    private NoteResponse toResponse(Note note) {
        return new NoteResponse(
                note.id,
                note.title,
                note.body,
                markdownRenderer.toSafeHtml(note.body),
                note.courseWeek == null ? null : note.courseWeek.intValue(),
                note.createdAt,
                note.updatedAt);
    }
}
