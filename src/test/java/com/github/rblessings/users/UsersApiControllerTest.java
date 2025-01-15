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
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.operation.preprocess.Preprocessors;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.when;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.documentationConfiguration;

@ExtendWith({MockitoExtension.class, RestDocumentationExtension.class})
public class UsersApiControllerTest {

    private WebTestClient webTestClient;

    @InjectMocks
    private UsersApiController usersApiController;

    @Mock
    private UserService userService;

    private UserRegistrationRequest validUserRequest;
    private UserDTO mockUserDTO;

    @BeforeEach
    public void setUp(RestDocumentationContextProvider restDocumentation) {
        webTestClient = WebTestClient.bindToController(usersApiController)
                .configureClient()
                .filter(documentationConfiguration(restDocumentation))
                .build();

        validUserRequest = new UserRegistrationRequest("John", "Doe", "john.doe@example.com", "password123");
        mockUserDTO = new UserDTO("1", "John", "Doe", "john.doe@example.com", "secret");
    }

    @Test
    public void testCreateUser_Success() {
        // Arrange
        when(userService.registerUser(
                validUserRequest.firstName(),
                validUserRequest.lastName(),
                validUserRequest.email(),
                validUserRequest.password())
        ).thenReturn(Mono.just(mockUserDTO));

        // Act & Assert
        webTestClient.post()
                .uri("/api/v1/users")
                .header("Authorization", "Bearer <dummy-jwt-token>")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validUserRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueEquals(HttpHeaders.LOCATION, String.format("/api/v1/users/%s", mockUserDTO.id()))
                .expectBody()
                .jsonPath("$.statusCode").isEqualTo(HttpStatus.CREATED.value())
                .jsonPath("$.data.id").isEqualTo(mockUserDTO.id())
                .jsonPath("$.data.firstName").isEqualTo(mockUserDTO.firstName())
                .jsonPath("$.data.lastName").isEqualTo(mockUserDTO.lastName())
                .jsonPath("$.data.email").isEqualTo(mockUserDTO.email())
                .consumeWith(document("users-create-account", preprocessRequest(Preprocessors.prettyPrint())));
    }

    @Test
    public void testGetUserById_Success() {
        // Arrange
        when(userService.findById("1")).thenReturn(Mono.just(mockUserDTO));

        // Act & Assert
        webTestClient.get()
                .uri("/api/v1/users/{id}", "1")
                .header("Authorization", "Bearer <your-jwt-token>")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.statusCode").isEqualTo(HttpStatus.OK.value())
                .jsonPath("$.data.id").isEqualTo(mockUserDTO.id())
                .jsonPath("$.data.firstName").isEqualTo(mockUserDTO.firstName())
                .jsonPath("$.data.lastName").isEqualTo(mockUserDTO.lastName())
                .jsonPath("$.data.email").isEqualTo(mockUserDTO.email())
                .consumeWith(document("users-get-user-by-id", preprocessRequest(Preprocessors.prettyPrint())));
    }

    @Test
    public void testGetUserById_NotFound() {
        // Arrange
        when(userService.findById("2")).thenReturn(Mono.empty());

        // Act & Assert
        webTestClient.get()
                .uri("/api/v1/users/{id}", "2")
                .header("Authorization", "Bearer <your-jwt-token>")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.statusCode").isEqualTo(HttpStatus.NOT_FOUND.value())
                .jsonPath("$.message").isEqualTo("User with ID 2 not found");
    }
}