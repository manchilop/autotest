package com.example.autotest_backend.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 *
 * <p>These pin down the contract the frontend relies on: which HTTP status each
 * kind of failure produces, and the shape of the JSON body.
 */
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("a broken business rule becomes 409 Conflict")
    void illegalStateBecomesConflict() {
        // Act
        ResponseEntity<Map<String, Object>> response =
                handler.handleIllegalState(new IllegalStateException("You are already a member"));

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("status", 409);
        assertThat(response.getBody()).containsEntry("message", "You are already a member");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    @Test
    @DisplayName("an invalid argument becomes 400 Bad Request")
    void illegalArgumentBecomesBadRequest() {
        // Act
        ResponseEntity<Map<String, Object>> response =
                handler.handleIllegalArgument(new IllegalArgumentException("Invalid invite code"));

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("status", 400);
        assertThat(response.getBody()).containsEntry("message", "Invalid invite code");
    }

    @Test
    @DisplayName("an unexpected exception becomes 500 without leaking its message")
    void genericExceptionBecomesServerError() {
        // Act
        ResponseEntity<Map<String, Object>> response =
                handler.handleGeneric(new RuntimeException("connection string with secrets"));

        // Assert: internal details must not reach the client
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("message", "Unexpected error");
        assertThat(response.getBody().get("message").toString())
                .doesNotContain("connection string with secrets");
    }

    @Test
    @DisplayName("a validation failure lists every rejected field")
    void validationFailureListsFields() {
        // Arrange
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "Email is required"));
        bindingResult.addError(new FieldError("request", "password", "Password is required"));

        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);

        // Act
        ResponseEntity<Map<String, Object>> response = handler.handleValidation(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("message").toString())
                .contains("email: Email is required")
                .contains("password: Password is required");
    }
}
