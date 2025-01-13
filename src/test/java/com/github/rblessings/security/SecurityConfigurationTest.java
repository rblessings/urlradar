package com.github.rblessings.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.documentationConfiguration;

@ActiveProfiles({"dev"})
@ExtendWith({SpringExtension.class, RestDocumentationExtension.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class SecurityConfigurationTest {

    private static final String TOKEN_ENDPOINT = "/oauth2/token";
    private static final String PRINCIPAL_ENDPOINT = "/api/v1/users/principal";
    private static final String CLIENT_ID = "curl-client";
    private static final String CLIENT_SECRET = "secret";

    @Container
    static GenericContainer<?> AUTHORIZATION_SERVER_CONTAINER = new GenericContainer<>(
            "rblessings/oauth2-oidc-jwt-auth-server:latest")
            .withEnv("spring.profiles.active", "dev")
            .withExposedPorts(9000)

            // Check that the server is up and ready to receive requests
            .waitingFor(Wait.forListeningPort())
            .waitingFor(Wait.forHttp("/").forStatusCode(404)); // TODO check /actuator/health for 200-OK status

    @Container
    @ServiceConnection
    static GenericContainer<?> REDIS_CONTAINER = new GenericContainer<>(DockerImageName.parse("redis:latest"))
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    static void dynamicPropertySource(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> "http://localhost:%d".formatted(AUTHORIZATION_SERVER_CONTAINER.getMappedPort(9000)));

    }

    private WebTestClient webTestClient;

    @LocalServerPort
    private int localServerPort;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        this.webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:%d".formatted(localServerPort))
                .filter(documentationConfiguration(restDocumentation)
                        .operationPreprocessors()
                        .withRequestDefaults(
                                modifyHeaders()
                                        // Removing non-essential headers to streamline the request for documentation purposes
                                        .remove("accept-encoding")
                                        .remove("user-agent")
                                        .remove("accept"),
                                modifyUris()
                                        // Ensuring consistent and fixed base URI for documentation, overriding dynamic test ports
                                        .scheme("http")
                                        .host("localhost")
                                        .port(8080)  // Explicitly setting the port to 8080 for documentation clarity
                        )
                        .withResponseDefaults(prettyPrint())
                )
                .build();
    }

    @Test
    void shouldReturnPrincipalForValidToken() {
        final String token = getValidAccessToken();

        webTestClient.get()
                .uri(PRINCIPAL_ENDPOINT)
                .header(HttpHeaders.AUTHORIZATION, "Bearer %s".formatted(token))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(response -> {
                    assertThat(response).contains("\"authenticated\":true");
                    assertThat(response).contains("\"name\":\"curl-client\"");
                    assertThat(response).contains(
                            "\"authorities\":[{\"authority\":\"SCOPE_apis:read\"},{\"authority\":\"SCOPE_apis:write\"}]");
                })
                .consumeWith(document("users-get-current-principal"));
    }

    // Helper method to get a valid access token using the OAuth2 client credentials flow from the Authorization Server
    private String getValidAccessToken() {
        String encodedCredentials = encodeClientCredentialsToBase64(CLIENT_ID, CLIENT_SECRET);
        AtomicReference<String> accessToken = new AtomicReference<>();

        WebTestClient authorizationServerWebTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:%d".formatted(AUTHORIZATION_SERVER_CONTAINER.getMappedPort(9000))) // Dynamic port for the authorization server
                .build();

        authorizationServerWebTestClient.post()
                .uri(TOKEN_ENDPOINT)
                .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .header(HttpHeaders.AUTHORIZATION, "Basic %s".formatted(encodedCredentials))
                .bodyValue("grant_type=client_credentials&scope=apis:read apis:write")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> {
                    String jsonResponse = new String(Objects.requireNonNull(response.getResponseBody()));
                    try {
                        String token = new JSONObject(jsonResponse).getString("access_token");
                        accessToken.set(token);
                    } catch (JSONException e) {
                        throw new RuntimeException("Failed to parse token from response", e);
                    }
                });

        return accessToken.get();
    }

    private static String encodeClientCredentialsToBase64(String clientId, String clientSecret) {
        String credentials = "%s:%s".formatted(clientId, clientSecret);
        return Base64.getEncoder().encodeToString(credentials.getBytes());
    }
}
