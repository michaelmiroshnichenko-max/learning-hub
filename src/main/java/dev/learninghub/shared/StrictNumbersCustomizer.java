package dev.learninghub.shared;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.inject.Singleton;

/** Numbers must be sent as JSON integers: no silent coercion of 2.5, "5" or true (S1.7). */
@Singleton
public class StrictNumbersCustomizer implements ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper objectMapper) {
        objectMapper.disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
        objectMapper.coercionConfigFor(LogicalType.Integer)
                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.String, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.EmptyString, CoercionAction.Fail);
    }
}
