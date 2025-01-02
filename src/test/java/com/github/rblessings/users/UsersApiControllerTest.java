package com.github.rblessings.users;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class UsersApiControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private UsersApiController usersApiController;

    @Mock
    private UserService userService;

    private UserRegistrationRequest validUserRequest;
    private UserDTO mockUserDTO;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(usersApiController).build();

        validUserRequest = new UserRegistrationRequest("John", "Doe", "john.doe@example.com", "password123");
        mockUserDTO = new UserDTO("1", "John", "Doe", "john.doe@example.com", "secret");
    }

    @Test
    public void testCreateUser_Success() throws Exception {
        // Arrange
        when(userService.registerUser(validUserRequest.firstName(), validUserRequest.lastName(), validUserRequest.email(), validUserRequest.password()))
                .thenReturn(mockUserDTO);

        URI location = ServletUriComponentsBuilder
                .fromPath("/api/v1/users/{id}")
                .buildAndExpand(mockUserDTO.id())
                .toUri();

        // Act and Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\": \"John\", \"lastName\": \"Doe\", \"email\": \"john.doe@example.com\", \"password\": \"password123\"}")
                )
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "http://localhost%s".formatted(location.getPath())))
                .andExpect(jsonPath("$.statusCode").value(HttpStatus.CREATED.value()))
                .andExpect(jsonPath("$.data.id").value(mockUserDTO.id()))
                .andExpect(jsonPath("$.data.firstName").value(mockUserDTO.firstName()))
                .andExpect(jsonPath("$.data.lastName").value(mockUserDTO.lastName()))
                .andExpect(jsonPath("$.data.email").value(mockUserDTO.email()));
    }

    @Test
    public void testGetUserById_Success() {
        // Arrange
        when(userService.findById("1")).thenReturn(Optional.of(mockUserDTO));

        ApiResponse<UserDTO> expectedResponse = ApiResponse.success(HttpStatus.OK.value(), mockUserDTO);

        // Act
        ResponseEntity<ApiResponse<UserDTO>> responseEntity = usersApiController.getUserById("1");

        // Assert
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isEqualTo(expectedResponse);
    }

    @Test
    public void testGetUserById_NotFound() {
        // Arrange
        when(userService.findById("2")).thenReturn(Optional.empty());

        ApiResponse<String> expectedResponse = ApiResponse.error(HttpStatus.NOT_FOUND.value(),
                "User with ID 2 not found");

        // Act
        ResponseEntity<ApiResponse<UserDTO>> responseEntity = usersApiController.getUserById("2");

        // Assert
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(responseEntity.getBody()).isEqualTo(expectedResponse);
    }
}
