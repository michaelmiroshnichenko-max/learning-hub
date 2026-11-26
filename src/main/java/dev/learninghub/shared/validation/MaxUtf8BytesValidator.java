package dev.learninghub.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;
import org.eclipse.microprofile.config.ConfigProvider;

public class MaxUtf8BytesValidator implements ConstraintValidator<MaxUtf8Bytes, String> {

    private int maxBytes;
    private String message;

    @Override
    public void initialize(MaxUtf8Bytes annotation) {
        maxBytes = ConfigProvider.getConfig().getValue(annotation.configKey(), Integer.class);
        message = maxBytes % 1024 == 0
                ? "must be at most " + maxBytes / 1024 + " KB (" + maxBytes + " bytes in UTF-8)"
                : "must be at most " + maxBytes + " bytes in UTF-8";
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.getBytes(StandardCharsets.UTF_8).length <= maxBytes) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}
