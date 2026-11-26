package dev.learninghub.note;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class TagService {

    @Transactional
    public List<TagResponse> list(UUID ownerId) {
        return Tag.listByOwner(ownerId).stream().map(tag -> new TagResponse(tag.id, tag.name)).toList();
    }

    /** Unlinks the tag from all notes; the notes and their updatedAt stay unchanged (ADR 0005). */
    @Transactional
    public boolean delete(long id, UUID ownerId) {
        return Tag.deleteByIdAndOwner(id, ownerId);
    }
}
