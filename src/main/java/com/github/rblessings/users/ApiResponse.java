package com.github.rblessings.users;

import java.util.HashMap;
import java.util.Objects;

/**
 * A standardized wrapper for API responses.
 * <p>
 * Provides a consistent response structure for all API responses, encapsulating:
 * <ul>
 *   <li><strong>message</strong>: A human-readable message indicating the result of the operation. (mostly used in error situations)</li>
 *   <li><strong>statusCode</strong>: The HTTP status code associated with the response.</li>
 *   <li><strong>data</strong>: The actual data of the response, which can be any type (e.g., the created user data).</li>
 * </ul>
 * <p>
 * This class is immutable and thread-safe, ensuring consistency in multithreaded environments.
 * </p>
 */
public final class ApiResponse<T> {
    private final int statusCode;
    private final String message;
    private final T data;

    private ApiResponse(int statusCode, String message, T data) {
        if (statusCode < 100 || statusCode > 599) {
            throw new IllegalArgumentException(String.format("Invalid HTTP status code: %d", statusCode));
        }
        this.statusCode = statusCode;
        this.message = message;
        this.data = data;
    }

    /**
     * Returns the HTTP status code of the response.
     *
     * @return the HTTP status code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the message associated with the response.
     *
     * @return a human-readable message or {@code null} if none is provided
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the data of the response.
     *
     * @return the response data or {@code null} if none is provided
     */
    public T getData() {
        return data;
    }

    /**
     * Creates a success response with the provided HTTP status code and data.
     *
     * @param statusCode the HTTP status code
     * @param data       the response data
     * @param <T>        the type of the response data
     * @return a new {@code ApiResponse} instance representing a success response
     */
    public static <T> ApiResponse<T> success(int statusCode, T data) {
        if (data == null) {
            throw new IllegalArgumentException("Success response data cannot be null");
        }
        return new ApiResponse<>(statusCode, null, data);
    }

    /**
     * Creates an error response with the provided HTTP status code and message.
     *
     * @param statusCode the HTTP status code
     * @param message    a human-readable error message
     * @param <T>        the type of the response data (use {@code Void} if no data is required)
     * @return a new {@code ApiResponse} instance representing an error response
     */
    public static <T> ApiResponse<T> error(int statusCode, String message) {
        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException("Error response message cannot be null or empty");
        }
        return new ApiResponse<>(statusCode, message, null);
    }

    /**
     * Defines equality based on {@code statusCode}, {@code message}, and {@code data}.
     * Ensures logical equivalence when used in collections or other frameworks requiring consistency.
     *
     * @param o the object to compare with
     * @return {@code true} if logically equivalent, {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ApiResponse<?> that = (ApiResponse<?>) o;
        return statusCode == that.statusCode &&
                Objects.equals(message, that.message) &&
                Objects.equals(data, that.data);
    }

    /**
     * Returns a hash code consistent with {@code equals}.
     * Necessary for proper behavior in hash-based collections like {@link HashMap}.
     *
     * @return the hash code of this {@code ApiResponse}
     */
    @Override
    public int hashCode() {
        return Objects.hash(statusCode, message, data);
    }

    /**
     * Provides a string representation for debugging.
     * Useful for inspecting key fields, but may be verbose for complex {@code data}.
     *
     * @return a string representation of this {@code ApiResponse}
     */
    @Override
    public String toString() {
        return "ApiResponse{" +
                "statusCode=" + statusCode +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}

