package dev.learninghub.shared.problem;

/** The request conflicts with the current state of the resource; mapped to 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String detail) {
        super(detail);
    }
}
