package dev.learninghub.note;

import dev.learninghub.shared.CurrentUser;
import dev.learninghub.shared.problem.Problem;
import dev.learninghub.shared.problem.ProblemExceptionMappers;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("/api/tags")
// Fully qualified because the Tag entity lives in this package.
@org.eclipse.microprofile.openapi.annotations.tags.Tag(name = "Tags")
@Produces(MediaType.APPLICATION_JSON)
public class TagResource {

    private final TagService tagService;
    private final CurrentUser currentUser;

    public TagResource(TagService tagService, CurrentUser currentUser) {
        this.tagService = tagService;
        this.currentUser = currentUser;
    }

    @GET
    @Operation(summary = "List the current learner's tags, sorted by name (FR-4, FR-9)")
    @APIResponse(responseCode = "200", description = "Own tags", content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(type = SchemaType.ARRAY, implementation = TagResponse.class)))
    @APIResponse(responseCode = "401", description = "Not signed in", content = @Content(
            mediaType = ProblemExceptionMappers.PROBLEM_JSON, schema = @Schema(implementation = Problem.class)))
    public List<TagResponse> list(@Context SecurityContext securityContext) {
        return tagService.list(currentUser.id(securityContext));
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete an own tag and unlink it from notes; the notes stay unchanged (FR-4, ADR 0005)")
    @APIResponse(responseCode = "204", description = "Deleted")
    @APIResponse(responseCode = "401", description = "Not signed in", content = @Content(
            mediaType = ProblemExceptionMappers.PROBLEM_JSON, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "404", description = "Unknown id or not owned by the current learner (FR-9)",
            content = @Content(mediaType = ProblemExceptionMappers.PROBLEM_JSON,
                    schema = @Schema(implementation = Problem.class)))
    public void delete(@PathParam("id") long id, @Context SecurityContext securityContext) {
        if (!tagService.delete(id, currentUser.id(securityContext))) {
            throw new NotFoundException();
        }
    }
}
