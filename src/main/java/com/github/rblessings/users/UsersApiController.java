package com.github.rblessings.users;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/users")
public class UsersApiController {
    private final UserService userService;

    public UsersApiController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserDTO>> createUser(@Valid @RequestBody UserRegistrationRequest re) {
        UserDTO createdUser = userService.registerUser(re.firstName(), re.lastName(), re.email(), re.password());

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdUser.id())
                .toUri();

        ApiResponse<UserDTO> response = ApiResponse.success(HttpStatus.CREATED.value(), createdUser);
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDTO>> getUserById(@PathVariable String id) {
        return userService.findById(id)
                .map(user -> ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), user)))
                .orElseGet(() -> {
                    var notFoundStatus = HttpStatus.NOT_FOUND;
                    return ResponseEntity.status(notFoundStatus).body(ApiResponse.error(notFoundStatus.value(),
                            "User with ID %s not found".formatted(id)));
                });
    }

    @GetMapping("/principal")
    public ResponseEntity<ApiResponse<Authentication>> getPrincipal(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), authentication));
    }
}
