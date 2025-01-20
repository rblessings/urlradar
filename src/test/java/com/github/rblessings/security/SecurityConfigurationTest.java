package com.github.rblessings.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
    private static final int AUTH_SERVER_PORT = 9090;

    @Container
    static final FixedHostPortGenericContainer<?> AUTHORIZATION_SERVER_CONTAINER =
            new FixedHostPortGenericContainer<>("rblessings/oauth2-oidc-jwt-auth-server:latest")
                    .withFixedExposedPort(AUTH_SERVER_PORT, 9000)
                    .withEnv("SPRING_PROFILES_ACTIVE", "dev")
                    .withEnv("SPRING_SECURITY_OAUTH2_AUTHORIZATIONSERVER_ISSUER",
                            String.format("http://localhost:%d", AUTH_SERVER_PORT))
                    .waitingFor(Wait.forListeningPort())
                    .waitingFor(Wait.forHttp("/.well-known/openid-configuration").forStatusCode(200));

    @DynamicPropertySource
    static void dynamicPropertySource(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> String.format("http://localhost:%d", AUTH_SERVER_PORT));
    }

    private WebTestClient webTestClient;

    @LocalServerPort
    private int localServerPort;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        this.webTestClient = WebTestClient.bindToServer()
                .baseUrl(String.format("http://localhost:%d", localServerPort))
                .filter(documentationConfiguration(restDocumentation)
                        .operationPreprocessors()
                        .withRequestDefaults(
                                modifyHeaders()
                                        .remove("accept-encoding")
                                        .remove("user-agent")
                                        .remove("accept"),
                                modifyUris()
                                        .scheme("http")
                                        .host("localhost")
                                        .port(8080)
                        )
                        .withResponseDefaults(prettyPrint())
                )
                .build();
    }

    @Test
    void shouldReturnAuthenticatedPrincipalForValidToken() {
        final String token = obtainValidClientJwtAccessToken();

        webTestClient.get()
                .uri(PRINCIPAL_ENDPOINT)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.statusCode").isEqualTo(HttpStatus.OK.value())
                .jsonPath("$.data.authenticated").isEqualTo(true)
                .jsonPath("$.data.principal").isNotEmpty()
                .jsonPath("$.data.credentials.claims.scope").isEqualTo(Arrays.asList("apis:read", "apis:write"))
                .consumeWith(document("users-get-current-principal"));
    }

    private String obtainValidClientJwtAccessToken() {
        String encodedCredentials = encodeClientCredentialsToBase64(CLIENT_ID, CLIENT_SECRET);
        AtomicReference<String> accessToken = new AtomicReference<>();

        WebTestClient authorizationServerWebTestClient = WebTestClient.bindToServer()
                .baseUrl(String.format("http://localhost:%d", AUTH_SERVER_PORT))
                .build();

        authorizationServerWebTestClient.post()
                .uri(TOKEN_ENDPOINT)
                .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .header(HttpHeaders.AUTHORIZATION, String.format("Basic %s", encodedCredentials))
                .bodyValue("grant_type=client_credentials&scope=apis:read apis:write")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> {
                    String jsonResponse = new String(Objects.requireNonNull(response.getResponseBody()));
                    try {
                        String token = new JSONObject(jsonResponse).getString("access_token");
                        assertThat(token).isNotBlank()
                                .withFailMessage(() -> "The client JWT token could not be obtained from the authorization server.");

                        accessToken.set(token);
                    } catch (JSONException e) {
                        throw new RuntimeException("Failed to parse token from response: " + jsonResponse, e);
                    }
                });

        return accessToken.get();
    }

    /**
     * Encodes the given client credentials (clientId and clientSecret) into a Base64 encoded string.
     *
     * @param clientId     The client identifier.
     * @param clientSecret The client secret.
     * @return A Base64 encoded string representing the client credentials.
     */
    private static String encodeClientCredentialsToBase64(String clientId, String clientSecret) {
        String credentials = String.format("%s:%s", clientId, clientSecret);
        return Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
