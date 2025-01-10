package com.github.rblessings.security;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.operation.Operation;
import org.springframework.restdocs.snippet.TemplatedSnippet;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.documentationConfiguration;

@ActiveProfiles({"dev"})
@ExtendWith({SpringExtension.class, RestDocumentationExtension.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(SecurityConfigurationTest.TestApiController.class)
class SecurityConfigurationTest {

    private static final String TOKEN_ENDPOINT = "/oauth2/token";
    private static final String HELLO_ENDPOINT = "/api/v1/hello";
    private static final String PRINCIPAL_ENDPOINT = "/api/v1/users/principal";
    private static final String CLIENT_ID = "curl-client";
    private static final String CLIENT_SECRET = "secret";

    private WebTestClient webTestClient;

    @LocalServerPort
    private int port;

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

    @Test
    void shouldGenerateJwtTokenForValidClientCredentials() {
        String encodedCredentials = encodeClientCredentials(CLIENT_ID, CLIENT_SECRET);

        webTestClient.post()
                .uri(TOKEN_ENDPOINT)
                .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedCredentials)
                .bodyValue("grant_type=client_credentials&scope=apis:read apis:write")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> {
                    String responseBody = getResponseBodyContent(response);
                    assertTokenResponse(responseBody);
                })

                .consumeWith(document("users-generate-oauth2-jwt-token", new CustomOAuth2CurlSnippet()));
    }

    @Test
    void shouldReturnUnauthorizedForInvalidClientCredentials() {
        // Given: Invalid client credentials
        String invalidClientId = "invalidClient";
        String invalidClientSecret = "invalidSecret";

        // When: Requesting the token with invalid credentials
        webTestClient.post()
                .uri(TOKEN_ENDPOINT)
                .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .bodyValue("grant_type=client_credentials&client_id=" + invalidClientId + "&client_secret=" + invalidClientSecret)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .consumeWith(response -> {
                    // Then: should not be able to retrieve a token
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

    @Test
    void shouldReturnPrincipalForValidToken() {
        // Given: Get a valid access token using OAuth2 client credentials
        String token = getValidAccessToken();

        // When: Request the /api/v1/principal endpoint with the valid Bearer token
        webTestClient.get()
                .uri(PRINCIPAL_ENDPOINT)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(response -> {
                    // Check if the "authenticated" status is true
                    assertThat(response).contains("\"authenticated\":true");

                    // Assert the principal's name is as expected
                    assertThat(response).contains("\"name\":\"curl-client\"");

                    // Assert specific values in the authorities
                    assertThat(response).contains(
                            "\"authorities\":[{\"authority\":\"SCOPE_apis:read\"},{\"authority\":\"SCOPE_apis:write\"}]");
                })
                .consumeWith(document("users-get-current-principal"));
    }

    private static String getResponseBodyContent(EntityExchangeResult<byte[]> response) {
        return new String(Objects.requireNonNull(
                response.getResponseBodyContent(),
                "Response body is null. The OAuth2 token service may have failed to return a valid response.")
        );
    }

    // Utility method to encode client credentials for Basic Authentication
    private static String encodeClientCredentials(String clientId, String clientSecret) {
        String credentials = "%s:%s".formatted(clientId, clientSecret);
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
        assertThat(scope).isEqualTo("apis:read apis:write");
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
                .bodyValue("grant_type=client_credentials&scope=apis:read apis:write")
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

    /**
     * A test controller used to verify the OAuth 2 Resource Server configuration.
     * This controller exposes a simple endpoint that can be accessed by clients
     * with valid access tokens.
     */
    @RestController
    @RequestMapping(value = "/api/v1/hello")
    static class TestApiController {

        @GetMapping
        ResponseEntity<String> hello() {
            return ResponseEntity.ok("{\"message\":\"Hello, world!\"}");
        }
    }

    /**
     * CustomOAuth2CurlSnippet is a specialized TemplatedSnippet implementation
     * designed to generate a custom cURL command for OAuth2 token requests.
     * <p>
     * This snippet:
     * - Generates a `curl` command tailored to an OAuth2 token generation endpoint.
     * - Excludes headers like `Host` and `Content-Length` from the generated output.
     * - Handles basic authorization credentials by extracting the base64-encoded value.
     * - Dynamically includes the request body when present.
     * <p>
     * Usage:
     * Add this snippet to your Spring REST Docs documentation to provide developers
     * with clear, concise instructions for generating OAuth2 tokens via `curl`.
     */
    static class CustomOAuth2CurlSnippet extends TemplatedSnippet {

        // Constructor: Initializes the snippet with the template name and no additional attributes.
        CustomOAuth2CurlSnippet() {
            super("custom-oauth2-curl", Collections.emptyMap());
        }

        @Override
        protected Map<String, Object> createModel(Operation operation) {
            Map<String, Object> model = new HashMap<>();

            // Populate the URL and HTTP method for the cURL command
            model.put("url", operation.getRequest().getUri());
            model.put("method", operation.getRequest().getMethod().name());

            // Populate headers, excluding `Host`, `Content-Length`, and `Authorization`
            List<Map<String, String>> headers = operation.getRequest().getHeaders().entrySet().stream()
                    .filter(entry -> {
                        String headerName = entry.getKey().toLowerCase();
                        return !headerName.equals("content-length") && !headerName.equals("host") && !headerName.equals("authorization");
                    })
                    .map(entry -> Map.of("name", entry.getKey(), "value", String.join(",", entry.getValue())))
                    .collect(Collectors.toList());
            model.put("headers", headers);

            // Extract the base64-encoded authorization credentials (if present)
            String authorizationHeader = operation.getRequest().getHeaders().getFirst("Authorization");
            model.put("authorizationHeader",
                    authorizationHeader != null && authorizationHeader.startsWith("Basic ")
                            ? authorizationHeader.substring("Basic ".length())
                            : null);

            // Include the request body, or set it to `null` if empty
            byte[] requestBody = operation.getRequest().getContent();
            String requestBodyString = requestBody != null ? new String(requestBody, StandardCharsets.UTF_8) : "";
            model.put("requestBody", requestBodyString.isEmpty() ? null : requestBodyString);

            return model;
        }
    }
}
