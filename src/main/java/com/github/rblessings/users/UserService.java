package com.github.rblessings.users;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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
     * Checks if the provided email is already in use. If so, throws an {@link EmailAlreadyInUseException}.
     * Otherwise, the user's details are saved after encoding the password, and a {@link UserDTO} is returned.
     * </p>
     *
     * @param firstName The user's first name.
     * @param lastName  The user's last name.
     * @param email     The user's email address. Must be unique.
     * @param password  The user's raw password, which will be encoded before saving.
     * @return A {@link UserDTO} representing the registered user.
     * @throws EmailAlreadyInUseException if the email is already registered.
     */
    @Transactional
    public UserDTO registerUser(String firstName, String lastName, String email, String password) {
        userRepository.findByEmail(email)
                .ifPresent(user -> {
                    throw new EmailAlreadyInUseException(user.email());
                });

        String encodedPassword = passwordEncoder.encode(password);

        User user = new User(null, firstName, lastName, email, encodedPassword);
        return UserDTO.from(userRepository.save(user));
    }

    /**
     * Retrieves a user by their email.
     * <p>
     * Searches for a user using the provided email. If found, returns the corresponding {@link UserDTO}.
     * If not found, returns {@link Optional#empty()}.
     * </p>
     *
     * @param email The email address of the user.
     * @return An {@link Optional} containing the {@link UserDTO} if found, {@link Optional#empty()} otherwise.
     */
    public Optional<UserDTO> findByEmail(String email) {
        return userRepository.findByEmail(email).map(UserDTO::from);
    }

    /**
     * Retrieves a user by their ID.
     * <p>
     * Searches for a user using the provided ID. If found, returns the corresponding {@link UserDTO}.
     * If not found, returns {@link Optional#empty()}.
     * </p>
     *
     * @param id The unique ID of the user.
     * @return An {@link Optional} containing the {@link UserDTO} if found, {@link Optional#empty()} otherwise.
     */
    public Optional<UserDTO> findById(String id) {
        return userRepository.findById(id).map(UserDTO::from);
    }
}
