package com.github.rblessings.users;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/users")
public class UsersApiController {
    private final UserService userService;

    public UsersApiController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<UserDTO>>> createUser(
            @Valid @RequestBody Mono<UserRegistrationRequest> requestMono) {
        return requestMono
                .flatMap(re -> userService.registerUser(re.firstName(), re.lastName(), re.email(), re.password()))
                .map(createdUser -> {
                    URI location = UriComponentsBuilder
                            .fromPath("/api/v1/users/{id}")
                            .buildAndExpand(createdUser.id())
                            .toUri();

                    ApiResponse<UserDTO> response = ApiResponse.success(HttpStatus.CREATED.value(), createdUser);
                    return ResponseEntity.created(location).body(response);
                });
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<UserDTO>>> getUserById(@PathVariable String id) {
        return userService.findById(id)
                .map(user -> ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), user)))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "User with ID %s not found".formatted(id))));
    }

    @GetMapping("/principal")
    public Mono<ResponseEntity<ApiResponse<Authentication>>> getPrincipal(Mono<Authentication> authenticationMono) {
        return authenticationMono
                .map(authentication -> {
                    ApiResponse<Authentication> body = ApiResponse.success(HttpStatus.OK.value(), authentication);
                    return ResponseEntity.ok(body);
                });
    }
}
