package dev.learninghub.note;

import dev.learninghub.shared.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Create and update request (FR-3, FR-4). Owner, id and times are never taken from the client.
 * Tag names are normalised here, before validation, so the 10-tag limit counts distinct tags (ADR 0005, 0006).
 */
public record NoteRequest(
        @NotBlank
        @Size(max = 200, message = "must be at most 200 characters")
        String title,

        @NotBlank
        @MaxUtf8Bytes(configKey = "learninghub.note.body-max-bytes")
        String body,

        @Min(value = 1, message = "must be a whole number from 1 to 26")
        @Max(value = 26, message = "must be a whole number from 1 to 26")
        Integer courseWeek,

        @Size(max = 10, message = "at most 10 distinct tags are allowed")
        List<@NotBlank @Size(max = Tag.NAME_MAX, message = "must be at most 50 characters") String> tags) {

    public NoteRequest {
        tags = TagNames.normalise(tags);
    }
}
