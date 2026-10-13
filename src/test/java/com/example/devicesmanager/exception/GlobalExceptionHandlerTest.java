package com.example.devicesmanager.exception;

import com.example.devicesmanager.model.Device;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleApiExceptionUsesProblemTypeForStatusTypeAndTitle() {
        ResponseEntity<ProblemDetail> response =
                handler.handleApiException(new DeviceInUseException("Device cannot be deleted while it is in use"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ProblemDetail problem = response.getBody();
        assertThat(problem).isNotNull();
        assertThat(problem.getType()).isEqualTo(URI.create("/problems/device-in-use"));
        assertThat(problem.getTitle()).isEqualTo("Device is in use");
        assertThat(problem.getDetail()).isEqualTo("Device cannot be deleted while it is in use");
    }

    @Test
    void handleOptimisticLockingReturnsConflict() {
        ObjectOptimisticLockingFailureException ex =
                new ObjectOptimisticLockingFailureException(Device.class, UUID.randomUUID());

        ResponseEntity<ProblemDetail> response = handler.handleOptimisticLocking(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getType()).isEqualTo(ProblemType.CONCURRENT_MODIFICATION.uri());
    }

    @Test
    void handleDataIntegrityViolationReturnsConflictWithoutLeakingDetails() {
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("duplicate key value violates unique constraint \"brands_name_key\"");

        ResponseEntity<ProblemDetail> response = handler.handleDataIntegrityViolation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).doesNotContain("brands_name_key");
    }

    @Test
    void handleUnexpectedReturns500WithoutLeakingDetails() {
        ResponseEntity<ProblemDetail> response = handler.handleUnexpected(new IllegalStateException("secret internals"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).doesNotContain("secret internals");
    }
}
