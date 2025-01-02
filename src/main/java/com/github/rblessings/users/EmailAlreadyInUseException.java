package com.github.rblessings.users;

/**
 * Exception thrown when attempting to register or use an email that is already in use.
 * <p>
 * This exception is thrown during user registration or email update processes if the email
 * is already associated with an existing user in the system. It ensures that each user has a
 * unique email address.
 * </p>
 * <p>
 * Example scenario:
 * <pre>
 *   if (userService.isEmailTaken(newUser.getEmail())) {
 *       throw new EmailAlreadyInUseException(newUser.getEmail());
 *   }
 * </pre>
 * </p>
 */
public final class EmailAlreadyInUseException extends RuntimeException {

    /**
     * Constructs a new exception with a detailed message indicating the email is already in use.
     *
     * @param email The email address that is already in use.
     */
    public EmailAlreadyInUseException(String email) {
        super(String.format("The email address '%s' is already in use.", email));
    }

    /**
     * Constructs a new exception with a detailed message and the specified cause.
     *
     * @param email The email address that is already in use.
     * @param cause The cause of the exception, if any.
     */
    public EmailAlreadyInUseException(String email, Throwable cause) {
        super(String.format("The email address '%s' is already in use.", email), cause);
    }
}
