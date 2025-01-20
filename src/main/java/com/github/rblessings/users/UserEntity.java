package com.github.rblessings.users;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Objects;

/**
 * Represents a user in the system.
 * Immutability ensures thread-safety, and email is the unique identity key for comparisons.
 *
 * <p>Key design decisions:</p>
 * <ul>
 *     <li><strong>Immutability:</strong> Ensures predictable, thread-safe behavior.</li>
 *     <li><strong>Email as Identity:</strong> Uniqueness is based on email, minimizing errors in comparisons and storage.</li>
 *     <li><strong>Email is indexed:</strong> Indexing the email field provides efficient query performance for
 *     operations that frequently rely on email lookups, such as authentication, user retrieval, and validation.
 * </ul>
 *
 * <p>Password security is handled externally, but it is sensitive and should be treated securely.</p>
 *
 * @param id        The unique identifier for relational mapping.
 * @param firstName The user’s first name.
 * @param lastName  The user’s last name.
 * @param email     The user’s email, used as the identity key.
 * @param password  The user’s password (securely handled externally).
 * @param version   Used for optimistic locking on {@link UserEntity} entity.
 */
@Document(collection = "users")
public record UserEntity(
        @Id String id,
        String firstName,
        String lastName,
        @Indexed(unique = true) String email,
        String password,
        @Version Integer version) {

    /**
     * Equality is based solely on the email to ensure uniqueness and avoid issues with mutable fields.
     *
     * @param o The object to compare.
     * @return {@code true} if both users have the same email.
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        UserEntity user = (UserEntity) o;
        return Objects.equals(email, user.email);
    }

    /**
     * Hash code is derived from email for consistent behavior in hash-based collections.
     *
     * @return The user's email hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(email);
    }
}
