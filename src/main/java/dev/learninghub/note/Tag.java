package dev.learninghub.note;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Internal to the note module. Names are stored trimmed and lowercased (ADR 0005).
 * Every finder takes the owner id (FR-9); never call the inherited {@code findById}, {@code listAll} or {@code count}.
 */
@Entity
@Table(name = "tag")
public class Tag extends PanacheEntity {

    public static final int NAME_MAX = 50;

    @Column(name = "owner_id", nullable = false, updatable = false)
    public UUID ownerId;

    @Column(nullable = false, length = NAME_MAX)
    public String name;

    /** Sorted in Java by code point, so the order does not depend on the database collation. */
    public static List<Tag> listByOwner(UUID ownerId) {
        return Tag.<Tag>list("ownerId", ownerId).stream()
                .sorted(Comparator.comparing((Tag tag) -> tag.name))
                .toList();
    }

    public static List<Tag> listByOwnerAndIds(UUID ownerId, Collection<Long> ids) {
        return list("ownerId = ?1 and id in ?2", ownerId, ids);
    }

    public static Optional<Tag> findByIdAndOwner(long id, UUID ownerId) {
        return find("id = ?1 and ownerId = ?2", id, ownerId).firstResultOptional();
    }

    /** Bulk delete; the database removes the note_tag rows (ON DELETE CASCADE) and leaves the notes as they are. */
    public static boolean deleteByIdAndOwner(long id, UUID ownerId) {
        return delete("id = ?1 and ownerId = ?2", id, ownerId) > 0;
    }

    /**
     * Finds or creates the owner's tags and returns their ids. {@code ON CONFLICT DO UPDATE} (a no-op update)
     * returns existing rows too and row-locks them until commit, so a parallel tag delete waits instead of
     * removing a tag this transaction is about to link (FK error or a dropped tag). Parallel creates of the same
     * new name wait on each other and both succeed (ADR 0006). Each new row takes one {@code tag_seq} value,
     * which Hibernate never hands out itself because its pooled optimizer only uses ids up to the values it fetched.
     */
    @SuppressWarnings("unchecked")
    public static List<Long> upsertAndLock(UUID ownerId, Collection<String> names) {
        if (names.isEmpty()) {
            return List.of();
        }
        List<Number> ids = getEntityManager().createNativeQuery("""
                        INSERT INTO tag (id, owner_id, name)
                        SELECT nextval('tag_seq'), :owner, n FROM unnest(cast(:names AS text[])) AS n
                        ON CONFLICT (owner_id, name) DO UPDATE SET name = excluded.name
                        RETURNING id""")
                .setParameter("owner", ownerId)
                .setParameter("names", names.toArray(String[]::new))
                .getResultList();
        return ids.stream().map(Number::longValue).toList();
    }
}
