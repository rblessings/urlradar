package com.github.rblessings.security;

import com.github.rblessings.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

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
@Import(TestcontainersConfiguration.class)
class SecurityConfigurationTest {

    private static final String AUTH_SERVER_BASE_URL = "http://localhost:9000";
    private static final String TOKEN_ENDPOINT = "/oauth2/token";
    private static final String PRINCIPAL_ENDPOINT = "/api/v1/users/principal";
    private static final String CLIENT_ID = "curl-client";
    private static final String CLIENT_SECRET = "secret";

    private WebTestClient webTestClient;
    private final Integer authorizationServerContainerPort;

    @LocalServerPort
    private int port;

    @Autowired
    SecurityConfigurationTest(Integer authorizationServerContainerPort) {
        this.authorizationServerContainerPort = authorizationServerContainerPort;
    }

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        this.webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:%d".formatted(port))
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

    // Test the OAuth2 Resource Server configuration to ensure the protected resource can be accessed with a valid JWT token
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
                .baseUrl("http://localhost:%d".formatted(authorizationServerContainerPort)) // Dynamic port for the authorization server
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
