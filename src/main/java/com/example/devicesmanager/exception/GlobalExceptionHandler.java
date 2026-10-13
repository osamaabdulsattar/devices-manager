package com.example.devicesmanager.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

/**
 * Renders every error as an RFC 9457 Problem Details document ({@code application/problem+json}).
 * <p>
 * Extending {@link ResponseEntityExceptionHandler} lets Spring MVC's own exceptions (unsupported method or media
 * type, unknown path, missing parameters, ...) keep their proper 4xx status instead of falling through to the
 * catch-all 500 handler.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApiException(ApiException ex) {
        log.warn("{}: {}", ex.getProblemType(), ex.getMessage());
        return toResponse(problem(ex.getProblemType(), ex.getMessage()));
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ProblemDetail> handleInvalidSortProperty(PropertyReferenceException ex) {
        log.warn("Invalid sort property: {}", ex.getPropertyName());
        String detail = String.format("Invalid sort property '%s'", ex.getPropertyName());
        return toResponse(problem(ProblemType.INVALID_PARAMETER, detail));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLocking(ObjectOptimisticLockingFailureException ex) {
        log.warn("Optimistic locking failure: {}", ex.getMessage());
        return toResponse(problem(ProblemType.CONCURRENT_MODIFICATION,
                "Resource was modified concurrently, please retry"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        // Usually a race the service-level checks could not catch (e.g. two concurrent creates with the same
        // brand name). Don't leak database details to the client.
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return toResponse(problem(ProblemType.DATA_CONFLICT, "Request conflicts with existing data"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("Unhandled exception: ", ex);
        return toResponse(problem(ProblemType.INTERNAL_ERROR, "An unexpected error occurred"));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        log.warn("Validation failed: {}", ex.getMessage());
        List<FieldViolation> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> new FieldViolation(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();

        ProblemDetail problem = problem(ProblemType.VALIDATION_FAILED, "Request body contains invalid fields");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        log.warn("Malformed request body: {}", ex.getMessage());
        String detail = "Malformed request payload";
        Throwable cause = ex.getCause();
        Throwable rootCause = ex.getRootCause();
        if (cause instanceof IllegalArgumentException) {
            detail = cause.getMessage();
        } else if (rootCause != null && rootCause.getMessage() != null) {
            detail = rootCause.getMessage();
        }
        return handleExceptionInternal(ex, problem(ProblemType.MALFORMED_REQUEST, detail), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        log.warn("Parameter type mismatch: {} = {}", ex.getPropertyName(), ex.getValue());
        String detail = String.format("Invalid value '%s' for parameter '%s'", ex.getValue(), ex.getPropertyName());
        return handleExceptionInternal(ex, problem(ProblemType.INVALID_PARAMETER, detail), headers, status, request);
    }

    private static ProblemDetail problem(ProblemType type, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(type.status(), detail);
        problem.setType(type.uri());
        problem.setTitle(type.title());
        return problem;
    }

    private static ResponseEntity<ProblemDetail> toResponse(ProblemDetail problem) {
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }
}
