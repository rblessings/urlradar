package com.github.rblessings.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * This class represents a custom authentication token that holds the value of custom claims (e.g., permissions)
 * from the access token’s body. It extends {@link JwtAuthenticationToken} to provide additional application-specific
 * details that can be used for authorization purposes.
 * <p>
 * By adding these details directly to the authentication object in the security context, you can simplify authorization
 * configuration across your application. This makes it easier to apply these permissions either at the endpoint level
 * or method level, depending on your security configuration.
 * <p>
 * This class can be extended to include other custom claims or details as needed for more complex authorization logic.
 */
public class JWTAuthentication extends JwtAuthenticationToken {
    private final List<String> permissions;

    public JWTAuthentication(Jwt jwt, Collection<? extends GrantedAuthority> authorities, List<String> permissions) {
        super(jwt, authorities);
        this.permissions = Objects.requireNonNull(permissions, "permissions should not be null");
    }

    public List<String> getPermissions() {
        return permissions;
    }
}
