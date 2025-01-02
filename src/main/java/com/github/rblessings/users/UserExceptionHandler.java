package com.github.rblessings.users;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for all user-related exceptions thrown by REST controllers.
 * <p>
 * This class ensures that all errors are consistently formatted using the {@link ApiResponse}.
 * </p>
 */
@RestControllerAdvice
public class UserExceptionHandler {

    /**
     * Handles the {@link EmailAlreadyInUseException} exception and returns a standardized response.
     *
     * @param ex The exception that was thrown.
     * @return A standardized API response with the error message.
     */
    @ExceptionHandler(EmailAlreadyInUseException.class)
    public ResponseEntity<ApiResponse<String>> handleEmailAlreadyInUse(EmailAlreadyInUseException ex) {
        ApiResponse<String> response = ApiResponse.error(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles all other exceptions and returns a generic error response.
     *
     * @param ex The exception that was thrown.
     * @return A standardized API response with a generic error message.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleException(Exception ex) {
        ApiResponse<String> response = ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

