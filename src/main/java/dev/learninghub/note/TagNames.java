package dev.learninghub.note;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/** Tag name rules (FR-4, ADR 0005): trimmed, lowercased with {@code Locale.ROOT}, duplicates removed. */
final class TagNames {

    private TagNames() {
    }

    /** Keeps blank names (as {@code ""}) so validation can reject them at their position. */
    static List<String> normalise(Collection<String> names) {
        if (names == null) {
            return List.of();
        }
        LinkedHashSet<String> distinct = new LinkedHashSet<>();
        for (String name : names) {
            distinct.add(normalise(name));
        }
        return List.copyOf(distinct);
    }

    static String normalise(String name) {
        return name == null ? "" : name.strip().toLowerCase(Locale.ROOT);
    }
}
