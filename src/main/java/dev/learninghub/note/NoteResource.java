package dev.learninghub.note;

import dev.learninghub.shared.CurrentUser;
import dev.learninghub.shared.problem.Problem;
import dev.learninghub.shared.problem.ProblemExceptionMappers;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/notes")
@Tag(name = "Notes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NoteResource {

    private final NoteService noteService;
    private final CurrentUser currentUser;

    public NoteResource(NoteService noteService, CurrentUser currentUser) {
        this.noteService = noteService;
        this.currentUser = currentUser;
    }

    @POST
    @Operation(summary = "Create a note owned by the current learner (FR-3)")
    @APIResponse(responseCode = "201", description = "Created; Location points to the new note",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = NoteResponse.class)))
    @APIResponse(responseCode = "400", description = "Malformed JSON", content = @Content(
            mediaType = ProblemExceptionMappers.PROBLEM_JSON, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "401", description = "Not signed in", content = @Content(
            mediaType = ProblemExceptionMappers.PROBLEM_JSON, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "415", description = "Content-Type is not application/json", content = @Content(
            mediaType = ProblemExceptionMappers.PROBLEM_JSON, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "422", description = "Validation failed; errors[] names each field",
            content = @Content(mediaType = ProblemExceptionMappers.PROBLEM_JSON,
                    schema = @Schema(implementation = Problem.class)))
    public Response create(
            @Valid @NotNull(message = "request body is required") NoteRequest request,
            @Context SecurityContext securityContext,
            @Context UriInfo uriInfo) {
        NoteResponse note = noteService.create(currentUser.id(securityContext), request);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(Long.toString(note.id())).build())
                .entity(note)
                .build();
    }

    @GET
    @Operation(summary = "List own notes, newest updated first, optionally filtered by tags (FR-6, FR-7, FR-9)")
    @APIResponse(responseCode = "200", description = "Page envelope; page is 0-based", content = @Content(
            mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = NotePage.class)))
    @APIResponse(responseCode = "401", description = "Not signed in", content = @Content(
            mediaType = ProblemExceptionMappers.PROBLEM_JSON, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "422", description = "Invalid page or size; errors[] names the parameter",
            content = @Content(mediaType = ProblemExceptionMappers.PROBLEM_JSON,
                    schema = @Schema(implementation = Problem.class)))
    public NotePage list(@BeanParam @Valid NoteListQuery query, @Context SecurityContext securityContext) {
        return noteService.list(currentUser.id(securityContext), query.page, query.size, query.tag);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get one of the current learner's notes with rendered, sanitised bodyHtml (FR-5)")
    @APIResponse(responseCode = "200", description = "The note", content = @Content(
            mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = NoteResponse.class)))
    @APIResponse(responseCode = "401", description = "Not signed in", content = @Content(
            mediaType = ProblemExceptionMappers.PROBLEM_JSON, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "404", description = "Unknown id or not owned by the current learner (FR-9)",
            content = @Content(mediaType = ProblemExceptionMappers.PROBLEM_JSON,
                    schema = @Schema(implementation = Problem.class)))
    public NoteResponse get(@PathParam("id") long id, @Context SecurityContext securityContext) {
        return noteService.find(id, currentUser.id(securityContext)).orElseThrow(NotFoundException::new);
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Replace an own note's title, body, course week and tags; sets updatedAt (FR-3, FR-4, FR-5)")
    @APIResponse(responseCode = "200", description = "The updated note", content = @Content(
            mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = NoteResponse.class)))
    @APIResponse(responseCode = "400", description = "Malformed JSON", content = @Content(
            mediaType = ProblemExceptionMappers.PROBLEM_JSON, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "401", description = "Not signed in", content = @Content(
            mediaType = ProblemExceptionMappers.PROBLEM_JSON, schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "404", description = "Unknown id or not owned by the current learner (FR-9)",
            content = @Content(mediaType = ProblemExceptionMappers.PROBLEM_JSON,
                    schema = @Schema(implementation = Problem.class)))
    @APIResponse(responseCode = "422", description = "Validation failed; errors[] names each field",
            content = @Content(mediaType = ProblemExceptionMappers.PROBLEM_JSON,
                    schema = @Schema(implementation = Problem.class)))
    public NoteResponse update(
            @PathParam("id") long id,
            @Valid @NotNull(message = "request body is required") NoteRequest request,
            @Context SecurityContext securityContext) {
        return noteService.update(id, currentUser.id(securityContext), request).orElseThrow(NotFoundException::new);
    }
}
