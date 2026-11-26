package dev.learninghub.note;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Internal to the note module. Every finder takes the owner id (FR-9);
 * never call the inherited {@code findById}, {@code listAll} or {@code count}.
 */
@Entity
@Table(name = "note")
public class Note extends PanacheEntity {

    @Column(name = "owner_id", nullable = false, updatable = false)
    public UUID ownerId;

    @Column(nullable = false, length = 200)
    public String title;

    @Column(nullable = false, columnDefinition = "text")
    public String body;

    @Column(name = "course_week")
    public Short courseWeek;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    /** Lazy and never fetch-joined in paged queries; lists load tags for a whole page in one query. */
    @ManyToMany
    @JoinTable(name = "note_tag",
            joinColumns = @JoinColumn(name = "note_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    public Set<Tag> tags = new HashSet<>();

    public static Optional<Note> findByIdAndOwner(long id, UUID ownerId) {
        return find("id = ?1 and ownerId = ?2", id, ownerId).firstResultOptional();
    }

    /** Columns of a list item; projecting keeps the body and the tags collection out of the page query. */
    public record Row(Long id, String title, Short courseWeek, Instant updatedAt) {
    }

    /**
     * Own notes, newest updated first with id as tie-breaker (ADR 0006). With tags, a note must have all of
     * them: the subquery keeps notes whose matching tag count equals the number of distinct names (FR-7).
     */
    public static PanacheQuery<Row> findRows(UUID ownerId, List<String> tagNames) {
        Sort sort = Sort.descending("updatedAt", "id");
        if (tagNames.isEmpty()) {
            return find("ownerId = :owner", sort, Parameters.with("owner", ownerId)).project(Row.class);
        }
        return find("""
                        ownerId = :owner and id in (
                            select n.id from Note n join n.tags t
                            where n.ownerId = :owner and t.ownerId = :owner and t.name in :tags
                            group by n.id having count(t) = :tagCount)""",
                sort,
                Parameters.with("owner", ownerId).and("tags", tagNames).and("tagCount", (long) tagNames.size()))
                .project(Row.class);
    }

    /** Tag names of the given notes in one query (no N+1), sorted by name per note. */
    public static Map<Long, List<String>> tagNamesByNoteId(UUID ownerId, Collection<Long> noteIds) {
        if (noteIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<String>> names = new HashMap<>();
        getEntityManager()
                .createQuery("select n.id, t.name from Note n join n.tags t where n.ownerId = :owner and n.id in :ids",
                        Object[].class)
                .setParameter("owner", ownerId)
                .setParameter("ids", noteIds)
                .getResultList()
                .forEach(row -> names.computeIfAbsent((Long) row[0], id -> new ArrayList<>()).add((String) row[1]));
        names.values().forEach(Collections::sort);
        return names;
    }
}
