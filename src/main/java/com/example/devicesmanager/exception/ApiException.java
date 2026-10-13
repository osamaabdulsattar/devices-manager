package com.example.devicesmanager.exception;

/**
 * Base class for expected, client-facing errors. The {@link ProblemType} decides the HTTP status and the problem
 * {@code type} URI; the message becomes the problem {@code detail}.
 */
public abstract class ApiException extends RuntimeException {

    private final ProblemType problemType;

    protected ApiException(ProblemType problemType, String detail) {
        super(detail);
        this.problemType = problemType;
    }

    public ProblemType getProblemType() {
        return problemType;
    }
}
