package com.github.rblessings.users;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new user.
     * <p>
     * Checks if the provided email is already in use. If so, emits an error signal with {@link EmailAlreadyInUseException}.
     * Otherwise, the user's details are saved after encoding the password, and a {@link UserDTO} is emitted.
     * </p>
     *
     * @param firstName The user's first name.
     * @param lastName  The user's last name.
     * @param email     The user's email address. Must be unique.
     * @param password  The user's raw password, which will be encoded before saving.
     * @return A {@link Mono} emitting the {@link UserDTO} representing the registered user.
     */
    public Mono<UserDTO> registerUser(String firstName, String lastName, String email, String password) {
        return userRepository.findByEmail(email)
                .flatMap(existingUser -> Mono.<UserDTO>error(new EmailAlreadyInUseException(existingUser.email())))
                .switchIfEmpty(Mono.defer(() -> {
                    final String encodedPassword = passwordEncoder.encode(password);
                    UserEntity user = new UserEntity(null, firstName, lastName, email, encodedPassword);
                    return userRepository.save(user).map(UserDTO::from);
                }));
    }

    /**
     * Retrieves a user by their email.
     * <p>
     * Searches for a user using the provided email. If found, emits the corresponding {@link UserDTO}.
     * If not found, emits an empty signal.
     * </p>
     *
     * @param email The email address of the user.
     * @return A {@link Mono} emitting the {@link UserDTO} if found, or an empty signal if not.
     */
    public Mono<UserDTO> findByEmail(String email) {
        return userRepository.findByEmail(email).map(UserDTO::from);
    }

    /**
     * Retrieves a user by their ID.
     * <p>
     * Searches for a user using the provided ID. If found, emits the corresponding {@link UserDTO}.
     * If not found, emits an empty signal.
     * </p>
     *
     * @param id The unique ID of the user.
     * @return A {@link Mono} emitting the {@link UserDTO} if found, or an empty signal if not.
     */
    public Mono<UserDTO> findById(String id) {
        return userRepository.findById(id).map(UserDTO::from);
    }
}

