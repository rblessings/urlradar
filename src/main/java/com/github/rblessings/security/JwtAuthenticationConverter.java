package com.github.rblessings.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * This class converts a JWT token into a custom {@link JWTAuthentication} object.
 * <p>
 * The conversion process extracts necessary claims (e.g., "permissions") from the JWT
 * and maps them to a custom authentication object that can be used for authorization purposes.
 * <p>
 * In a real-world scenario, you'd typically derive the authorities (e.g., roles, permissions)
 * either directly from the access token (if they are managed at the authorization server level)
 * or dynamically from a database or other third-party system (if they are managed from a business perspective).
 * <p>
 * Authorities represent what the user can do in the system (e.g., "read", "write"), and they are crucial for
 * authorization decisions. The JWT may include a custom claim for these authorities, or they might need to be
 * queried based on information in the JWT (such as a user identifier).
 * <p>
 * For the permissions, they are currently being extracted from the JWT's custom claim ("permissions").
 * The authority values should also ideally be set in the authentication object based on this information,
 * enabling granular access control based on the user's roles and permissions.
 */
@Component
public class JwtAuthenticationConverter implements Converter<Jwt, JWTAuthentication> {

    @Override
    public JWTAuthentication convert(Jwt source) {
        // TODO: Dynamically query authorities (roles/permissions) for the user or client from the database
        //  or another service. In this example, we assume a static "read" authority for simplicity.
        List<GrantedAuthority> authorities = List.of(() -> "read");

        List<String> permissions = source.getClaim("permissions");

        return new JWTAuthentication(source, authorities, permissions);
    }
}


