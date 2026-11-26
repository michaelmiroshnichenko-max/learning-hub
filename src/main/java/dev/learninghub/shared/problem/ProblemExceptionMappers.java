package dev.learninghub.shared.problem;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.Comparator;
import java.util.List;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/**
 * Maps every error to RFC 9457 problem details (architecture section 5, ADR 0006). Bodies never contain
 * submitted values, exception messages or stack traces (NFR-9).
 */
public class ProblemExceptionMappers {

    public static final String PROBLEM_JSON = "application/problem+json";

    private static final Logger LOG = Logger.getLogger(ProblemExceptionMappers.class);
    private static final int UNPROCESSABLE_CONTENT = 422;

    private final ObjectMapper objectMapper;

    public ProblemExceptionMappers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @ServerExceptionMapper
    public Response validation(ConstraintViolationException e, UriInfo uriInfo) {
        List<Problem.FieldError> errors = e.getConstraintViolations().stream()
                .map(v -> new Problem.FieldError(jsonField(v), v.getMessage()))
                .sorted(Comparator.comparing(Problem.FieldError::field, Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();
        return validationProblem(errors, uriInfo);
    }

    @ServerExceptionMapper
    public Response mismatchedInput(MismatchedInputException e, UriInfo uriInfo) {
        return wrongType(e, uriInfo);
    }

    @ServerExceptionMapper
    public Response invalidFormat(InvalidFormatException e, UriInfo uriInfo) {
        return wrongType(e, uriInfo);
    }

    @ServerExceptionMapper
    public Response jsonMapping(JsonMappingException e, UriInfo uriInfo) {
        return wrongType(e, uriInfo);
    }

    @ServerExceptionMapper
    public Response invalidDefinition(InvalidDefinitionException e, UriInfo uriInfo) {
        return unexpected(e, uriInfo);
    }

    @ServerExceptionMapper
    public Response malformedJson(JsonProcessingException e, UriInfo uriInfo) {
        return problem(new Problem("/problems/malformed-request", "Malformed request", 400,
                "The request body is not valid JSON.", uriInfo.getPath(), null));
    }

    @ServerExceptionMapper
    public Response notFound(NotFoundException e, UriInfo uriInfo) {
        return problem(new Problem("/problems/not-found", "Not found", 404, null, uriInfo.getPath(), null));
    }

    @ServerExceptionMapper
    public Response conflict(ConflictException e, UriInfo uriInfo) {
        return problem(new Problem("/problems/conflict", "Conflict", 409, e.getMessage(), uriInfo.getPath(), null));
    }

    @ServerExceptionMapper
    public Response webApplication(WebApplicationException e, UriInfo uriInfo) {
        // Quarkus REST wraps some JSON read errors (e.g. a body that does not start as JSON) in a 400.
        for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
            if (cause instanceof JsonProcessingException json) {
                return switch (json) {
                    case InvalidDefinitionException definition -> unexpected(definition, uriInfo);
                    case JsonMappingException mapping -> wrongType(mapping, uriInfo);
                    default -> malformedJson(json, uriInfo);
                };
            }
        }
        Response.StatusType status = e.getResponse().getStatusInfo();
        return problem(new Problem("about:blank", status.getReasonPhrase(), status.getStatusCode(), null,
                uriInfo.getPath(), null));
    }

    @ServerExceptionMapper
    public Response unexpected(Throwable e, UriInfo uriInfo) {
        // Log the type only: messages may contain submitted values (NFR-9).
        LOG.errorf("Unexpected %s on %s", e.getClass().getName(), uriInfo.getPath());
        return problem(new Problem("about:blank", "Internal Server Error", 500,
                "An unexpected error occurred.", uriInfo.getPath(), null));
    }

    private Response wrongType(JsonMappingException e, UriInfo uriInfo) {
        String field = jsonField(e.getPath());
        if (field == null) {
            return malformedJson(e, uriInfo);
        }
        return validationProblem(List.of(new Problem.FieldError(field, "has the wrong type")), uriInfo);
    }

    private Response validationProblem(List<Problem.FieldError> errors, UriInfo uriInfo) {
        String detail = errors.size() == 1 ? "1 field is invalid" : errors.size() + " fields are invalid";
        return problem(new Problem("/problems/validation", "Validation failed", UNPROCESSABLE_CONTENT, detail,
                uriInfo.getPath(), errors));
    }

    private Response problem(Problem problem) {
        try {
            return Response.status(problem.status())
                    .type(PROBLEM_JSON)
                    .entity(objectMapper.writeValueAsString(problem))
                    .build();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialise problem details", e);
        }
    }

    /** JSON property path of a violation, e.g. {@code title}; method and parameter nodes are skipped. */
    private static String jsonField(ConstraintViolation<?> violation) {
        StringBuilder field = new StringBuilder();
        for (Path.Node node : violation.getPropertyPath()) {
            if (node.getKind() == ElementKind.METHOD || node.getKind() == ElementKind.PARAMETER
                    || node.getKind() == ElementKind.CROSS_PARAMETER || node.getKind() == ElementKind.RETURN_VALUE) {
                continue;
            }
            if (node.getName() != null) {
                if (!field.isEmpty()) {
                    field.append('.');
                }
                field.append(node.getName());
            }
            if (node.getIndex() != null) {
                field.append('[').append(node.getIndex()).append(']');
            }
        }
        return field.isEmpty() ? null : field.toString();
    }

    private static String jsonField(List<JsonMappingException.Reference> path) {
        StringBuilder field = new StringBuilder();
        for (JsonMappingException.Reference ref : path) {
            if (ref.getFieldName() != null) {
                if (!field.isEmpty()) {
                    field.append('.');
                }
                field.append(ref.getFieldName());
            } else if (ref.getIndex() >= 0) {
                field.append('[').append(ref.getIndex()).append(']');
            }
        }
        return field.isEmpty() ? null : field.toString();
    }
}
