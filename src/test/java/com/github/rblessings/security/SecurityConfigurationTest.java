package com.github.rblessings.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityConfigurationTest {

    @Autowired
    private WebTestClient webTestClient;

    private String tokenEndpoint;

    @BeforeEach
    void setUp() {
        tokenEndpoint = "/oauth2/token";  // Token endpoint for OAuth2 client credentials flow
    }

    @Test
    void shouldGenerateJwtTokenForValidClientCredentials() {
        // Given valid client credentials
        String clientId = "client";
        String clientSecret = "secret";

        // When client credentials are encoded for Basic Authentication
        String encodedCredentials = encodeClientCredentials(clientId, clientSecret);

        // Then request the token and assert the response
        webTestClient.post()
                .uri(tokenEndpoint)
                .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedCredentials)
                .bodyValue("grant_type=client_credentials&scope=openid")
                .exchange()
                .expectStatus().isOk()  // HTTP 200 OK
                .expectBody()
                .consumeWith(response -> {
                    String responseBody = new String(response.getResponseBodyContent());

                    // Validate token response structure
                    assertTokenResponse(responseBody);
                });
    }

    @Test
    void shouldReturnUnauthorizedForInvalidClientCredentials() {
        // Given invalid client credentials
        String invalidClientId = "invalidClient";
        String invalidClientSecret = "invalidSecret";

        // When requesting the token with invalid credentials
        webTestClient.post()
                .uri(tokenEndpoint)
                .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .bodyValue("grant_type=client_credentials&client_id=" + invalidClientId + "&client_secret=" + invalidClientSecret)
                .exchange()
                .expectStatus().isUnauthorized()  // HTTP 401 Unauthorized
                .expectBody()
                .consumeWith(response -> {
                    String responseBody = new String(response.getResponseBody());
                    // Assert that the response indicates an authentication error
                    assertThat(responseBody).contains("invalid_client");
                });
    }

    // Utility method to encode client credentials for Basic Authentication
    private String encodeClientCredentials(String clientId, String clientSecret) {
        String credentials = clientId + ":" + clientSecret;
        return Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    // Utility method to assert the structure of the JWT token response
    private void assertTokenResponse(String responseBody) {
        assertThat(responseBody).contains("\"access_token\":\"");
        assertThat(responseBody).contains("\"scope\":\"openid\"");
        assertThat(responseBody).contains("\"token_type\":\"Bearer\"");
        assertThat(responseBody).contains("\"expires_in\":");

        assertThat(responseBody).doesNotContain("\"access_token\":\"\"")
                .doesNotContain("\"expires_in\":\"\"");
    }
}
