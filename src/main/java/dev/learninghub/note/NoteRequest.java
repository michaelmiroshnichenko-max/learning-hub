package dev.learninghub.note;

import dev.learninghub.shared.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Create request (FR-3). Owner, id and times are never taken from the client. */
public record NoteRequest(
        @NotBlank
        @Size(max = 200, message = "must be at most 200 characters")
        String title,

        @NotBlank
        @MaxUtf8Bytes(configKey = "learninghub.note.body-max-bytes")
        String body,

        @Min(value = 1, message = "must be a whole number from 1 to 26")
        @Max(value = 26, message = "must be a whole number from 1 to 26")
        Integer courseWeek) {
}
