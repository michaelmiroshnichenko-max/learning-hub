package dev.learninghub.note;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

/**
 * Query of {@code GET /api/notes} (ADR 0006). A bean, not plain parameters, so validation errors name the
 * query parameter ({@code page}, {@code size}) in {@code errors[].field}.
 */
public class NoteListQuery {

    static final int SIZE_MAX = 100;
    /** Keeps page * size inside the int offset Hibernate uses; a larger page would wrap around. */
    static final int PAGE_MAX = Integer.MAX_VALUE / SIZE_MAX;

    @QueryParam("page")
    @DefaultValue("0")
    @Min(value = 0, message = "must be from 0 to " + PAGE_MAX)
    @Max(value = PAGE_MAX, message = "must be from 0 to " + PAGE_MAX)
    @Parameter(description = "0-based page index")
    public int page;

    @QueryParam("size")
    @DefaultValue("20")
    @Min(value = 1, message = "must be from 1 to " + SIZE_MAX)
    @Max(value = SIZE_MAX, message = "must be from 1 to " + SIZE_MAX)
    @Parameter(description = "Notes per page, 1 to 100")
    public int size;

    @QueryParam("tag")
    @Parameter(description = "Tag name; repeat to require all given tags. Case is ignored; an unknown tag gives an empty page")
    public List<String> tag;
}
