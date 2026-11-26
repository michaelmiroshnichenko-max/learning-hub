package dev.learninghub.shared.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/** The string's UTF-8 encoding must not exceed the byte limit read from config key {@link #configKey()}. */
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = MaxUtf8BytesValidator.class)
public @interface MaxUtf8Bytes {

    String configKey();

    String message() default "is too large";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
