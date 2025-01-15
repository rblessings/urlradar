package com.github.rblessings.users;

import java.util.Objects;

public record UserDTO(
        String id,
        String firstName,
        String lastName,
        String email,
        String password
) {

    public static UserDTO from(final UserEntity user) {
        Objects.requireNonNull(user);
        return new UserDTO(user.id(), user.firstName(), user.lastName(), user.email(), user.password());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        UserDTO userDTO = (UserDTO) o;
        return Objects.equals(email, userDTO.email);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(email);
    }
}
