package dev.learninghub.note;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Optional;
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

    public static Optional<Note> findByIdAndOwner(long id, UUID ownerId) {
        return find("id = ?1 and ownerId = ?2", id, ownerId).firstResultOptional();
    }
}
