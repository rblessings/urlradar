package com.github.rblessings.users;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    private UserEntity user;

    @BeforeEach
    public void setUp() {
        // Initialize user entity for testing
        user = new UserEntity("1", "John", "Doe", "john.doe@example.com", "encodedPassword");
    }

    @Test
    public void testRegisterUser_EmailAlreadyInUse() {
        // Arrange
        String email = "john.doe@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Mono.just(user));

        // Act & Assert
        Mono<UserDTO> result = userService.registerUser("John", "Doe", email, "password123");

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof EmailAlreadyInUseException
                        && throwable.getMessage()
                        .equals(String.format("The email address '%s' is already in use.", email)))
                .verify();

        verify(userRepository).findByEmail(email);
    }

    @Test
    public void testRegisterUser_SuccessfullyRegistersNewUser() {
        // Arrange
        String email = "john.doe@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Mono.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(Mono.just(user));

        // Act
        Mono<UserDTO> result = userService.registerUser("John", "Doe", email, "password123");

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(userDTO -> userDTO.email().equals("john.doe@example.com"))
                .expectComplete()
                .verify();

        verify(userRepository).findByEmail(email);
        verify(userRepository).save(argThat(userEntity -> userEntity.email().equals(email)));
    }
}
