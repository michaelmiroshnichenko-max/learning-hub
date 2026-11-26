package dev.learninghub.note;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
        apply(note, request);
        note.createdAt = now;
        note.updatedAt = now;
        note.persist();
        return toResponse(note);
    }

    @Transactional
    public Optional<NoteResponse> find(long id, UUID ownerId) {
        return Note.findByIdAndOwner(id, ownerId).map(this::toResponse);
    }

    /** Replaces title, body, week and tags; empty when the note is unknown or not owned (FR-5, FR-9). */
    @Transactional
    public Optional<NoteResponse> update(long id, UUID ownerId, NoteRequest request) {
        return Note.findByIdAndOwner(id, ownerId).map(note -> {
            apply(note, request);
            note.updatedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
            return toResponse(note);
        });
    }

    /**
     * One page in three statements whatever the size (no N+1, no in-memory paging): the projected page query
     * with LIMIT/OFFSET, its count, and the tags of the page's notes.
     */
    @Transactional
    public NotePage list(UUID ownerId, int page, int size, List<String> tagNames) {
        List<String> tags = TagNames.normalise(tagNames).stream().filter(name -> !name.isEmpty()).toList();
        PanacheQuery<Note.Row> query = Note.findRows(ownerId, tags).page(Page.of(page, size));
        List<Note.Row> rows = query.list();
        long totalItems = query.count();
        Map<Long, List<String>> tagsByNote = Note.tagNamesByNoteId(ownerId, rows.stream().map(Note.Row::id).toList());
        List<NoteSummary> items = rows.stream()
                .map(row -> new NoteSummary(
                        row.id(),
                        row.title(),
                        row.courseWeek() == null ? null : row.courseWeek().intValue(),
                        tagsByNote.getOrDefault(row.id(), List.of()),
                        row.updatedAt()))
                .toList();
        return new NotePage(items, page, size, totalItems, (totalItems + size - 1) / size);
    }

    private void apply(Note note, NoteRequest request) {
        note.title = request.title();
        note.body = request.body();
        note.courseWeek = request.courseWeek() == null ? null : request.courseWeek().shortValue();
        note.tags = resolveTags(note.ownerId, request.tags());
    }

    /** Finds the owner's tags by name and creates the missing ones, locked until commit (ADR 0006). */
    private static Set<Tag> resolveTags(UUID ownerId, List<String> names) {
        if (names.isEmpty()) {
            return new HashSet<>();
        }
        // Sorted, so parallel requests lock the same names in the same order and cannot deadlock.
        List<String> sorted = names.stream().sorted().toList();
        return new HashSet<>(Tag.listByOwnerAndIds(ownerId, Tag.upsertAndLock(ownerId, sorted)));
    }

    private NoteResponse toResponse(Note note) {
        return new NoteResponse(
                note.id,
                note.title,
                note.body,
                markdownRenderer.toSafeHtml(note.body),
                note.courseWeek == null ? null : note.courseWeek.intValue(),
                note.tags.stream().map(tag -> tag.name).sorted().toList(),
                note.createdAt,
                note.updatedAt);
    }
}
