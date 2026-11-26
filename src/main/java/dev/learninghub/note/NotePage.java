package dev.learninghub.note;

import java.util.List;

/** Page envelope (ADR 0006); {@code page} is 0-based. */
public record NotePage(List<NoteSummary> items, int page, int size, long totalItems, long totalPages) {
}
