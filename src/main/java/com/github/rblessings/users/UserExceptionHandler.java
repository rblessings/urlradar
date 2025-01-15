package com.github.rblessings.users;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

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
     * @return A standardized API response with the error message wrapped in a Mono.
     */
    @ExceptionHandler(EmailAlreadyInUseException.class)
    public Mono<ResponseEntity<ApiResponse<String>>> handleEmailAlreadyInUse(EmailAlreadyInUseException ex) {
        final var httpStatus = HttpStatus.BAD_REQUEST;
        ApiResponse<String> response = ApiResponse.error(httpStatus.value(), ex.getMessage());
        return Mono.just(new ResponseEntity<>(response, httpStatus));
    }

    /**
     * Handles all other exceptions and returns a generic error response.
     *
     * @param ex The exception that was thrown.
     * @return A standardized API response with a generic error message wrapped in a Mono.
     */
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ApiResponse<String>>> handleException(Exception ex) {
        final var httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        ApiResponse<String> response = ApiResponse.error(httpStatus.value(), ex.getMessage());
        return Mono.just(new ResponseEntity<>(response, httpStatus));
    }
}

