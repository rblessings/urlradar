package com.github.rblessings.users;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    private User user;

    @BeforeEach
    public void setUp() {
        // Prepare test data
        user = new User("1", "John", "Doe", "john.doe@example.com", "encodedPassword");
    }

    @Test
    public void testRegisterUser_EmailAlreadyInUse() {
        // Arrange
        String email = "john.doe@example.com";
        when(userRepository.findByEmail(email)).thenReturn(java.util.Optional.of(user));

        // Act & Assert
        EmailAlreadyInUseException exception = assertThrows(EmailAlreadyInUseException.class, () -> {
            userService.registerUser("John", "Doe", email, "password123");
        });

        assertEquals("The email address 'john.doe@example.com' is already in use.", exception.getMessage());

        verify(userRepository).findByEmail(email);
    }

    @Test
    public void testRegisterUser_Success() {
        // Arrange
        String email = "john.doe@example.com";
        when(userRepository.findByEmail(email)).thenReturn(java.util.Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        UserDTO result = userService.registerUser("John", "Doe", email, "password123");

        // Assert
        assertNotNull(result);
        assertEquals("john.doe@example.com", result.email());

        verify(userRepository).findByEmail(email);
        verify(userRepository).save(any(User.class));
    }
}
