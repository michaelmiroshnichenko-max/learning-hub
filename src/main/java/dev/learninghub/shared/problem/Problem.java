package dev.learninghub.shared.problem;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** RFC 9457 problem details body (architecture section 5). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Problem(String type, String title, int status, String detail, String instance, List<FieldError> errors) {

    public record FieldError(String field, String message) {
    }
}
