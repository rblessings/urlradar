package com.github.rblessings.security;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(SecurityConfigurationTest.TestApiController.class)
class SecurityConfigurationTest {

    private static final String TOKEN_ENDPOINT = "/oauth2/token";
    private static final String HELLO_ENDPOINT = "/api/v1/hello";
    private static final String CLIENT_ID = "client";
    private static final String CLIENT_SECRET = "secret";

    private final WebTestClient webTestClient;

    @Autowired
    public SecurityConfigurationTest(WebTestClient webTestClient) {
        this.webTestClient = webTestClient;
    }

    @BeforeEach
    void setUp() {
        // Any setup required for each test
    }

    @Test
    void shouldGenerateJwtTokenForValidClientCredentials() {
        // Given valid client credentials
        String encodedCredentials = encodeClientCredentials(CLIENT_ID, CLIENT_SECRET);

        // When requesting the token
        webTestClient.post()
                .uri(TOKEN_ENDPOINT)
                .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedCredentials)
                .bodyValue("grant_type=client_credentials&scope=apis")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> {
                    String responseBody = getResponseBodyContent(response);
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
                .uri(TOKEN_ENDPOINT)
                .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .bodyValue("grant_type=client_credentials&client_id=" + invalidClientId + "&client_secret=" + invalidClientSecret)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .consumeWith(response -> {
                    String responseBody = getResponseBodyContent(response);
                    assertThat(responseBody).contains("invalid_client");
                });
    }

    @Test
    void shouldReturnHelloMessageForValidToken() {
        // Given: Get a valid access token using OAuth2 client credentials
        String token = getValidAccessToken();

        // When: Request the /api/v1/hello endpoint with the valid Bearer token
        webTestClient.get()
                .uri(HELLO_ENDPOINT)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(response -> {
                    // Then: Assert the response body contains the expected message
                    assertThat(response).contains("{\"message\":\"Hello, world!\"}");
                });
    }

    @Test
    void shouldReturnUnauthorizedForInvalidToken() {
        // Given: Use an invalid token (a random or malformed token)
        String invalidToken = "invalid_token";  // This token is intentionally invalid

        // When: Request the /api/v1/hello endpoint with the invalid Bearer token
        webTestClient.get()
                .uri(HELLO_ENDPOINT)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidToken)
                .exchange()
                .expectStatus().isUnauthorized()  // Expect HTTP 401 Unauthorized
                .expectHeader()
                .value("WWW-Authenticate", message ->
                        assertThat(message).contains("Bearer error=\"invalid_token\"")
                );
    }

    private static String getResponseBodyContent(EntityExchangeResult<byte[]> response) {
        return new String(Objects.requireNonNull(
                response.getResponseBodyContent(),
                "Response body is null. The OAuth2 token service may have failed to return a valid response.")
        );
    }

    // Utility method to encode client credentials for Basic Authentication
    private String encodeClientCredentials(String clientId, String clientSecret) {
        String credentials = clientId + ":" + clientSecret;
        return Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    // Utility method to assert the structure of the JWT token response
    private void assertTokenResponse(String responseBody) {
        // Use JsonPath to extract values
        String accessToken = JsonPath.read(responseBody, "$.access_token");
        String scope = JsonPath.read(responseBody, "$.scope");
        String tokenType = JsonPath.read(responseBody, "$.token_type");
        Integer expiresIn = JsonPath.read(responseBody, "$.expires_in");

        // Perform assertions
        assertThat(accessToken).isNotNull();
        assertThat(accessToken).isNotEmpty();
        assertThat(scope).isEqualTo("apis");
        assertThat(tokenType).isEqualTo("Bearer");
        assertThat(expiresIn).isNotNull();
    }

    // Helper method to get a valid access token using the OAuth2 client credentials flow
    private String getValidAccessToken() {
        String encodedCredentials = encodeClientCredentials(CLIENT_ID, CLIENT_SECRET);
        AtomicReference<String> accessToken = new AtomicReference<>();

        webTestClient.post()
                .uri(TOKEN_ENDPOINT)
                .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedCredentials)
                .bodyValue("grant_type=client_credentials&scope=apis")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> {
                    String jsonResponse = new String(Objects.requireNonNull(response.getResponseBody()));
                    try {
                        // Extract the access token from the response
                        String token = new JSONObject(jsonResponse).getString("access_token");
                        accessToken.set(token);
                    } catch (JSONException e) {
                        throw new RuntimeException("Failed to parse token from response", e);
                    }
                });

        return accessToken.get();
    }

    // Endpoint to test the OAuth 2 resource server configuration
    @RestController
    @RequestMapping(value = "/api/v1/hello")
    static class TestApiController {

        @GetMapping
        ResponseEntity<String> hello() {
            return ResponseEntity.ok("{\"message\":\"Hello, world!\"}");
        }
    }
}

