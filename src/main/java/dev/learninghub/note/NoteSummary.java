package dev.learninghub.note;

import java.time.Instant;
import java.util.List;

/** List item without the body (architecture section 3.1); {@code tags} are sorted by name. */
public record NoteSummary(long id, String title, Integer courseWeek, List<String> tags, Instant updatedAt) {
}
