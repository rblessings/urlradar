package com.github.rblessings.security;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This REST controller exposes an endpoint to retrieve the current authenticated principal
 * from the security context. The principal typically represents the currently authenticated
 * user or client, and the `Authentication` object contains information about the user, including
 * authorities, credentials, and custom claims (e.g., permissions).
 * <p>
 * This can be useful for debugging, user profile management, or providing application-specific
 * information about the authenticated user.
 */
@RestController
@RequestMapping(value = "/api/v1/principal")
public class PrincipalRestController {

    /**
     * This endpoint returns the current authenticated principal in the form of an {@link Authentication} object.
     * The {@link Authentication} object contains details such as the username, authorities (roles/permissions),
     * and any custom claims (e.g., "permissions" or user-specific data) from the JWT or security context.
     *
     * @param authentication The current {@link Authentication} object from the security context,
     *                       which is automatically injected by Spring Security.
     * @return A {@link ResponseEntity} containing the {@link Authentication} object.
     */
    @GetMapping
    public ResponseEntity<Authentication> getPrincipal(Authentication authentication) {
        return ResponseEntity.ok(authentication);
    }
}

