package com.github.rblessings.users;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
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
                        && throwable.getMessage().equals("The email address 'john.doe@example.com' is already in use."))
                .verify();

        verify(userRepository).findByEmail(email);
    }

    @Test
    public void testRegisterUser_Success() {
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
        verify(userRepository).save(any(UserEntity.class));
    }
}
